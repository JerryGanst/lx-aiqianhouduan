package org.example.ai_api.Service;

import org.example.ai_api.Bean.Entity.KnowledgeFileInfo;
import org.example.ai_api.Bean.Entity.PublicFileResult;
import org.example.ai_api.Bean.Model.AiPlatformFileNotification; // 新增导入
import org.example.ai_api.Bean.Model.AiPlatformFileNotification.NotificationType; // 新增导入
import org.example.ai_api.Config.AIPlatformConfig;
import org.example.ai_api.Persistence.Repository.PublicFileResultRepository;
import org.example.ai_api.Utils.ContentTypeDetector;
import org.example.ai_api.Utils.MinioOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 10353965
 */
@Service
public class PublicFileService {

    private static final Logger logger = LoggerFactory.getLogger(PublicFileService.class);

    @Autowired
    private PublicFileResultRepository publicFileResultRepository;
    @Autowired
    private MinioOperations minioOperations;
    @Autowired
    private AIPlatformConfig aiPlatformConfig;
    @Autowired
    private FileService fileService;
    @Autowired
    @Qualifier("SyncWebClient")
    private WebClient webClient;
    @Value("${minio.bucketName}")
    private String bucketName;

    /**
     * 保存 PublicFileResult 列表，并统一初始化或补充字段
     */
    public void savePublicFileResults(List<PublicFileResult> results, String target) {
        results.forEach(result -> {
            if (result.getId() == null) {
                result.setConversionTime(LocalDateTime.now());
                result.setWrittenByAiPlatform(false);
                result.setFileTarget(target);
                if (result.getContentType() == null || result.getContentType().isEmpty()) {
                    String detectedContentType = ContentTypeDetector.getContentType(result.getFilePath());
                    result.setContentType(detectedContentType);
                    logger.debug("文件 {} (ID: {}) contentType 被推断为 {}", result.getFilePath(), result.getFileId(), result.getContentType());
                }
            }
        });
        publicFileResultRepository.saveAll(results);
    }

