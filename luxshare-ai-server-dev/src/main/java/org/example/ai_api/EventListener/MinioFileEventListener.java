package org.example.ai_api.EventListener;

import org.apache.commons.codec.digest.DigestUtils;
import org.example.ai_api.Bean.Entity.FileIdData;
import org.example.ai_api.Bean.Entity.KnowledgeFileInfo;
import org.example.ai_api.Bean.Entity.PublicFileResult;
import org.example.ai_api.Bean.Events.MinioFileDeleteEvent;
import org.example.ai_api.Bean.Events.MinioFileUploadedEvent;
import org.example.ai_api.Persistence.Repository.FileIdDataRepository;
import org.example.ai_api.Persistence.Repository.KnowledgeFileRepository;
import org.example.ai_api.Persistence.Repository.PublicFileResultRepository;
import org.example.ai_api.Service.AIPlatformSyncService;
import org.example.ai_api.Service.FileService;
import org.example.ai_api.Service.PublicFileService;
import org.example.ai_api.Utils.MinioOperations;
import org.example.ai_api.Utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MinioFileEventListener {
    private static final Logger logger = LoggerFactory.getLogger(MinioFileEventListener.class);
    @Autowired
    private KnowledgeFileRepository knowledgeFileRepository;
    @Autowired
    private FileIdDataRepository fileIdDataRepository;
    @Autowired
    private FileService fileService;
    @Autowired
    private AIPlatformSyncService aiPlatformSyncService;
    @Autowired
    private MinioOperations minioOperations;
    @Value("${minio.bucketName}")
    private String bucketName;
    @Value("${minio.endpoint}")
    private String endpoint;

    @Async
    @EventListener
    public void handleFileUploadedEvent(MinioFileUploadedEvent event) throws Exception {
        logger.info("同步文件到minio，{}", event);
        List<Path> result = new ArrayList<>();
        List<String> fileHash = new ArrayList<>();
        List<byte[]> fileBytes = event.getFiles();
        String userId = event.getUserId();
        String target = event.getTarget();
        List<String> fileNames = event.getOriginalNames()
                .stream()
                .map(Utils::renameFileToUbuntu)
                .collect(Collectors.toList());
        List<String> idsInSystem = event.getIdsInSystem();
        List<String> contentTypes = event.getContentTypes();
        for (int i = 0; i < fileBytes.size(); i++) {
            fileHash.add(DigestUtils.md5Hex(fileBytes.get(i)));
            InputStream stream = new ByteArrayInputStream(fileBytes.get(i));
            String objectName = minioOperations.createKnowledgeFileName(fileNames.get(i), target, userId, "",event.isPublic());
            minioOperations.uploadFile(objectName, stream, fileBytes.get(i).length, contentTypes.get(i));
            URI uri = URI.create(String.format("%s/%s/%s", endpoint, bucketName, objectName));
            result.add(Paths.get(uri.getPath()));
        }
        //构建文件信息
        List<KnowledgeFileInfo> fileInfos = buildUploadFileInfo(event.getOriginalNames(), fileNames, fileHash, userId, result, target, event.isPublic());
        //保存文件信息
        List<KnowledgeFileInfo> knowledgeFileInfos = fileService.saveAll(fileInfos);
        //构建文件转换任务
        //fileService.buildFileConversionTask(knowledgeFileInfos);
        //构建文件id数据并保存
        buildFileIdData(idsInSystem, knowledgeFileInfos);
        // 调用第三方转换服务并保存结果
//        List<PublicFileResult> convertedResults = fileService.convertFiles(knowledgeFileInfos);
        // 将转换结果保存到 PublicFileResult 集合，其中会统一初始化字段
//        publicFileService.savePublicFileResults(convertedResults,target);
        //第三方转换服务异步调用
        fileService.covertPublicKnowledgeFilesAsync(knowledgeFileInfos);
    }

    private void buildFileIdData(List<String> idsInSystem, List<KnowledgeFileInfo> knowledgeFileInfos) {
        List<FileIdData> result = new ArrayList<>();
        for (int i = 0; i < idsInSystem.size(); i++) {
            FileIdData fileIdData = new FileIdData();
            fileIdData.setFileIdInSystem(idsInSystem.get(i));
            fileIdData.setFileId(knowledgeFileInfos.get(i).getId());
            result.add(fileIdData);
        }
        fileIdDataRepository.saveAll(result);
    }

    private List<KnowledgeFileInfo> buildUploadFileInfo(List<String> originalNames, List<String> fileNames, List<String> hashCode, String userId, List<Path> filePath, String target, boolean isPublic) {
        List<KnowledgeFileInfo> result = new ArrayList<>();
        for (int i = 0; i < originalNames.size(); i++) {
            KnowledgeFileInfo fileInfo = new KnowledgeFileInfo();
            fileInfo.setUploaderId(userId);
            fileInfo.setOriginalFileName(originalNames.get(i));
            fileInfo.setFileName(fileNames.get(i));
            fileInfo.setStoragePath(filePath.get(i).toString().replace("\\", "/"));
            fileInfo.setCreateTime(LocalDateTime.now());
            fileInfo.setUpdateTime(LocalDateTime.now());
            fileInfo.setFileTarget(isPublic ? target : "");
            fileInfo.setPublic(isPublic);
            fileInfo.setFileType(Utils.getFileExtension(originalNames.get(i)));
            fileInfo.setHashCode(hashCode.get(i));
            result.add(fileInfo);
        }
        return result;
    }

    @Async
    @EventListener
    public void handleFileDeletedEvent(MinioFileDeleteEvent event) throws Exception {
        logger.info(event.toString());
        logger.info("开始删除minio文件");
        FileIdData fileIdData = fileIdDataRepository.findByFileIdInSystem(event.getFileId());
        if (fileIdData == null) {
            return;
        }
        String fileId = fileIdData.getFileId();//获得minio文件id
        KnowledgeFileInfo fileInfo = knowledgeFileRepository.findById(fileId).orElse(null);
        if (fileInfo == null) {
            return;
        }
        minioOperations.deleteFile(fileInfo.getStoragePath().replace("/"+bucketName, ""));
        minioOperations.deleteFile(fileInfo.getConvertPath());
        fileIdDataRepository.delete(fileIdData);
        knowledgeFileRepository.delete(fileInfo);
        // 删除所有相关的 PublicFileResult 记录
        //List<PublicFileResult> relatedConvertedFiles = publicFileResultRepository.findByFileId(fileId);
        //publicFileResultRepository.deleteAll(relatedConvertedFiles);
        // 调用通知 AI 平台文件删除事件
        //publicFileService.notifyAiPlatformFileDeleted(fileId);
        // 保持对其他可能依赖的转换文件的删除 (如果 fileService.convertFilesDelete 包含了 Minio 文件删除逻辑)
        fileService.convertFilesDelete(fileId);
        logger.info("同步信息到AI平台");
        aiPlatformSyncService.deleteFileFromAIPlatform(fileInfo.getAiFileId())
                .doOnSuccess(v -> logger.info("公共知识库文件删除同步AI平台成功"))
                .doOnError(e -> logger.error("公共知识库文件删除同步AI平台失败", e))
                .subscribe();
    }
}
