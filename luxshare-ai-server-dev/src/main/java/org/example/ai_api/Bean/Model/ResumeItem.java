package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class ResumeItem {
    @JsonProperty("id")
    private String id;
    @JsonProperty("text")
    private String text;
}
