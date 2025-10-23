package org.example.ai_api.Bean.WebRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class UpdateFileTagsRequest {
    @JsonProperty("fileId")
    private String fileId;              // 目标文件ID
    @JsonProperty("isDepartment")
    private boolean isDepartment;       // 是否部门文件
    @JsonProperty("userId")
    private String userId;              // 创建新标签时需要使用
    @JsonProperty("tags")
    private List<TagRef> tags;          // 期望的标签列表（id 或 name）
}
