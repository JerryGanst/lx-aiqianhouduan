package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class GlossaryUpdateItem {
    @JsonProperty("glossary_id")
    private String glossaryId;
    @JsonProperty("en")
    private String en;
    @JsonProperty("zh")
    private String zh;
    @JsonProperty("vi")
    private String vi;
    @JsonProperty("es")
    private String es;
    @JsonProperty("enabled")
    private boolean enabled;
    @JsonProperty("operation_type")
    private String operationType;
}
