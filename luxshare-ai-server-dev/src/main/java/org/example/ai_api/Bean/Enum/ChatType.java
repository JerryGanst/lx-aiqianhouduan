package org.example.ai_api.Bean.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ChatType {
    // 全量文件问答
    ALL_FILES("all"),
    // 部分文件问答
    PARTIAL_FILES("partial"),
    // 单个文件问答
    SINGLE_FILE("single"),
    // 部门文件夹问答
    Department_Partial("department_partial"),
    // 部门单个文件问答
    Department_Single("department_single"),
    // 第二级文件夹问答
    Tag_Files("tag_files");

    private final String knowledgeType;

    ChatType(String knowledgeType) {
        this.knowledgeType = knowledgeType;
    }

    // 序列化为小写
    @JsonValue
    public String getKnowledgeType() {
        return knowledgeType;
    }

    @JsonCreator
    public static ChatType from(String input) {
        if (input == null) return null;
        String s = input.trim();
        for (ChatType r : values()) {
            if (r.name().equalsIgnoreCase(s) || r.knowledgeType.equalsIgnoreCase(s)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown ChatType: " + input);
    }
}
