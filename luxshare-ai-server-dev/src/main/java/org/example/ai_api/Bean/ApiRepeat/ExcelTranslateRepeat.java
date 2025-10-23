package org.example.ai_api.Bean.ApiRepeat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class ExcelTranslateRepeat {
    @JsonProperty("session_id")
    private String sessionId;
    @JsonProperty("object_name")
    private String objectName;
    @JsonProperty("file_name")
    private String originalFileName;
}
