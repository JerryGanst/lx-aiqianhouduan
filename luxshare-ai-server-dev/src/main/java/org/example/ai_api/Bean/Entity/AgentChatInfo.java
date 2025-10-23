package org.example.ai_api.Bean.Entity;

import lombok.*;
import org.example.ai_api.Bean.Model.ChatMessage;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "AgentChat")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
@CompoundIndexes({
    // 主要查询索引：agentId + title + updateTime
    @CompoundIndex(name = "agentId_title_updateTime_idx", def = "{'agentId': 1, 'title': 1, 'updateTime': -1}"),
    // 用户查询索引：userId + updateTime
    @CompoundIndex(name = "userId_updateTime_idx", def = "{'userId': 1, 'updateTime': -1}"),
    // 时间排序索引：updateTime
    @CompoundIndex(name = "updateTime_idx", def = "{'updateTime': -1}")
})
public class AgentChatInfo<T> {
    /**
     * 对话记录id
     */
    @Id
    private String id;
    /**
     * 智能体id
     */
    private String agentId;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 对话标题
     */
    private String title;
    /**
     * 创建时间
     */
    private String createTime;
    /**
     * 更新时间
     */
    private String updateTime;
    /**
     * 操作时间
     */
    private String lastOperationTime;
    /**
     * 对话内容
     */
    private List<ChatMessage> messages;
    /**
     * 用户反馈
     */
    private T feedback;
}
