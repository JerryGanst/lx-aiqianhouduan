package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class MessageMetaData {
    @JsonProperty("model_name")
    private String modelName;
    @JsonProperty("finish_reason")
    private String finishReason;
    @JsonProperty("token_usage")
    private TokenUsage tokenUsage;
}
