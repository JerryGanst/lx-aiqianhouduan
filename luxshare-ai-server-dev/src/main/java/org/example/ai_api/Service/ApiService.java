package org.example.ai_api.Service;

import org.example.ai_api.Bean.ApiRequests.*;
import org.example.ai_api.Bean.Entity.Agent;
import org.example.ai_api.Bean.Entity.FileUpload;
import org.example.ai_api.Bean.Entity.KnowledgeFileInfo;
import org.example.ai_api.Bean.Entity.SessionFile;
import org.example.ai_api.Bean.Enum.ChatType;
import org.example.ai_api.Bean.Model.*;
import org.example.ai_api.Bean.WebRequest.*;
import org.example.ai_api.Bean.ApiRepeat.*;
import org.example.ai_api.Config.AIConfig;
import org.example.ai_api.Exception.*;
import org.example.ai_api.Persistence.Dao.KnowledgeFileDao;
import org.example.ai_api.Persistence.Dao.SessionFileDao;
import org.example.ai_api.Strategy.KnowledgeBase.KnowledgeBaseContext;
import org.example.ai_api.Strategy.KnowledgeBase.KnowledgeBaseStrategy;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.example.ai_api.Utils.MinioOperations;
import org.example.ai_api.Bean.ApiRepeat.ExcelChatRepeat;
import org.example.ai_api.Utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.List;

/**
 * AI Api 相关服务.
 * @author 10353965
 */
@Service
public class ApiService {
    private static final Subscription PLACEHOLDER_SUBSCRIPTION = new Subscription() {
        @Override
        public void request(long n) {}
        @Override
        public void cancel() {}
    };
    private static final Logger logger = LoggerFactory.getLogger(ApiService.class.getName());
    private final Map<String, Subscription> subscriptionMap ;
    @Autowired
    private AIConfig aiConfig;
    @Autowired
    private FileUploadInfoService fileUploadInfoService;
    @Autowired
    private KnowledgeBaseContext knowledgeBaseContext;
    @Autowired
    private AgentService agentService;
    @Autowired
    private FileService fileService;
    @Autowired
    @Qualifier("SyncWebClient")
    private WebClient syncWebClient;
    @Autowired
    @Qualifier("StreamWebClient")
    private WebClient streamWebClient;
    @Autowired
    private KnowledgeFileDao knowledgeFileDao;
    @Autowired
    private MinioOperations minioOperations; // 注入 MinioOperations
    @Autowired
    private SessionFileDao sessionFileDao;
    private final ObjectMapper objectMapper = new ObjectMapper(); // 实例化 ObjectMapper
    @Value("${minio.linkExpirySeconds:3600}")
    private int linkExpirySeconds;
    @Value("${minio-proxy}")
    private String minioProxy;
    @Value("${local}")
    private String local;

    private static final String CALLBACK_BUCKET = "ai-artifacts";

