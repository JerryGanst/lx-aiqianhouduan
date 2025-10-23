package org.example.ai_api.Bean.WebRequest;

import lombok.Getter;
import lombok.Setter;
import org.example.ai_api.Bean.Enum.KnowledgeFileUpload;

/**
 * 知识库分片上传-准备阶段请求
 */
@Setter
@Getter
public class KnowledgeMultipartPrepareRequest {
    // 通用上传信息
    private String filename;
    private String contentType;
    private Long size;
    private Long partSize; // 可选
    private Integer expireSeconds; // 可选

    // 业务上下文
    private KnowledgeFileUpload uploadType; // private/public/department
    private String userId;
    private String target;
    private String folderId;     // 私有/部门
    private String departmentId; // 部门
    private String hash;         // 可选，前端计算

}

