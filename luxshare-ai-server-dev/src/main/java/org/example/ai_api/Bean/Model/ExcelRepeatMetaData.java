package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class ExcelRepeatMetaData {
    @JsonProperty("tool_names")
    private List<String> toolNames;
    @JsonProperty("tool_count")
    private int toolCount;
    @JsonProperty("message_count")
    private int messageCount;
    @JsonProperty("total_iterations")
    private int totalIterations;
    @JsonProperty("error_type")
    private String errorType;
    @JsonProperty("source")
    private String source;
}
