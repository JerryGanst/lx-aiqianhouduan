package org.example.ai_api.Bean.ApiRepeat;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class ChatStreamRepeat {
    @JsonProperty("type")
    private String type;

    @JsonProperty("content")
    private String content;

    @JsonProperty("metadata")
    private ChatStreamMetadata metadata;

    private String role = "assistant";

    @JsonIgnore
    public boolean isFinalAnswer() {
        return "final_answer".equalsIgnoreCase(type);
    }

    @JsonIgnore
    public boolean isCompleteContext() {
        return "complete_context".equalsIgnoreCase(type);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Setter
    @Getter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChatStreamMetadata {
        @JsonProperty("ts")
        private Long timestamp;

        @JsonProperty("subgraph")
        private String subgraph;

        @JsonProperty("sanitized")
        private Boolean sanitized;

        @JsonProperty("stage")
        private String stage;

        @JsonProperty("route")
        private String route;

        @JsonProperty("reason")
        private String reason;

        @JsonProperty("source")
        private String source;

        @JsonProperty("candidates")
        private List<List<String>> candidates;

        @JsonProperty("evaluation")
        private Evaluation evaluation;

        @JsonProperty("tool_exploration")
        private ToolExploration toolExploration;

        @JsonProperty("name")
        private String name;

        @JsonProperty("args_preview")
        private String argsPreview;

        @JsonProperty("result_preview")
        private String resultPreview;

        private Map<String, Object> extras = new HashMap<>();

        @JsonAnySetter
        public void addExtra(String key, Object value) {
            if (extras == null) {
                extras = new HashMap<>();
            }
            extras.put(key, value);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Setter
    @Getter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Evaluation {
        @JsonProperty("done")
        private Boolean done;

        @JsonProperty("reason")
        private String reason;

        @JsonProperty("revised_instruction")
        private String revisedInstruction;

        private Map<String, Object> extras = new HashMap<>();

        @JsonAnySetter
        public void addExtra(String key, Object value) {
            if (extras == null) {
                extras = new HashMap<>();
            }
            extras.put(key, value);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Setter
    @Getter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ToolExploration {
        @JsonProperty("tagsets")
        private List<List<String>> tagsets;

        @JsonProperty("found")
        private Integer found;

        private Map<String, Object> extras = new HashMap<>();

        @JsonAnySetter
        public void addExtra(String key, Object value) {
            if (extras == null) {
                extras = new HashMap<>();
            }
            extras.put(key, value);
        }
    }
}
