package org.example.ai_api.Bean.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum KnowledgeBaseType {
    Enterprise("enterprise"),
    Personal("personal"),
    Department("department");
    private final String type;

    KnowledgeBaseType(String type) {
        this.type = type;
    }

    // 序列化为小写
    @JsonValue
    public String getType() {
        return type;
    }

    @JsonCreator
    public static KnowledgeBaseType from(String input) {
        if (input == null) return null;
        String s = input.trim();
        for (KnowledgeBaseType r : values()) {
            if (r.name().equalsIgnoreCase(s) || r.type.equalsIgnoreCase(s)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown KnowledgeBaseType: " + input);
    }
}
