package org.example.ai_api.Bean.ApiRequests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class ExcelTranslateRequest {
    @JsonProperty("sessionId")
    private String sessionId;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("file_url")
    private String fileUrl;
    @JsonProperty("file_name")
    private String fileName;
    @JsonProperty("target_language")
    private String targetLanguage;
    @JsonProperty("upload_api_url")
    private String uploadApiUrl;
}