    /**
     * 异步：将文件列表提交给第三方转换服务并保存结果。
     */
    @Async
    public void asyncConvertAndSave(List<KnowledgeFileInfo> files, String target) {
        try {
            List<PublicFileResult> converted = fileService.convertFiles(files);
            savePublicFileResults(converted, target);
        } catch (Exception e) {
            logger.error("异步转换并保存公共文件结果失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 根据 ID 获取 PublicFileResult
     */
    public Optional<PublicFileResult> getPublicFileResultById(String id) {
        return publicFileResultRepository.findById(id);
    }

    /**
     * 根据源文件 ID 获取 PublicFileResult 列表
     */
    public List<PublicFileResult> getPublicFileResultsByFileId(String fileId) {
        return publicFileResultRepository.findByFileId(fileId);
    }

    /**
     * 获取所有未被 AI 平台处理的转换文件结果
     */
    public List<PublicFileResult> getUnprocessedPublicFileResults() {
        return publicFileResultRepository.findByIsWrittenByAiPlatformFalse();
    }

    /**
     * 批量更新 PublicFileResult 的 isWrittenByAiPlatform 状态
     */
    public List<PublicFileResult> updateAiPlatformWrittenStatus(Collection<String> fileIds, boolean status) {
        List<PublicFileResult> results = publicFileResultRepository.findByFileIdIn(fileIds);
        results.forEach(result -> {
            result.setWrittenByAiPlatform(status);
            if (status) {
                result.setProcessedTime(LocalDateTime.now());
            } else {
                result.setProcessedTime(null);
            }
        });
        return publicFileResultRepository.saveAll(results);
    }

    /**
     * 通知第三方 AI 平台文件删除事件。
     */
    public void notifyAiPlatformFileDeleted(String originalFileId) {
        logger.warn("文件 {} (原始ID) 已被删除，准备通知第三方 AI 平台。", originalFileId);

        List<PublicFileResult> deletedFilesInfo = publicFileResultRepository.findByFileId(originalFileId);
        if (deletedFilesInfo.isEmpty()) {
            logger.warn("未找到原始文件ID {} 的任何转换文件信息，无法发送删除通知。", originalFileId);
            return;
        }
        processAndSendAiPlatformNotification(deletedFilesInfo,NotificationType.DELETE,true);
    }

    /**
     * 定时任务：轮询未处理的文件，并推送给 AI 平台
     */
    //@Scheduled(fixedRateString = "${ai-platform-push-interval-ms}")
    public void pushUnprocessedDataToAiPlatform() {
        logger.info("定时任务开始：检查并推送未处理的训练数据到 AI 平台。");

        List<PublicFileResult> unprocessedFiles = getUnprocessedPublicFileResults();

        if (unprocessedFiles.isEmpty()) {
            logger.info("目前没有未处理的训练数据，跳过推送。");
            return;
        }

        processAndSendAiPlatformNotification(unprocessedFiles,NotificationType.ADD,false);
    }

    /**
     * 构建文件信息列表，forDelete为true时downloadUrl为固定字符串，否则生成真实下载链接
     */
    private List<Map<String, String>> buildFileInfoList(List<PublicFileResult> fileResults, boolean forDelete) {
        return fileResults.stream().map(fileResult -> {
            Map<String, String> data = new HashMap<>();
            data.put("original_file_id", fileResult.getId());
            data.put("converted_file_id", fileResult.getFileId());
            data.put("content_type", fileResult.getContentType());
            if (forDelete) {
                data.put("download_url", "N/A - File Deleted");
            } else {
                try {
                    String downloadUrl = minioOperations.getDownloadUrl(bucketName, fileResult.getFilePath(), 3600, null);
                    data.put("download_url", downloadUrl);
                } catch (Exception e) {
                    logger.error("为文件 {} 生成下载链接失败，无法推送：{}", fileResult.getFileId(), e.getMessage());
                    return null;
                }
            }
            return data;
        }).filter(java.util.Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 统一发送通知到 AI 平台的私有方法。
     * @param notification 包含操作类型和文件信息的通知对象。
     */
    private void sendNotificationToAiPlatform(AiPlatformFileNotification notification,String aiPlatformPushUrl) {
        try {
            logger.info("准备向 AI 平台推送 {} 操作的 {} 个文件信息到 URL: {}", notification.getType(), notification.getFiles().size(), aiPlatformPushUrl);
            String responseBody = webClient.post()
                    .uri(aiPlatformPushUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(notification)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            logger.info("AI 平台接收接口响应：{}", responseBody);
            logger.info("{} 文件信息已推送至 AI 平台，等待 AI 平台调用确认接口进行状态更新。", notification.getType());

        } catch (Exception e) {
            logger.error("推送 {} 数据到 AI 平台失败：{}", notification.getType(), e.getMessage(), e);
        }
    }

    /**
     * 发送文件信息通知到 AI 平台
     * @param files 需要处理的文件列表
     * @param notificationType 通知类型 (ADD, DELETE)
     * @param isDelete 操作是否是删除
     */
    private void processAndSendAiPlatformNotification(List<PublicFileResult> files, NotificationType notificationType, boolean isDelete) {
        Map<String,List<PublicFileResult>> listMap = new HashMap<>();
        files.forEach(file ->
                listMap.computeIfAbsent(file.getFileTarget(), k -> new ArrayList<>()).add(file)
        );

        listMap.forEach((target, classifiedFiles) -> {
            List<Map<String, String>> dataToPush = buildFileInfoList(classifiedFiles, isDelete);

            if (dataToPush.isEmpty()) {
                logger.warn("所有未处理文件在生成下载链接时失败，没有数据推送给 AI 平台。分类: {}", target);
                return;
            }

            AiPlatformFileNotification notification = new AiPlatformFileNotification(notificationType, dataToPush);
            String url = aiPlatformConfig.getPushUrls().get(target);
            sendNotificationToAiPlatform(notification, url);
        });
    }
}
