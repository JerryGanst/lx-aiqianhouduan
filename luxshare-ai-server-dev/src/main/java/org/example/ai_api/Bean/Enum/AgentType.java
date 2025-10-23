package org.example.ai_api.Bean.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum AgentType {

    COMPARE_AGENT_TYPE("compare"),
    RESUME_AGENT_TYPE("resume"),
    TABLE_AGENT_TYPE ("table"),
    DEFAULT_AGENT_TYPE("default");

    private final String agentType;

    AgentType(String agentType) {
        this.agentType = agentType;
    }

    // 序列化为小写
    @JsonValue
    public String getAgentType() {
        return agentType;
    }

    @JsonCreator
    public static AgentType from(String input) {
        if (input == null) return null;
        String s = input.trim();
        for (AgentType r : values()) {
            if (r.name().equalsIgnoreCase(s) || r.agentType.equalsIgnoreCase(s)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown AgentType: " + input);
    }
}
