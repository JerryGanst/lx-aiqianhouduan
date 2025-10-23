package org.example.ai_api.Persistence.ConditionBuilder;

public class AgentConditionBuilder extends BaseConditionBuilder<AgentConditionBuilder>{
    public AgentConditionBuilder byUserId(String userId) {
        return addCondition("userId", userId);
    }

    public AgentConditionBuilder byPersonaNameKeyword(String keyword) {
        return addKeywordCondition("persona.name", keyword);
    }

    public AgentConditionBuilder byId(String id) {
        return addCondition("_id", id);
    }

}
