package org.example.ai_api.Bean.WebRequest;

import lombok.Getter;
import lombok.Setter;
import org.example.ai_api.Bean.Enum.KnowledgeFileUpload;

import java.util.List;

/**
 * 知识库分片上传-完成阶段请求
 */
@Setter
@Getter
public class KnowledgeMultipartCompleteRequest {
    // 完成合并所需
    private String uploadId; // 可选：如果前端希望后端代为 Complete
    private String objectKey; // 必填：最终对象键
    private List<MultipartCompletedPart> parts; // 可选：若带上则后端代为 Complete

    // 元数据
    private String originalFilename;
    private String contentType;
    private Long size;

    // 业务上下文
    private KnowledgeFileUpload uploadType; // private/public/department
    private String userId;
    private String target;       // 私有/公共
    private String folderId;     // 私有/部门
    private String departmentId; // 部门
    private String targetFolderId; //二级文件夹id
    private String hash;         // 可选

}

