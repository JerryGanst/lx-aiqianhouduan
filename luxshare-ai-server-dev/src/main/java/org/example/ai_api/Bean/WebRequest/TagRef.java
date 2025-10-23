package org.example.ai_api.Bean.WebRequest;

import lombok.Data;

@Data
public class TagRef {
    private String id;           // 已存在标签ID（可选）
    private String targetName;   // 标签名称（可选，当id缺失时使用）
}
