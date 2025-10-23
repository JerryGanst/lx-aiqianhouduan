package org.example.ai_api.Bean.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum MessageRole {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool");

    private final String value;

    MessageRole(String value) { this.value = value; }

    // 序列化为小写
    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static MessageRole from(String input) {
        if (input == null) return null;
        String s = input.trim();
        // 既支持传“USER/ASSISTANT/...”，也支持传“user/assistant/...”
        for (MessageRole r : values()) {
            if (r.name().equalsIgnoreCase(s) || r.value.equalsIgnoreCase(s)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown MessageRole: " + input);
    }
}
