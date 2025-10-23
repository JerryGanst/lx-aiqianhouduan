package org.example.ai_api.Bean.ApiRepeat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.example.ai_api.Bean.Model.Source;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class UnifiedChatRepeat {
    @JsonProperty("type")
    private String type;
    @JsonProperty("content")
    private String content;
    @JsonProperty("sources")
    private List<Source> sources;
    @JsonProperty("lifecycle")
    private String lifecycle;
    @JsonProperty("channel")
    private String channel;
}