    public ApiService() {
        this.subscriptionMap = new ConcurrentHashMap<>();
    }

//    /**
//     * 知识库问答.
//     *
//     * @param requestBody 请求结构体
//     * @param type        提问种类
//     * @param userId      提问的用户id
//     * @return 问答结果(流式)
//     */
//    public Flux<ServerSentEvent<QueryRepeat>> query(QueryRequest requestBody, String type, String userId, int model) {
//        KnowledgeBaseStrategy strategy = knowledgeBaseContext.getStrategy(type);
//        String url = strategy.getUrl(aiConfig);
//        validateModelIndex(model);
//        if ("法务专题".equals(type)) {
//            //法务部分模型临时写死
//            requestBody.setModel("reasoning");
//        } else {
//            requestBody.setModel(aiConfig.getModels().get(model));
//        }
//        logger.info("requestBody:{}", requestBody);
//        logger.info("url:{}", url);
//        return handleStreamRequest(requestBody, url, userId, QueryRepeat.class)
//                .filter(qa -> {
//                    assert qa.data() != null;
//                    return !qa.data().getContent().contains("Result");
//                });
//    }

//    /**
//     * AI总结.
//     *
//     * @param request 请求体
//     * @return 总结结果
//     */
//    public SummaryRepeat summary(SummarizeRequest request) {
//        return handleSyncRequest(request, aiConfig.getCategories().get("summary"), SummaryRepeat.class);
//    }

//    /**
//     * AI翻译.(流式响应)
//     *
//     * @param request 请求体
//     * @return 翻译结果
//     */
//    public Flux<ServerSentEvent<TranslateRepeat>> translateStream(TranslateRequest request) {
//        return handleStreamRequest(request, aiConfig.getCategories().get("translate"), request.getUser_id(),TranslateRepeat.class)
//                .filter(item -> {
//                    assert item.data() != null;
//                    //最初始返回为end = 0 翻译内容为 end = 1 结束为end = 2
//                    return item.data().getEnd() != 0;
//                });
//    }

//    /**
//     * 流式问答.
//     *
//     * @param request 问答请求体
//     * @param userId  用户id
//     * @return 对话结果(流式)
//     */
//    public Flux<ServerSentEvent<ChatStreamRepeat>> chatStream(ChatRequest request, String userId, int model) {
//        validateModelIndex(model);
//        request.setModel(aiConfig.getModels().get(model));
//        logger.info("chatStream");
//        return handleStreamRequest(request, aiConfig.getCategories().get("chat"), userId, ChatStreamRepeat.class);
//    }

//    /**
//     * 智能体对话.
//     * @param request 智能体对话请求体
//     * @return 智能体对话结果(流式)
//     */
//    public Flux<ServerSentEvent<ChatStreamRepeat>> agentChat(AgentChatRequest request) {
//        logger.info("agentChat");
//        return handleStreamRequest(request, aiConfig.getCategories().get("chat"), request.getUserId(), ChatStreamRepeat.class);
//    }

//    /**
//     * 智能体设定生成
//     * @param agentConfig 已有智能体设定
//     * @return 智能体设定
//     */
//    public AgentConfig generateAgentSetting(AgentConfig agentConfig) {
//        logger.info("generateAgentSetting");
//        return handleSyncRequest(agentConfig, aiConfig.getCategories().get("agentSetting"), AgentConfig.class);
//    }

//    /**
//     * 统一的通用对话接口
//     * @param request 对话请求
//     * @return 对话结果(流式)
//     */
//    public Flux<ServerSentEvent<UnifiedChatRepeat>> unifiedChat(UnifiedChatRequest request) {
//        logger.info("unifiedChat {}",request.getUserId());
//        return handleStreamRequest(request, aiConfig.getCategories().get("unifiedChat"), request.getUserId(), UnifiedChatRepeat.class);
//    }

//    /**
//     * 图片对比
//     * @param imageRecognitionRequest 图片对比请求
//     * @return 图片对比结果(流式)
//     */
//    public Flux<ServerSentEvent<ImgRepeat>> imgRecognition(ImageRecognitionRequest imageRecognitionRequest)  {
//        logger.info("imgRecognition {}",imageRecognitionRequest.getUserId());
//        //根据对话记录选择不同的请求url
//        String url;
//        if(imageRecognitionRequest.getMessages().size() == 1){
//            url = aiConfig.getCategories().get("imgRecognitionPlus");
//        }else {
//            url = aiConfig.getCategories().get("imgRecognition");
//        }
//        return handleStreamRequest(imageRecognitionRequest, url, imageRecognitionRequest.getUserId(), ImgRepeat.class);
//    }

//    /**
//     * excel问答
//     * @param excelChatRequest excel问答请求
//     * @return excel问答结果(流式)
//     */
//    public Flux<ServerSentEvent<ExcelChatRepeat>> excelChat(ExcelChatRequest excelChatRequest) {
//        logger.info("excel问答 {}",excelChatRequest.getSessionId());
//        return handleStreamRequest(excelChatRequest, aiConfig.getCategories().get("excelChat"), excelChatRequest.getUserId(), ExcelChatRepeat.class);
//    }

//    /**
//     * 扫描Minio中指定会话ID的文件并生成文件就绪事件。
//     * @param sessionId 会话ID
//     * @param userId 用户ID
//     * @param iteration 轮次
//     * @return 包含文件就绪事件的Flux
//     */
//    public Flux<ServerSentEvent<ExcelChatRepeat>> scanMinioForSessionFiles(String sessionId, String userId,int iteration) {
//        logger.info("开始扫描Minio中指定会话ID {} 的文件", sessionId);
//        return Flux.defer(() -> {
//            try {
//                List<SessionFile> sessionFiles = sessionFileDao.findByUserIdAndSessionIdAndIteration(userId, sessionId,iteration);
//                // 1. 过滤出最新轮次的文件
//                List<SessionFile> userFiles = getLatestIterationFiles(sessionFiles);
//                if (userFiles.isEmpty()) {
//                    logger.info("Minio中没有找到与会话ID {} 和用户ID {} 相关的文件。", sessionId, userId);
//                    return Flux.empty();
//                }
//                // 2. 构建SSE事件
//                return Flux.fromIterable(userFiles)
//                        .flatMap(sessionFile -> buildFileReadyEvent(sessionFile, userId, sessionId));
//            } catch (Exception e) {
//                // 如果整个扫描过程出错，则发出错误
//                logger.error("扫描Minio文件时发生错误: {}", e.getMessage());
//                return Flux.error(e);
//            }
//        });
//    }

//    /**
//     * 停止流式返回.
//     *
//     * @param userId 用户id
//     * @return 是否成功停止
//     */
//    public String stop(String userId) {
//        if (userId == null) {
//            throw new BadRequestException("用户id不可为空");
//        }
//        logger.info("stop {}",subscriptionMap);
//        Subscription subscription = subscriptionMap.remove(userId);
//        if (subscription == null) {
//            // 这里直接返回"已停止"，不抛异常
//            return "请求已停止";
//        } else if (subscription == PLACEHOLDER_SUBSCRIPTION) {
//            return "请求已停止（尚未建立连接）";
//        } else {
//            subscription.cancel();
//            return "请求已停止";
//        }
//    }

//    /**
//     * 从会话文件中过滤出最新轮次的文件。
//     * @param allSessionFiles 所有的会话文件列表
//     * @return 最新轮次的会话文件列表
//     */
//    private List<SessionFile> getLatestIterationFiles(List<SessionFile> allSessionFiles) {
//        Optional<Integer> maxIteration = allSessionFiles.stream()
//                .map(SessionFile::getIteration)
//                .max(Comparator.naturalOrder());
//
//        return maxIteration.map(
//                        maxIter -> allSessionFiles.stream()
//                                .filter(file -> file.getIteration() == maxIter)
//                                .collect(Collectors.toList())
//                )
//                .orElse(Collections.emptyList());
//    }

//    /**
//     * 根据SessionFile构建一个ExcelChatRepeat类型的ServerSentEvent。
//     * @param sessionFile 会话文件实体
//     * @param userId 用户ID
//     * @param sessionId 会话ID
//     * @return 包含文件就绪事件的Mono
//     */
//    private Mono<ServerSentEvent<ExcelChatRepeat>> buildFileReadyEvent(SessionFile sessionFile, String userId, String sessionId) {
//        try {
//            String objectName = sessionFile.getObjectName();
//            String url = minioOperations.getDownloadUrl(CALLBACK_BUCKET, objectName, linkExpirySeconds, Collections.emptyMap());
//            Utils.exchangeFileUrl(url,local,minioProxy);
//            Map<String, Object> fileReadyContentMap = new HashMap<>();
//            fileReadyContentMap.put("userId", userId);
//            fileReadyContentMap.put("objectName", objectName);
//            fileReadyContentMap.put("downloadUrl", url);
//            fileReadyContentMap.put("timestamp", Utils.getNowDate());
//            fileReadyContentMap.put("sessionId", sessionId);
//
//            ExcelChatRepeat fileReadyEvent = new ExcelChatRepeat();
//            fileReadyEvent.setType("fileReady");
//            fileReadyEvent.setContent(objectMapper.writeValueAsString(fileReadyContentMap));
//            fileReadyEvent.setMetaData(null);
//
//            return Mono.just(ServerSentEvent.<ExcelChatRepeat>builder().data(fileReadyEvent).build());
//        } catch (Exception e) {
//            logger.error("处理Session文件 {} 时发生错误: {}", sessionFile.getObjectName(), e.getMessage());
//            return Mono.error(e); // 如果处理单个文件出错，则发出错误
//        }
//    }

//    /**
//     * 对前端的翻译请求进行预处理.
//     *
//     * @param translate 前端翻译请求
//     * @return 预处理后的翻译请求
//     * @throws Exception 处理过程中的异常
//     */
//    public TranslateRequest processTranslate(Translate translate) throws Exception {
//        TranslateRequest translateRequest = new TranslateRequest();
//        translateRequest.setTarget_language(translate.getTarget_language());
//        translateRequest.setUser_id(translate.getUserId());
//        FileId fileId = translate.getFile();
//        translateRequest.setSource_text(getFileContentOrDefault(fileId, translate.getSource_text()));
//        logger.info("翻译文件文本获取完成");
//        return translateRequest;
//    }

//    /**
//     * 对前端的总结请求进行预处理.
//     *
//     * @param summary 前端总结请求
//     * @return 预处理后的总结请求
//     * @throws Exception 处理过程中的异常
//     */
//    public SummarizeRequest processSummary(Summary summary) throws Exception {
//        SummarizeRequest summarizeRequest = new SummarizeRequest();
//        summarizeRequest.setUser_id(summary.getUserId());
//        FileId fileId = summary.getFile();
//        summarizeRequest.setQuestion(getFileContentOrDefault(fileId, summary.getQuestion()));
//        logger.info("总结文件文本获取完成");
//        return summarizeRequest;
//    }

//    /**
//     * 对前端的聊天请求进行预处理.
//     *
//     * @param chatStream 前端聊天请求
//     * @return 预处理后的聊天请求
//     * @throws Exception 处理过程中的异常
//     */
//    public ChatRequest processChat(ChatStream chatStream) throws Exception {
//        ChatRequest chatRequest = createBaseChatRequest(chatStream);
//        processNewFiles(chatStream.getFileIds(), chatRequest::setFile);
//        processHistoricalFiles(chatStream.getMessages());
//        return chatRequest;
//    }

//    /**
//     * 对前端的智能体对话请求进行预处理.
//     *
//     * @param agentChat 前端聊天请求
//     * @return 预处理后的聊天请求
//     * @throws Exception 处理过程中的异常
//     */
//    public AgentChatRequest processAgentChat(AgentChat agentChat) throws Exception {
//        AgentChatRequest agentChatRequest = createBaseAgentChatRequest(agentChat);
//        processNewFiles(agentChat.getFileIds(), agentChatRequest::setFile);
//        processHistoricalFiles(agentChat.getMessages());
//        return agentChatRequest;
//    }

//    /**
//     * 根据前端请求构造基本的统一问答请求对象
//     *
//     * @param unifiedChatStream 前端统一问答请求
//     * @return 基本统一问答请求
//     * @throws Exception 处理过程中的异常
//     */
//    public UnifiedChatRequest processUnifiedChat(UnifiedChatStream unifiedChatStream) throws Exception {
//        //构造基本的统一会话请求
//        UnifiedChatRequest unifiedChatRequest = createBaseUnifiedChatRequest(unifiedChatStream);
//        //处理历史记录中的文件
//        processHistoricalFiles(unifiedChatStream.getMessages());
//        //处理新上传的文件
//        processNewFiles(unifiedChatStream.getFileIds(), unifiedChatRequest::setFile);
//        if (unifiedChatStream.isPersonalKnowledgeBase()) {
//            //若选择启用个人知识库，将个人知识库内容加入请求体
//            addPersonalKnowledgeToRequest(unifiedChatRequest, unifiedChatStream.getUserId(),unifiedChatStream.getChatType(),unifiedChatStream.getFolderId(),unifiedChatStream.getFileId());
//        }
//        //是否有智能体设定
//        if(unifiedChatStream.getAgentId() != null){
//            //生成智能体设定，并将智能体设定加入请求体
//            AgentConfig agentConfig = createAgentConfig(agentService.findAgentById(unifiedChatStream.getAgentId()));
//            unifiedChatRequest.setAgentConfig(agentConfig);
//        }
//        return unifiedChatRequest;
//    }

//    /**
//     * 处理excel对话请求中的excel对话文件
//     * @param excelChat 前端excel对话请求
//     * @return excel对话请求
//     */
//    public ExcelChatRequest processExcelChat(ExcelChat excelChat) {
//        //构造基本的excel对话请求
//        ExcelChatRequest excelChatRequest = createBaseExcelChatRequest(excelChat);
//        //处理对话的excel文件
//        addExcelFilesToRequest(excelChatRequest, excelChat.getFiles());
//        return excelChatRequest;
//    }

//    /**
//     * 根据前端请求构造基本的excel对话请求对象
//     * @param excelChat 前端excel对话请求
//     * @return 基本的excel对话请求
//     */
//    private ExcelChatRequest createBaseExcelChatRequest(ExcelChat excelChat) {
//        ExcelChatRequest excelChatRequest = new ExcelChatRequest();
//        excelChatRequest.setStream(true);
//        excelChatRequest.setUserId(excelChat.getUserId());
//        excelChatRequest.setExcelChatMessages(excelChat.getMessages());
//        excelChatRequest.setSessionId(excelChat.getSessionId());
//        excelChatRequest.setIteration(excelChat.getMessages().size()/2+1);
//        validateModelIndex(excelChat.getModel());
//        excelChatRequest.setModel(aiConfig.getModels().get(excelChat.getModel()));
//        return excelChatRequest;
//    }

//    /**
//     * 将excelId转换为excel文件路径
//     * @param excelChatRequest excel对话请求
//     * @param excelFiles excel文件id
//     */
//    private void addExcelFilesToRequest(ExcelChatRequest excelChatRequest,List<String> excelFiles) {
//        List<String> excelFileNames = new ArrayList<>();
//        for (String excelFile : excelFiles) {
//            excelFileNames.add(getFileObjectNameById(excelFile));
//        }
//        if (excelFileNames.isEmpty()) {
//            excelFileNames = null;
//        }
//        excelChatRequest.setExcelFileList(excelFileNames);
//    }

//    /**
//     * 根据文件id获取文件minio路径
//     * @param fileId 文件id
//     * @return 文件minio路径
//     */
//    private String getFileObjectNameById(String fileId) {
//        try {
//            return fileUploadInfoService.getFileObjectName(fileId);
//        }catch (NotFoundException e){
//            return fileService.getFileObjectName(fileId);
//        }
//    }

//    /**
//     * 根据前端请求构造基本的统一问答请求对象
//     *
//     * @param unifiedChatStream 前端统一问答请求
//     * @return 基本统一问答请求
//     */
//    private UnifiedChatRequest createBaseUnifiedChatRequest(UnifiedChatStream unifiedChatStream) {
//        UnifiedChatRequest unifiedChatRequest = new UnifiedChatRequest();
//        //将历史记录放入提交给AI的请求体
//        unifiedChatRequest.setMessages(unifiedChatStream.getMessages());
//        //设置请求体用户id
//        unifiedChatRequest.setUserId(unifiedChatStream.getUserId());
//        //要求流式返回
//        unifiedChatRequest.setStream(true);
//        //判断并设定模型
//        validateModelIndex(unifiedChatStream.getModel());
//        unifiedChatRequest.setModel(aiConfig.getModels().get(unifiedChatStream.getModel()));
//        return unifiedChatRequest;
//    }

//    /**
//     * 将个人知识库内容加入请求体
//     *
//     * @param unifiedChatRequest 统一问答请求
//     * @param userId 用户id
//     */
//    private void addPersonalKnowledgeToRequest(UnifiedChatRequest unifiedChatRequest, String userId, ChatType chatType,String folderId,String fileId) {
//        unifiedChatRequest.setUsePersonalKnowledge(true);
//        List<String> filePaths;
//        if (chatType == null){
//            throw new BadRequestException("请确定知识库问答类型");
//        }
//        switch (chatType){
//            case ALL_FILES: filePaths = addAllKnowledgeFilesToObjects(userId); break;
//            case PARTIAL_FILES: filePaths = addPartKnowledgeFilesToObjects(folderId); break;
//            case SINGLE_FILE: filePaths = addSingleKnowledgeFilesToObjects(fileId);break;
//            default:throw new BadRequestException("知识库问答请求参数错误");
//        }
//        if (filePaths == null || filePaths.isEmpty()){
//            throw new BadRequestException("未找到有效的知识库文件，请上传或选择知识库文件后再提问");
//        }
//        unifiedChatRequest.setObjects(filePaths);
//    }

//    /**
//     * 将所有知识库文件加入请求体
//     * @param userId 用户id
//     * @return 知识库文件列表
//     */
//    private List<String> addAllKnowledgeFilesToObjects(String userId){
//        return knowledgeFileDao.findPrivateFilesByUploaderId(userId).stream()
//                .map(KnowledgeFileInfo::getConvertPath)
//                .filter(path -> path != null && !path.isEmpty())
//                .filter(path -> !path.endsWith(".xlsx")&&!path.endsWith(".xls"))
//                .collect(Collectors.toList());
//
//    }

//    /**
//     * 将指定文件夹中的知识库文件加入请求体
//     * @param folderId 文件夹id
//     * @return 知识库文件列表
//     */
//    private List<String> addPartKnowledgeFilesToObjects(String folderId){
//        if(folderId == null || folderId.isEmpty()){
//            throw new BadRequestException("请选择有效的文件夹提问");
//        }
//        return knowledgeFileDao.findByFolderId(folderId).stream()
//                .map(KnowledgeFileInfo::getConvertPath)
//                .filter(path -> path != null && !path.isEmpty())
//                .filter(path -> !path.endsWith(".xlsx")&&!path.endsWith(".xls"))
//                .collect(Collectors.toList());
//    }

//    /**
//     * 将指定文件加入请求体
//     * @param fileId 文件id
//     * @return 知识库文件列表
//     */
//    private List<String> addSingleKnowledgeFilesToObjects(String fileId){
//        String filePath = knowledgeFileDao.findByFileId(fileId).getConvertPath();
//        if(filePath != null && !filePath.isEmpty()){
//            List<String> list = new ArrayList<>();
//            list.add(filePath);
//            return list;
//        }else{
//            throw new BadRequestException("请选择确定的知识库文件提问");
//        }
//    }


//    /**
//     * 根据前端请求构造基本的智能体对话请求对象
//     *
//     * @param agentChat 前端智能体聊天请求
//     * @return 基本智能体聊天请求
//     */
//    private AgentChatRequest createBaseAgentChatRequest(AgentChat agentChat) {
//        AgentChatRequest agentChatRequest = new AgentChatRequest();
//        agentChatRequest.setStream(true);
//        agentChatRequest.setUserId(agentChat.getUserId());
//        agentChatRequest.setMessages(agentChat.getMessages());
//        agentChatRequest.setSessionId(agentChat.getSessionId());
//        validateModelIndex(agentChat.getModel());
//        agentChatRequest.setModel(aiConfig.getModels().get(agentChat.getModel()));
//        AgentConfig agentConfig = createAgentConfig(agentService.findAgentById(agentChat.getAgentId()));
//        agentChatRequest.setAgentConfig(agentConfig);
//        return agentChatRequest;
//    }

//    /**
//     * 根据智能体信息创建提供到AI侧的智能体配置
//     *
//     * @param agent 智能体
//     * @return 智能体配置
//     */
//    private AgentConfig createAgentConfig(Agent agent) {
//        AgentConfig agentConfig = new AgentConfig();
//        Persona persona = agent.getPersona();
//        if(persona == null){
//            throw new BadRequestException("智能体配置错误");
//        }
//        agentConfig.setAgentName(persona.getName() != null ? persona.getName() : "");
//        agentConfig.setAgentSetting(persona.getDescription() != null ? persona.getDescription() : "");
//        agentConfig.setAgentDescription(persona.getIntroduction() != null ? persona.getIntroduction() : "");
//        return agentConfig;
//    }

//    /**
//     * 创建基础的聊天请求对象
//     */
//    private ChatRequest createBaseChatRequest(ChatStream chatStream) {
//        ChatRequest chatRequest = new ChatRequest();
//        chatRequest.setStream(true);
//        chatRequest.setUserId(chatStream.getUserId());
//        chatRequest.setMessages(chatStream.getMessages());
//        return chatRequest;
//    }

//    /**
//     * 处理新上传的文件
//     * @param fileIds 文件id列表
//     * @param setFileConsumer 文件内容设置器
//     * @throws Exception 处理过程中的异常
//     */
//    private void processNewFiles(List<FileId> fileIds, Consumer<List<String>> setFileConsumer) throws Exception {
//        if (fileIds == null || fileIds.isEmpty()) {
//            setFileConsumer.accept(null);
//            return;
//        }
//        List<String> fileContents = new ArrayList<>();
//        for (FileId fileId : fileIds) {
//            if (fileId.isLocal()) {
//                fileContents.add(fileUploadInfoService.getContentById(fileId.getFileId()));
//            } else {
//                fileContents.add(fileService.getContentById(fileId.getFileId()));
//            }
//        }
//        setFileConsumer.accept(fileContents);
//    }

//    /**
//     * 处理历史记录中的文件（通用）
//     */
//    private void processHistoricalFiles(List<ChatMessage> messages) throws Exception {
//        for (int index = 0; index < messages.size() - 1; index++) {
//            ChatMessage message = messages.get(index);
//            if (isUserMessageWithFiles(message)) {
//                appendFilesToContent(message);
//            }
//        }
//    }

//    /**
//     * 判断是否为包含文件的用户消息
//     */
//    private boolean isUserMessageWithFiles(ChatMessage message) {
//        return "user".equals(message.getRole()) && message.getUploads() != null;
//    }

//    /**
//     * 将文件内容追加到消息中
//     */
//    private void appendFilesToContent(ChatMessage message) throws Exception {
//        List<FileUpload> files = message.getUploads();
//        if (files == null || files.isEmpty()) {
//            return;
//        }
//        List<String> fileContents = new ArrayList<>();
//        for (FileUpload file : files) {
//            if(file.isLocal()){
//                fileContents.add(fileUploadInfoService.getContentById(file.getFileId()));
//            }else {
//                fileContents.add(fileService.getContentById(file.getFileId()));
//            }
//        }
//        String fileSection = buildFileSection(fileContents);
//        String originalContent = message.getContent() != null ? message.getContent() : "";
//        message.setContent(originalContent + fileSection);
//    }

//    /**
//     * 构建文件内容部分
//     */
//    private String buildFileSection(List<String> files) {
//        StringJoiner fileJoiner = new StringJoiner("\n\n");
//        for (int i = 0; i < files.size(); i++) {
//            fileJoiner.add("文件" + (i + 1) + "：\n" + files.get(i));
//        }
//
//        return "\n\n#####用户提供的文件内容开始#####\n\n"
//                + fileJoiner
//                + "\n\n#####用户提供的文件内容结束#####\n\n";
//    }

//    /**
//     * 获取文件内容，如果fileId无效则返回默认内容
//     */
//    private String getFileContentOrDefault(FileId fileId, String defaultContent) throws Exception {
//        if (fileId == null || fileId.getFileId() == null || fileId.getFileId().isEmpty()) {
//            return defaultContent;
//        } else {
//            return fileId.isLocal() ?
//                fileUploadInfoService.getContentById(fileId.getFileId()) :
//                fileService.getContentById(fileId.getFileId());
//        }
//    }

//    /**
//     * 校验模型下标是否合法
//     * @param model 模型下标
//     */
//    private void validateModelIndex(int model) {
//        if (model > aiConfig.getModels().size()) {
//            throw new NotFoundException("模型不存在");
//        }
//    }

//    /**
//     * 流式返回.
//     * @param requestBody 请求结构体
//     * @param url 请求url
//     * @param userId 用户id
//     * @param responseType 返回类型
//     * @return 流式返回结果
//     * @param <T> 返回类型
//     */
//    private <T> Flux<ServerSentEvent<T>> handleStreamRequest(
//            Object requestBody,
//            String url,
//            String userId,
//            Class<T> responseType
//    ) {
//        return streamWebClient
//                .post()
//                .uri(url)
//                .accept(MediaType.TEXT_EVENT_STREAM)
//                .body(BodyInserters.fromValue(requestBody))
//                .exchangeToFlux(response -> {
//                    if (response.statusCode().isError()) {
//                        return response.bodyToMono(String.class)
//                                .flatMapMany(errorBody ->
//                                        // 抛出异常，触发全局错误处理
//                                        Flux.error(new StreamApiException(
//                                                response.rawStatusCode(),
//                                                errorBody
//                                        ))
//                                );
//                    }
//                    return response.bodyToFlux(responseType)
//                            .map(item -> ServerSentEvent.builder(item).build())
//                            //.delayElements(Duration.ofMillis(50))
//                            .doOnSubscribe(subscription -> {
//                                subscriptionMap.put(userId, subscription);
//                                logger.info("Subscribe{}",userId);
//                            })
//                            .doOnTerminate(() -> {
//                                subscriptionMap.remove(userId);
//                                logger.info("Terminate{}",userId);
//                            })
//                            .doOnCancel(() -> {
//                                subscriptionMap.remove(userId);
//                                logger.info("Cancel{}",userId);
//                            })
//                            .doOnError(e -> {
//                                subscriptionMap.remove(userId);
//                                logger.info("Error{}",userId);
//                            });
//                });
//    }

//    /**
//     * 同步请求.
//     * @param request 请求结构体
//     * @param url 请求url
//     * @param responseType 返回类型
//     * @return 同步返回结果
//     * @param <T> 返回类型
//     */
//    private <T> T handleSyncRequest(
//            Object request,
//            String url,
//            Class<T> responseType
//    ) {
//        return syncWebClient
//                .post()
//                .uri(url)
//                .body(BodyInserters.fromValue(request))
//                .retrieve()
//                .onStatus(HttpStatus::isError, response ->
//                        response.bodyToMono(String.class)
//                                .flatMap(error -> {
//                                    int statusCode = response.rawStatusCode();
//                                    logger.warn("请求失败，错误码：{}, 错误信息：{}", statusCode, error);
//                                    // 针对5xx错误创建可重试异常
//                                    if (statusCode >= 500 && statusCode < 600) {
//                                        return Mono.error(new RetryableApiException(statusCode, error));
//                                    } else {
//                                        return Mono.error(new SyncApiException(statusCode, error));
//                                    }
//                                })
//                )
//                .bodyToMono(responseType)
//                // 添加重试机制 (只重试5xx错误和网络异常)
//                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
//                        .filter(throwable ->
//                                throwable instanceof RetryableApiException || throwable instanceof WebClientRequestException
//                        )
//                        .doAfterRetry(retrySignal ->
//                                logger.debug("重试次数: {}", retrySignal.totalRetries())
//                        )
//                )
//                .blockOptional()
//                .orElseThrow(() -> new ThirdPartyDataException("返回体为空"));
//    }

//    public void putSubscriptionPlaceholder(String userId) {
//        // 只在没有的情况下放入，避免覆盖已存在的 Subscription
//        subscriptionMap.putIfAbsent(userId, PLACEHOLDER_SUBSCRIPTION);
//        logger.info("put{}",subscriptionMap);
//    }
}
