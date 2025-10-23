package org.example.ai_api.Strategy.KnowledgeMultipart.Commons;

import org.example.ai_api.Bean.Entity.DepartmentFile;
import org.example.ai_api.Bean.Entity.KnowledgeFileInfo;
import org.example.ai_api.Bean.WebRequest.KnowledgeMultipartCompleteRequest;
import org.example.ai_api.Utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class FileMetaDataBuilder {

    @Value("${minio.bucketName}")
    private String bucketName;
    @Value("${minio.endpoint}")
    private String endpoint;

    /**
     *  基于完成请求与实际对象大小，构建 KnowledgeFileInfo 元数据。
     * @param request 请求
     * @param sizeOnServer  实际对象大小
     * @param isPublic  是否公开
     * @return  KnowledgeFileInfo 元数据
     */
    public KnowledgeFileInfo knowledgeFileInfoBuilder(KnowledgeMultipartCompleteRequest request, long sizeOnServer, boolean isPublic) {
        KnowledgeFileInfo fileInfo = new KnowledgeFileInfo();
        fileInfo.setUploaderId(request.getUserId());
        fileInfo.setOriginalFileName(request.getOriginalFilename());
        String normalizedName = Utils.renameFileToUbuntu(request.getOriginalFilename());
        fileInfo.setFileName(normalizedName);
        fileInfo.setFileType(Utils.getFileExtension(request.getOriginalFilename()));
        fileInfo.setFileTarget(request.getTarget());
        fileInfo.setFolderId(request.getFolderId());
        fileInfo.setCreateTime(LocalDateTime.now());
        fileInfo.setUpdateTime(LocalDateTime.now());
        fileInfo.setHashCode(request.getHash());
        fileInfo.setFileSize(sizeOnServer);
        fileInfo.setPublic(isPublic);
        URI uri = URI.create(String.format("%s/%s/%s", endpoint, bucketName, request.getObjectKey()));
        fileInfo.setStoragePath(Paths.get(uri.getPath()).toString().replace("\\", "/"));
        return fileInfo;
    }

    /**
     * 基于完成请求与实际对象大小，构建 DepartmentFile 元数据。
     */
    public DepartmentFile buildDepartmentFile(KnowledgeMultipartCompleteRequest request, long sizeOnServer) {
        DepartmentFile file = new DepartmentFile();
        file.setUploaderId(request.getUserId());
        file.setOriginalFileName(request.getOriginalFilename());
        String normalizedName = Utils.renameFileToUbuntu(request.getOriginalFilename());
        file.setFileName(normalizedName);
        file.setFileType(Utils.getFileExtension(request.getOriginalFilename()));
        file.setDepartmentId(request.getDepartmentId());
        file.setFolderId(request.getFolderId());
        file.setCreateTime(LocalDateTime.now());
        file.setUpdateTime(LocalDateTime.now());
        file.setHashCode(Optional.ofNullable(request.getHash()).orElse(""));
        file.setFileSize(sizeOnServer);
        URI uri = URI.create(String.format("%s/%s/%s", endpoint, bucketName, request.getObjectKey()));
        file.setStoragePath(Paths.get(uri.getPath()).toString().replace("\\", "/"));
        return file;
    }
}
