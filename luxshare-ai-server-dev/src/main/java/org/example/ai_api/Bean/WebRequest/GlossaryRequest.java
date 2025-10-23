package org.example.ai_api.Bean.WebRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class GlossaryRequest {
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("scope")
    private String scope;
    @JsonProperty("query")
    private String query;
    @JsonProperty("threshold")
    private Integer threshold;
    @JsonProperty("offset")
    private Integer offset;
    @JsonProperty("limit")
    private Integer limit;
}
