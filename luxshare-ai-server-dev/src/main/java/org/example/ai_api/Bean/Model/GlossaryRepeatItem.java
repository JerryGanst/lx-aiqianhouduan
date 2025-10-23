package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class GlossaryRepeatItem {
    @JsonProperty("glossary_id")
    private String glossaryId;
    @JsonProperty("user_id")
    private String userId;
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
    @JsonProperty("update_by")
    private String updateBy;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("updated_at")
    private String updatedAt;
}
