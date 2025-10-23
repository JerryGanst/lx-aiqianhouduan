package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class Progress {
    @JsonProperty("total")
    private int total;
    @JsonProperty("done")
    private int done;
    @JsonProperty("failed")
    private int failed;
}
