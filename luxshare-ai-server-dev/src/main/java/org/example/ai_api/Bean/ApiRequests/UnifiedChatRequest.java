package org.example.ai_api.Bean.ApiRequests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.example.ai_api.Bean.Model.AgentConfig;
import org.example.ai_api.Bean.Model.ChatMessage;

import java.util.List;

//统一对话请求体
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class UnifiedChatRequest {
    /**
     * 对话历史记录
     */
    @JsonProperty("messages")
    private List<ChatMessage> messages;
    /**
     * 用户id
     */
    @JsonProperty("user_id")
    private String userId;
    /**
     * 模型名称
     */
    @JsonProperty("model")
    private String model;
    /**
     * 用户最后一次提问部分附带的文件
     */
    @JsonProperty("file")
    private List<String> file;
    /**
     * 此次回答是否使用个人知识库
     */
    @JsonProperty("use_personal_knowledge")
    private boolean usePersonalKnowledge = false;
    /**
     * 是否开启流式输出
     */
    @JsonProperty("stream")
    private boolean stream = false;
    /**
     * 个人知识库文件的minio对象名称(usePersonalKnowledge为true时有效)
     */
    @JsonProperty("objects")
    private List<String> objects;
    /**
     * 智能体配置
     */
    @JsonProperty("agent_config")
    private AgentConfig agentConfig;
}
