package org.example.ai_api.Bean.Enum;

import lombok.Getter;

@Getter
public enum SortType {
    NAME("name", "按名称排序"),
    TIME("time", "按时间排序"),
    SIZE("size", "按大小排序");

    private final String code;
    private final String description;

    SortType(String code, String description) {
        this.code = code;
        this.description = description;
    }
} 