package org.example.ai_api.Bean.ApiRepeat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class GlossaryUpdateRepeat {
    @JsonProperty("added")
    private int added;
    @JsonProperty("edited")
    private int edited;
    @JsonProperty("deleted")
    private int deleted;
    @JsonProperty("skipped")
    private int skipped;
}
