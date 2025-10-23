package org.example.ai_api.Bean.ApiRequests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 将文件同步到ai平台落库
 */
@Data
public class FileSynRequest {
    @JsonProperty("file_path")
    private String filePath;
    @JsonProperty("title")
    private String title;
    @JsonProperty("tenant_id")
    private String tenantId = "luxshare-tech";
    @JsonProperty("kb_type")
    private String knowledgeBaseType;
    @JsonProperty("doc_type")
    private String domain;
    @JsonProperty("owner_user_id")
    private String ownerId;
    @JsonProperty("folder_id")
    private String folderId;
    @JsonProperty("categories")
    private List<String> categories;
}
