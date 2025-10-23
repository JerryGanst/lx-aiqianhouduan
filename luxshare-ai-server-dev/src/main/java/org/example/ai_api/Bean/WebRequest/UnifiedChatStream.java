package org.example.ai_api.Bean.WebRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.example.ai_api.Bean.Model.AIChatMessage;
import org.example.ai_api.Bean.Model.ChatMessage;
import org.example.ai_api.Bean.Model.FileId;
import org.example.ai_api.Bean.Enum.ChatType;

import javax.validation.constraints.NotBlank;
import java.util.List;

//统一问答前端请求体
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class UnifiedChatStream {
    /**
     * 单次对话唯一标识
     */
    @JsonProperty("sessionId")
    private String sessionId;
    /**
     * 对话内容
     */
    @JsonProperty("messages")
    private List<AIChatMessage> messages;
    /**
     * 对话用户id
     */
    @JsonProperty("userId")
    @NotBlank(message = "userId不能为空")
    private String userId;
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
     * 智能体id
     */
    @JsonProperty("agentId")
    private String agentId;
    /**
     * 对话是否开启个人知识库
     */
    @JsonProperty("personalKnowledgeBase")
    private boolean personalKnowledgeBase;
    /**
     *  对话是否开启部门知识库
     */
    @JsonProperty("departmentKnowledgeBase")
    private boolean departmentKnowledgeBase;
    /**
     * 对话知识库问答类型
     * 当personalKnowledgeBase为真时有效
     */
    @JsonProperty("chatType")
    private ChatType chatType;
    /**
     * 知识库提问的文件夹id
     * 当personalKnowledgeBase为真且chatType为partial有效
     */
    @JsonProperty("folderId")
    private String folderId;
    /**
     * 文件id
     * 当personalKnowledgeBase为真且chatType为single有效
     */
    @JsonProperty("fileId")
    private String fileId;
    /**
     *  知识库提问的二级文件夹id
     *  当开启知识库问答且chatType为subfolder_files有效
     */
    @JsonProperty("tagList")
    private  List<String> tagList;
}
