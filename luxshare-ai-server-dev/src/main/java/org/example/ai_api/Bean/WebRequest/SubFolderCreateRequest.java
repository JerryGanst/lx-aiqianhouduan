package org.example.ai_api.Bean.WebRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class SubFolderCreateRequest {
    @JsonProperty("folderId")
    private String folderId;     // 父级文件夹ID
    @JsonProperty("userId")
    private String userId;       // 操作用户ID
    @JsonProperty("targetName")
    private String targetName;   // 二级文件夹名称
}

