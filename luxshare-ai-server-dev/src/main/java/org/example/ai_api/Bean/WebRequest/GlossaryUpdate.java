package org.example.ai_api.Bean.WebRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.example.ai_api.Bean.Model.GlossaryUpdateItem;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class GlossaryUpdate {
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("actor_id")
    private String actorId;
    @JsonProperty("items")
    private List<GlossaryUpdateItem> items;
}
