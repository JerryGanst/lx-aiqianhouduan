package org.example.ai_api.Bean.WebRequest;

import lombok.*;
import org.example.ai_api.Bean.Model.AgentConfig;

//智能体设定生成
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class AgentSetting {
    /**
     * 智能体名称
     */
    private String agentName;
    /**
     * 智能体设定
     */
    private String agentDescription;
    /**
     * 智能体简介
     */
    private String agentIntroduction;

    public AgentSetting(AgentConfig agentConfig) {
        this.agentName = agentConfig.getAgentName();
        this.agentDescription = agentConfig.getAgentSetting();
        this.agentIntroduction = agentConfig.getAgentDescription();
    }
}
