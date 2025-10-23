package org.example.ai_api.Service.Apis.Applications;

import org.example.ai_api.Bean.Model.AgentConfig;
import org.example.ai_api.Bean.WebRequest.AgentSetting;
import org.example.ai_api.Config.AIConfig;
import org.example.ai_api.Service.Apis.Infrastructure.AIClient;
import org.example.ai_api.Service.Apis.Infrastructure.Factory.AgentSettingRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 10353965
 */
@Component
public class AgentSettingCase {

    private static final Logger logger = LoggerFactory.getLogger(AgentSettingCase.class);

    @Autowired
    private AgentSettingRequestFactory agentSettingRequestFactory;
    @Autowired
    private AIConfig aiConfig;
    @Autowired
    private AIClient aiClient;


    /**
     * 智能体设定生成
     * @param agentSetting  智能体设定请求
     * @return 智能体设定
     */
    public AgentSetting generateAgentSetting(AgentSetting agentSetting) {
        logger.info("generateAgentSetting");
        AgentConfig agentConfig = agentSettingRequestFactory.processAgentSetting(agentSetting);
        AgentConfig aiAgentConfig = aiClient.handleSyncRequest(agentConfig, aiConfig.getCategories().get("agentSetting"), AgentConfig.class);
        return new AgentSetting(aiAgentConfig);
    }


}
