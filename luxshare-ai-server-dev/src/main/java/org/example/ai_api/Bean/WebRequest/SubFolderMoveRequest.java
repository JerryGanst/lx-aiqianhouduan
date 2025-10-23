package org.example.ai_api.Bean.WebRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class SubFolderMoveRequest {
    @JsonProperty("userId")
    private String userId;
    @JsonProperty("fileId")
    private String fileId;
    @JsonProperty("isDepartment")
    private boolean isDepartment; // true: 部门文件；false: 个人文件
    @JsonProperty("toSubFolderId")
    private String toSubFolderId; // 目标二级目录ID
    @JsonProperty("fromSubFolderId")
    private String fromSubFolderId; // 可选：来源二级目录ID，用于校验
}

