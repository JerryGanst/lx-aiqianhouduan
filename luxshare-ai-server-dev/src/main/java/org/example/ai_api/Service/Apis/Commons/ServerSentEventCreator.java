package org.example.ai_api.Service.Apis.Commons;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ai_api.Bean.ApiRepeat.UpstreamSseEvent;
import org.example.ai_api.Bean.Entity.SessionFile;
import org.example.ai_api.Persistence.Dao.SessionFileDao;
import org.example.ai_api.Utils.MinioOperations;
import org.example.ai_api.Utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SSE 事件构造器（Excel 场景）：
 * - 扫描 MinIO 回调桶中指定会话的生成物
 * - 产生文件就绪事件并拼接到 SSE 流
 * @author 10353965
 */
@Component
public class ServerSentEventCreator {

    private static final Logger logger = LoggerFactory.getLogger(ServerSentEventCreator.class);

    @Autowired
    private SessionFileDao sessionFileDao;
    @Autowired
    private MinioOperations minioOperations;
    @Value("${minio-proxy}")
    private String minioProxy;
    @Value("${local}")
    private String local;
    @Value("${minio.linkExpirySeconds:3600}")
    private int linkExpirySeconds;

    private static final String CALLBACK_BUCKET = "ai-artifacts";
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 扫描Minio中指定会话ID的文件并生成文件就绪事件。
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param iteration 轮次
     * @return 包含文件就绪事件的Flux
     */
    public Flux<ServerSentEvent<UpstreamSseEvent>> scanMinioForSessionFiles(String sessionId, String userId, int iteration) {
        logger.info("开始扫描Minio中指定会话ID {} 的文件", sessionId);
        return Flux.defer(() -> {
            try {
                List<SessionFile> sessionFiles = sessionFileDao.findByUserIdAndSessionIdAndIteration(userId, sessionId,iteration);
                // 1. 过滤出最新轮次的文件
                List<SessionFile> userFiles = getLatestIterationFiles(sessionFiles);
                if (userFiles.isEmpty()) {
                    logger.info("Minio中没有找到与会话ID {} 和用户ID {} 相关的文件。", sessionId, userId);
                    return Flux.empty();
                }
                // 2. 构建SSE事件
                return Flux.fromIterable(userFiles)
                        .flatMap(sessionFile -> buildFileReadyEvent(sessionFile, userId, sessionId));
            } catch (Exception e) {
                // 如果整个扫描过程出错，则发出错误
                logger.error("扫描Minio文件时发生错误: {}", e.getMessage());
                return Flux.error(e);
            }
        });
    }

    /**
     * 从会话文件中过滤出最新轮次的文件。
     * @param allSessionFiles 所有的会话文件列表
     * @return 最新轮次的会话文件列表
     */
    private List<SessionFile> getLatestIterationFiles(List<SessionFile> allSessionFiles) {
        Optional<Integer> maxIteration = allSessionFiles.stream()
                .map(SessionFile::getIteration)
                .max(Comparator.naturalOrder());

        return maxIteration.map(
                        maxIter -> allSessionFiles.stream()
                                .filter(file -> file.getIteration() == maxIter)
                                .collect(Collectors.toList())
                )
                .orElse(Collections.emptyList());
    }

    /**
     * 根据SessionFile构建一个上游事件的ServerSentEvent。
     * @param sessionFile 会话文件实体
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return 包含文件就绪事件的Mono
     */
    private Mono<ServerSentEvent<UpstreamSseEvent>> buildFileReadyEvent(SessionFile sessionFile, String userId, String sessionId) {
        try {
            String objectName = sessionFile.getObjectName();
            String url = minioOperations.getDownloadUrl(CALLBACK_BUCKET, objectName, linkExpirySeconds, Collections.emptyMap());
            Utils.exchangeFileUrl(url,local,minioProxy);
            Map<String, Object> fileReadyContentMap = new HashMap<>();
            fileReadyContentMap.put("userId", userId);
            fileReadyContentMap.put("objectName", objectName);
            fileReadyContentMap.put("downloadUrl", url);
            fileReadyContentMap.put("timestamp", Utils.getNowDate());
            fileReadyContentMap.put("sessionId", sessionId);

            UpstreamSseEvent fileReadyEvent = UpstreamSseEvent.builder()
                    .type("notice")
                    .content(objectMapper.writeValueAsString(fileReadyContentMap))
                    .component("excel")
                    .timestamp(System.currentTimeMillis())
                    .build();

            return Mono.just(ServerSentEvent.<UpstreamSseEvent>builder().data(fileReadyEvent).build());
        } catch (Exception e) {
            logger.error("处理Session文件 {} 时发生错误: {}", sessionFile.getObjectName(), e.getMessage());
            return Mono.error(e); // 如果处理单个文件出错，则发出错误
        }
    }

}
