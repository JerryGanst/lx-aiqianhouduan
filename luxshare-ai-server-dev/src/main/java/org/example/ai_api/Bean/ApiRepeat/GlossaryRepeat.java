package org.example.ai_api.Bean.ApiRepeat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.example.ai_api.Bean.Model.GlossaryRepeatItem;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class GlossaryRepeat {
    @JsonProperty("items")
    private List<GlossaryRepeatItem> items;
    @JsonProperty("count")
    private int count;
}
