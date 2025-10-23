package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class Function {
    @JsonProperty("name")
    private String name;
    @JsonProperty("arguments")
    private String arguments;
}
