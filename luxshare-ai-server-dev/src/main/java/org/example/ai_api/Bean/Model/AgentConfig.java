package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.example.ai_api.Bean.WebRequest.AgentSetting;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class AgentConfig {
//    /**
//     * 生成的针对当前请求的唯一id
//     */
//    @JsonProperty("uid")
//    private String uid;
    /**
     * agent_name: 智能体名称
     */
    @JsonProperty("name")
    private String agentName;
    /**
     * agent_setting: 智能体设定
     */
    @JsonProperty("setting")
    private String agentSetting;
    /**
     * agent_description: 智能体描述
     */
    @JsonProperty("descriptions")
    private String agentDescription;

    public AgentConfig(AgentSetting agentSetting) {
        this.agentName = agentSetting.getAgentName();
        this.agentSetting = agentSetting.getAgentDescription();
        this.agentDescription = agentSetting.getAgentIntroduction();
    }
}
