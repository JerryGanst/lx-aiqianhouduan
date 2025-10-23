package org.example.ai_api.Persistence.ConditionBuilder;

public class AgentChatConditionBuilder extends BaseConditionBuilder<AgentChatConditionBuilder>{
    public AgentChatConditionBuilder byAgentId(String agentId) {
        return addCondition("agentId", agentId);
    }

    public AgentChatConditionBuilder byUserId(String userId) {
        return addCondition("userId", userId);
    }

    public AgentChatConditionBuilder byId(String id) {
        return addCondition("id", id);
    }

    public AgentChatConditionBuilder byTitleKeyword(String keyword) {
        return addKeywordCondition("title", keyword);
    }
}
