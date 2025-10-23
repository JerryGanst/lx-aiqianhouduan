package org.example.ai_api.Strategy.KnowledgeBaseUpload;

import org.example.ai_api.Bean.Enum.KnowledgeFileUpload;
import org.example.ai_api.Bean.WebRequest.FileUpload;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 *  知识库上传策略接口
 */
public interface KnowledgeUploadStrategy {
    boolean type(KnowledgeFileUpload type);

    void upload(List<MultipartFile> files, FileUpload fileUpload) throws Exception;
}
