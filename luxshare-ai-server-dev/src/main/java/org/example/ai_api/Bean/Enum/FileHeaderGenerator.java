package org.example.ai_api.Bean.Enum;

import lombok.Getter;

@Getter
public enum FileHeaderGenerator {
    PREVIEW("inline"),   // 预览模式
    DOWNLOAD("attachment"); // 下载模式

    private final String dispositionType;

    FileHeaderGenerator(String dispositionType) {
        this.dispositionType = dispositionType;
    }

}
