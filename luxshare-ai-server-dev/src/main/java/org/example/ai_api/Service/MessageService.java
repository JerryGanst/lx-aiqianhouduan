package org.example.ai_api.Service;

import org.example.ai_api.Bean.ApiRepeat.QueryRepeat;
import org.example.ai_api.Bean.Entity.Message;
import org.example.ai_api.Bean.ApiRepeat.SimilarityRepeat;
import org.example.ai_api.Bean.ApiRequests.SimilarityRequest;
import org.example.ai_api.Bean.Model.Source;
import org.example.ai_api.Config.AIConfig;
import org.example.ai_api.Exception.NotFoundException;
import org.example.ai_api.Exception.ThirdPartyDataException;
import org.example.ai_api.Persistence.Dao.HistoryDao;
import org.example.ai_api.Persistence.Repository.HistoryRepository;
import org.example.ai_api.Utils.MinioOperations;
import org.example.ai_api.Utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.Document;

import java.util.LinkedHashSet;
import org.example.ai_api.Bean.Model.FileInfoFormSystem;
import java.util.Collections;
import java.util.HashMap;

/**
 * 聊天历史记录相关服务.
 * @author 10353965
 */
@Service
public class MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class.getName());
    private static final Map<String, String> MESSAGE_TYPE_TARGET_MAP;
    static {
        Map<String, String> aMap = new HashMap<>();
        aMap.put("人资行政专题", "HR");
        aMap.put("IT专题", "IT");
        aMap.put("法务专题", "Law");
        MESSAGE_TYPE_TARGET_MAP = Collections.unmodifiableMap(aMap);
    }
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private HistoryRepository historyRepository;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private AIConfig aiConfig;
    @Autowired
    private HistoryDao historyDao;
    @Autowired
    private MinioOperations minioOperations;
    @Autowired
    private FileService fileService;
    @Value("${targetScore}")
    private double targetScore;
    @Value("${categoriesKey}")
    private String categoriesKey;
    @Value("${rank_key}")
    private String rankKey;
    @Value("${local}")
    private String local;
    @Value("${minio-proxy}")
    private String minioProxy;
    @Value("${systemFiles-view}")
    private String fileLink;

    /**
     * 根据用户id返回历史记录.
     *
     * @param userId 用户id
     * @return 历史记录链表
     */
    public List<Map> findByUserid(String userId,String type,String keyword) {
        logger.info("根据用户id获得历史记录: {}", userId);
        List<Map> result = historyDao.findMessagesByUserIdWithFields(userId, type, keyword);
        Utils.convertMongoIdToStringId(result, "id", true);
        return result;
    }

    /**
     * 保存历史记录.
     *
     * @param message 需要保存的记录
     * @return 保存后的信息结构
     */
    public Message save(Message message) {
        logger.info("保存历史记录,用户id: {}", message.getUserid());
        return historyDao.save(message);
    }

    /**
     * 根据id获得记录.
     *
     * @param id 历史记录id
     * @return 查询的对应历史记录
     */
    public Message findById(String id) {
        logger.info("根据记录id获得历史记录: {}", id);
        return historyRepository.findById(id).orElseThrow(() -> new NotFoundException("未找到历史记录"));
    }

    /**
     * 根据id删除历史记录.
     *
     * @param id 历史记录id
     */
    public void deleteById(String id) {
        logger.info("根据记录id删除历史记录: {}", id);
        historyDao.deleteById(id);
    }

    public List<Message> findByUserIdAndDateBetween(String userid, String startDate, String endDate) {
        logger.info("根据用户id和时间段获得历史记录: {} {} {}", userid, startDate, endDate);
        return historyDao.findHistoryByUserIdAndDateBetween(userid, startDate, endDate);
    }

    public List<Message> findByIdAndDateBefore(String id, String startDate) {
        logger.info("根据记录id和时间获得时间点前的历史记录: {} {}", id, startDate);
        return historyDao.findHistoryByUserIdAndDateBefore(id, startDate);
    }

    public List<Message> findByIdAndDateAfter(String id, String endDate) {
        logger.info("根据记录id和时间获得时间点后的历史记录: {} {}", id, endDate);
        return historyDao.findHistoryByUserIdAndDateAfter(id, endDate);
    }

    /**
     * 将问题添加到redis缓存.
     *
     * @param title 问题内容
     * @param type  问题所属分类
     */
    @Async("taskExecutor")
    public void addTitleToRedis(String title, String type) {
        Set<String> set = stringRedisTemplate.opsForZSet().range(rankKey + type, 0, -1);
        //若set为空，表示当前分类不存在，直接保存即可
        if (set == null || set.isEmpty()) {
            stringRedisTemplate.opsForSet().add(categoriesKey, type);
            stringRedisTemplate.opsForZSet().incrementScore(rankKey + type, title, 1);
            return;
        }
        boolean flag = true;
        double nowScore = 0.0;
        String nowTitle = "";
        WebClient webClient = WebClient.builder()
                .defaultHeader("Content-type", MediaType.APPLICATION_JSON_VALUE)
                .baseUrl(aiConfig.getCategories().get("similarity"))
                .build();
        //set不为空，计算后根据阈值判断是否添加
        for (String value : set) {
            SimilarityRequest similarityRequest = new SimilarityRequest(value, title);
            double score = webClient.post()
                    .body(BodyInserters.fromValue(similarityRequest))
                    .retrieve()
                    .bodyToMono(SimilarityRepeat.class)
                    .blockOptional()
                    .orElseThrow(() -> new ThirdPartyDataException("返回体为空"))
                    .getScore();
            logger.info("similarityRequest:{}", similarityRequest);
            logger.info("score: {}", score);
            if (score > targetScore) {
                flag = false;
                if (score > nowScore) {
                    nowScore = score;
                    nowTitle = value;
                }
            }
        }
        if (flag) {
            stringRedisTemplate.opsForSet().add(categoriesKey, type);
            stringRedisTemplate.opsForZSet().incrementScore(rankKey + type, title, 1);
        } else {
            stringRedisTemplate.opsForZSet().incrementScore(rankKey + type, nowTitle, 1);
        }
    }

    public List<Message> MessageSortByDate(List<Message> list) {
        logger.info("对历史记录进行排序");
        list.sort((o1, o2) -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime date1 = LocalDateTime.parse(o1.getDate(), formatter);
            LocalDateTime date2 = LocalDateTime.parse(o2.getDate(), formatter);
            return date2.compareTo(date1);
        });
        return list;
    }

    /**
     * 根据文件名获取文件预览链接,添加到流式问答结果中
     * @param data 流式问答结果
     */
    public void addFileUrlToSource(QueryRepeat data, String type){
        if ("final_answer".equals(data.getType()) && data.getSources() != null) {
            for (Source source : data.getSources()) {
                try {
                    String documentTitle = source.getDocument_title();
                    String target = MESSAGE_TYPE_TARGET_MAP.get(type);
                    List<FileInfoFormSystem> fileInfoFormSystems = fileService.getFileByTarget(target);
                    for (FileInfoFormSystem fileInfoFormSystem : fileInfoFormSystems) {
                        String name = Utils.removeFileExtension(fileInfoFormSystem.getCategory());
                        if (documentTitle.equals(name)) {
                            source.setFileUrl(fileLink + fileInfoFormSystem.getFileKey());
                        }
                    }
                } catch (Exception e) {
                    logger.error("为QueryRepeat生成fileUrl失败，document_title: {}", source.getDocument_title(), e);
                    source.setFileUrl(null);
                }
            }
        }
    }

    /**
     * 当 message.type 为 "通用模式" 时，基于每个 data[i].sources[j].document_title 生成并更新对应的 fileUrl。
     * - 只接受一个已查询出的 Message 对象作为参数，不在内部再次查询；
     * - 在内存对象上直接更新（保证返回给前端的数据是最新）；
     * - 如果 message.id 存在，会用 arrayFilters 将变更精准持久化到 MongoDB；
     * <p>
     * 结构说明（结合你的 JSON）：
     * - message：Message<T, K>，其中 T 实际为 List（对话消息数组 data）。
     * - message.data：List<?>，每个元素为一条消息项（可能是 org.bson.Document 或 Map<String,Object>）。
     * - data[i].sources：List<?>，每个元素为来源对象（可能是 Document 或 Map<String,Object>）。
     * - sources[j].document_title：String，用于生成/匹配 fileUrl。
     * - sources[j].fileUrl：String，待更新的新 URL。
     *
     * @param message 外部已按 id 查询出的消息对象
     * @return 已在内存更新后的消息对象
     */
    public Message updateSourcesFileUrl(Message message, boolean isGeneral) {
        if (isGeneral && !isGeneralMode(message)) {
            return message;
        }

        Object dataObj = message.getData();
        Set<String> touchedTitles = new LinkedHashSet<>();
        processDataForFileUrlUpdate(dataObj, touchedTitles, message.getType(), isGeneral);
        return message;
    }

    /** 判断是否需要按规则更新（仅在 type="通用模式" 时更新）。 */
    private boolean isGeneralMode(Message message) {
        if (message == null) {
            return false;
        }
        String type = message.getType();
        return "通用模式".equals(type);
    }

    /**
     * 通用辅助方法：根据 document_title 生成并设置 fileUrl。
     * 支持 Document 和 Map 类型的来源元素。
     */
    @SuppressWarnings("unchecked")
    private void setFileUrlForSource(Object src, Set<String> touchedTitles, String messageType, boolean isGeneral) {
        String title = null;
        if (src instanceof Document) {
            title = ((Document) src).getString("document_title");
        } else if (src instanceof Map<?, ?>) {
            Object titleObj = ((Map<String, Object>) src).get("document_title");
            if (titleObj != null) {
                title = String.valueOf(titleObj);
            }
        }

        if (title == null || title.isEmpty()) {
            return;
        }

        String newUrl = isGeneral ? buildFileUrlFromTitle(title) : buildFileUrlForNonGeneralType(title, messageType);
        if (newUrl == null || newUrl.isEmpty()) {
            return;
        }

        if (src instanceof Document) {
            ((Document) src).put("fileUrl", newUrl);
        } else {
            ((Map<String, Object>) src).put("fileUrl", newUrl);
        }
        touchedTitles.add(title);
    }

    /**
     * 处理 message.data 字段，可以是 List<?> 或单个 Document/Map<?>。
     * 递归查找并处理其中的 sources 列表。
     */
    private void processDataForFileUrlUpdate(Object dataObj, Set<String> touchedTitles, String messageType, boolean isGeneral) {
        if (dataObj instanceof List<?>) {
            List<?> items = (List<?>) dataObj;
            for (Object item : items) {
                processMessageItemForSourcesUpdate(item, touchedTitles, messageType, isGeneral);
            }
        } else if (dataObj instanceof Map<?, ?>) {
            processMessageItemForSourcesUpdate(dataObj, touchedTitles, messageType, isGeneral);
        }
    }

    /**
     * 处理单个消息项（Document 或 Map 形态），查找并更新其中的 sources 列表。
     * sources 可能是直接子字段，也可能是嵌套在 'answer' 字段下。
     */
    @SuppressWarnings("unchecked")
    private void processMessageItemForSourcesUpdate(Object item, Set<String> touchedTitles, String messageType, boolean isGeneral) {
        List<?> sources;
        if (item instanceof Document) {
            Document itemDoc = (Document) item;
            // Try to get sources directly from the item
            sources = itemDoc.getList("sources", Document.class);
            if (sources == null && itemDoc.containsKey("answer")) {
                Object answerObj = itemDoc.get("answer");
                if (answerObj instanceof Document) {
                    sources = ((Document) answerObj).getList("sources", Document.class);
                }
            }
            if (sources != null) {
                for (Object src : sources) {
                    if (src instanceof Document) {
                        setFileUrlForSource(src, touchedTitles, messageType, isGeneral);
                    } else if (src instanceof Map<?, ?>) {
                        setFileUrlForSource(src, touchedTitles, messageType, isGeneral);
                    }
                }
            }
        } else if (item instanceof Map<?, ?>) {
            Map<String, Object> itemMap = (Map<String, Object>) item;
            // Try to get sources directly from the item
            Object srcListObj = itemMap.get("sources");
            if (srcListObj == null && itemMap.containsKey("answer")) {
                Object answerObj = itemMap.get("answer");
                if (answerObj instanceof Map<?, ?>) {
                    srcListObj = ((Map<String, Object>) answerObj).get("sources");
                }
            }
            if (srcListObj instanceof List<?>) {
                sources = (List<?>) srcListObj;
                for (Object src : sources) {
                    if (src instanceof Document) {
                        setFileUrlForSource(src, touchedTitles, messageType, isGeneral);
                    } else if (src instanceof Map<?, ?>) {
                        setFileUrlForSource(src, touchedTitles, messageType, isGeneral);
                    }
                }
            }
        }
    }

    /**
     * 示例 URL 生成器：根据 document_title 生成/刷新 fileUrl。
     * 请用你的真实业务规则替换此实现（例如签名直链、CDN、MinIO 临时链接等）。
     */
    private String buildFileUrlFromTitle(String documentTitle) {
        try {
            String url = minioOperations.getDownloadUrl(documentTitle.substring(1), 24*60*60, null);
            return Utils.exchangeFileUrl(url, local, minioProxy);
        } catch (Exception e) {
            logger.error("生成 fileUrl 失败", e);
            return null;
        }
    }

    private String buildFileUrlForNonGeneralType(String documentTitle, String messageType) {
        try {
            String target = MESSAGE_TYPE_TARGET_MAP.getOrDefault(messageType, messageType);
            List<FileInfoFormSystem> fileInfoFormSystems = fileService.getFileByTarget(target);
            for (FileInfoFormSystem fileInfoFormSystem : fileInfoFormSystems) {
                String name = Utils.removeFileExtension(fileInfoFormSystem.getCategory());
                if (documentTitle.equals(name)) {
                    return fileLink + fileInfoFormSystem.getFileKey();
                }
            }
        } catch (Exception e) {
            logger.error("生成非通用模式 fileUrl 失败", e);
        }
        return null;
    }
}
