package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class AssistantMessage extends BaseMessage {
    @JsonIgnore
    private String role = "assistant";

    @JsonProperty("content")
    private List<TextContent> content;
    
    @JsonProperty("before")
    private String before;
    
    @JsonProperty("after")
    private String after;
    
    @JsonProperty("hasSplit")
    private Boolean hasSplit;
    
    @JsonProperty("isNewData")
    private Boolean isNewData;

    @JsonProperty("thinking")
    private String thinking;
} 