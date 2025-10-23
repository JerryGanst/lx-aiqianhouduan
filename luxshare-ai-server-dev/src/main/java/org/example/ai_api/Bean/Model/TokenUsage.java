package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class TokenUsage {
    @JsonProperty("input_tokens")
    private int inputTokens;
    @JsonProperty("output_tokens")
    private int outputTokens;
    @JsonProperty("total_tokens")
    private int totalTokens;
}
