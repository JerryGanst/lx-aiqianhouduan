package org.example.ai_api.Bean.ApiRepeat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnifiedStreamEvent {

    @JsonProperty("type")
    private String type; // fixed: "stream"

    @JsonProperty("stage")
    private String stage; // thinking | answering | error

    @JsonProperty("content")
    private String content;

    @JsonProperty("component")
    private String component;

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("done")
    private Boolean done;
}

