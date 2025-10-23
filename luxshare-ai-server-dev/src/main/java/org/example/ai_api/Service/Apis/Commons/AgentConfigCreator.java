package org.example.ai_api.Service.Apis.Commons;

import org.example.ai_api.Bean.Entity.Agent;
import org.example.ai_api.Bean.Model.AgentConfig;
import org.example.ai_api.Bean.Model.Persona;
import org.example.ai_api.Exception.BadRequestException;
import org.example.ai_api.Service.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 智能体配置构造器：
 * - 从业务侧 Agent 信息生成 AI 侧可用的 AgentConfig
 * - 负责 Persona 字段到提示词与元信息的映射
 * @author 10353965
 */
@Component
public class AgentConfigCreator {

    @Autowired
    private AgentService agentService;

    /**
     * 根据智能体信息创建提供到AI侧的智能体配置
     *
     * @param agentId 智能体id
     * @return 智能体配置
     */
    public AgentConfig createAgentConfig(String agentId) {
        Agent agent = agentService.findAgentById(agentId);
        AgentConfig agentConfig = new AgentConfig();
        Persona persona = agent.getPersona();
        if(persona == null){
            throw new BadRequestException("智能体配置错误");
        }
        agentConfig.setAgentName(persona.getName() != null ? persona.getName() : "");
        agentConfig.setAgentSetting(persona.getDescription() != null ? persona.getDescription() : "");
        agentConfig.setAgentDescription(persona.getIntroduction() != null ? persona.getIntroduction() : "");
        return agentConfig;
    }
}
