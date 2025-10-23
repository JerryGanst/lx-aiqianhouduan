package org.example.ai_api.Bean.Entity;

import lombok.*;
import org.example.ai_api.Bean.Model.Persona;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "Agent")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class Agent {
    /**
     * 智能体数据库id
     */
    @Id
    private String id;
    /**
     * 创建者id
     */
    private String userId;
    /**
     * 智能体头像
     */
    private String agentPic;
    /**
     * 智能体头像预览链接
     */
    private String agentPicUrl;
    /**
     * 智能体名称
     */
    private String agentName;
    /**
     * 智能体具体内容
     */
    private Persona persona;
    /**
     * 智能体创建时间
     */
    private String createTime;
    /**
     * 智能体更新时间
     */
    private String updateTime;
    /**
     * 最近一次的操作时间
     */
    private String lastOperationTime;
    /**
     * 智能体是否是系统内置
     */
    private boolean isSystem = false;
}
