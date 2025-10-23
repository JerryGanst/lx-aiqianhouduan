package org.example.ai_api.Bean.WebRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class SubFolderUpdateRequest {
    @JsonProperty("id")
    private String id;           // 二级文件夹ID（Tag ID）
    @JsonProperty("userId")
    private String userId;       // 操作用户ID
    @JsonProperty("targetName")
    private String targetName;   // 新名称
}

