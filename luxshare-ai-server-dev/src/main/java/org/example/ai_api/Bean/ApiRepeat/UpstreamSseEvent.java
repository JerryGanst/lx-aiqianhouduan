package org.example.ai_api.Bean.ApiRepeat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpstreamSseEvent {

    @JsonProperty("type")
    private String type;

    @JsonProperty("ts")
    private Long ts;

    @JsonProperty("content")
    private String content; // may be null when using delta

    @JsonProperty("subgraph")
    private String subgraph; // optional subgraph name

    @JsonProperty("component")
    private String component; // e.g. planner/agent/summarizer/final

    @JsonProperty("lifecycle")
    private String lifecycle; // e.g. start|delta|end

    @JsonProperty("channel")
    private String channel; // valid only for llm: text|reasoning

    @JsonProperty("attrs")
    private Map<String, Object> attrs; // structured extra info

    // Optional fields that may appear
    @JsonProperty("delta")
    private String delta; // streaming piece

    @JsonProperty("timestamp")
    private Long timestamp; // fallback if ts missing
}

