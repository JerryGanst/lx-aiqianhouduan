package org.example.ai_api.Bean.WebRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.example.ai_api.Bean.Model.ChatMessage;
import org.example.ai_api.Bean.Model.FileId;

import javax.validation.constraints.NotBlank;
import java.util.List;

//智能体对话请求
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class AgentChat {
    /**
     * 对话所选智能体id
     */
    @JsonProperty("agentId")
    @NotBlank(message = "agentId不能为空")
    private String agentId;
    /**
     * 对话用户id
     */
    @JsonProperty("userId")
    @NotBlank(message = "userId不能为空")
    private String userId;
    /**
     * 对话内容
     */
    @JsonProperty("messages")
    private List<ChatMessage> messages;
    /**
     * 对话所选模型
     */
    @JsonProperty("model")
    private int model;
    /**
     * 对话附带文件信息
     */
    @JsonProperty("files")
    private List<FileId> fileIds;
    /**
     * sessionId
     */
    @JsonProperty("sessionId")
    @NotBlank(message = "sessionId不能为空")
    private String sessionId;
}
