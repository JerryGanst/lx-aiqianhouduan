package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;
import org.example.ai_api.Bean.Entity.FileUpload;
import org.example.ai_api.Bean.Model.ContentItem.BaseContentItem;

import org.example.ai_api.Bean.Model.Deserializer.AIChatMessageContentDeserializer;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class AIChatMessage {
    @JsonProperty("role")
    private String role;
    @JsonProperty("content")
    @JsonDeserialize(using = AIChatMessageContentDeserializer.class)
    private List<BaseContentItem> content;

    //业务模型字段
    @JsonProperty("personalKnowledge")
    private Boolean personalKnowledge = false;
    @JsonProperty("files")
    private List<FileUpload> uploads;

    //AI模型业务字段
    @JsonProperty("tool_calls")
    private List<ToolCall>  toolCalls;
    @JsonProperty("tool_call_id")
    private String toolCallId;
    @JsonProperty("name")
    private String name;
    @JsonProperty("metadata")
    private MessageMetaData metadata;
}
