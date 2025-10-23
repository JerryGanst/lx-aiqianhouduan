package org.example.ai_api.Service.Apis.Infrastructure.Factory;

import org.example.ai_api.Bean.Model.AgentConfig;
import org.example.ai_api.Bean.WebRequest.AgentSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *  智能体设置请求构造
 * @author 10353965
 */
@Component
public class AgentSettingRequestFactory {

    private static final Logger logger = LoggerFactory.getLogger(AgentSettingRequestFactory.class);

    public AgentConfig processAgentSetting(AgentSetting agentSetting) {
        logger.info("构造基本的智能体设置请求");
        AgentConfig agentConfig = new AgentConfig();
        agentConfig.setAgentName(agentSetting.getAgentName());
        agentConfig.setAgentSetting(agentSetting.getAgentDescription());
        agentConfig.setAgentDescription(agentSetting.getAgentIntroduction());
        return agentConfig;
    }

}
