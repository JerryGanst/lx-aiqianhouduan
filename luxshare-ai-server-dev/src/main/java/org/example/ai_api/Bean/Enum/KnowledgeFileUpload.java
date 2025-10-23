package org.example.ai_api.Bean.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum KnowledgeFileUpload {

    PrivateType("private", "个人文件"),
    PublicType("public", "公共文件"),
    DepartmentType("department", "部门文件");

    private final String type;
    private final String description;

    KnowledgeFileUpload(String type, String description){
        this.type = type;
        this.description = description;
    }

    @JsonValue
    public String getType() {
        return type;
    }

    @JsonCreator
    public static KnowledgeFileUpload from(String input) {
        if (input == null) return null;
        String s = input.trim();
        for (KnowledgeFileUpload r : values()) {
            if (r.name().equalsIgnoreCase(s) || r.type.equalsIgnoreCase(s)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown KnowledgeFileUpload: " + input);
    }


}
