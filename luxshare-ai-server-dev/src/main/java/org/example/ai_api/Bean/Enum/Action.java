package org.example.ai_api.Bean.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum Action {
    AUTO("auto"),
    EXCEL_AGENT("excel_agent"),
    GENERAL_CHAT("general_chat"),
    IMAGE_COMPARE("image_compare"),
    TRANSLATE_WORKFLOW("translate_workflow"),
    RagItAgent("rag_it_agent"),
    RagHrAgent("rag_hr_agent"),
    RagPersonalAgent("rag_personal_agent"),
    RagDepartmentAgent("rag_department_agent");


    private final String actionType;

    Action(String string) {
        this.actionType = string;
    }

    // 序列化为小写
    @JsonValue
    public String getActionType() {
        return actionType;
    }

    @JsonCreator
    public static Action from(String input) {
        if (input == null) return null;
        String s = input.trim();
        for (Action r : values()) {
            if (r.name().equalsIgnoreCase(s) || r.actionType.equalsIgnoreCase(s)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown Action: " + input);
    }
}
