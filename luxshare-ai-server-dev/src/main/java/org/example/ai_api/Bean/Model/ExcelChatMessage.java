package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.example.ai_api.Bean.Entity.FileUpload;
import org.example.ai_api.Bean.Enum.MessageRole;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class ExcelChatMessage {
    @JsonProperty("role")
    private MessageRole role;
    @JsonProperty("content")
    private String content;
    @JsonProperty("files")
    private List<FileUpload> files;
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
    @JsonProperty("objectName")
    private String objectName;
    @JsonProperty("downloadUrl")
    private String downloadUrl;
    @JsonProperty("name")
    private String name;
    @JsonProperty("tool_calls")
    private List<ToolCall> toolCalls;
    @JsonProperty("tool_call_id")
    private String toolCallId;
    @JsonProperty("metadata")
    private MessageMetaData messageMetaData;
}
