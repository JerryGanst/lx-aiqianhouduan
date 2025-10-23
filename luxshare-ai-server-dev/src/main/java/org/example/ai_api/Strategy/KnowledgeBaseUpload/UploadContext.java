package org.example.ai_api.Strategy.KnowledgeBaseUpload;

import org.example.ai_api.Bean.Enum.KnowledgeFileUpload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 *  知识库上传方式策略路由
 */
@Component
public class UploadContext {

    private final List<KnowledgeUploadStrategy> strategies;

    @Autowired
    public UploadContext(List<KnowledgeUploadStrategy> strategies) {
        this.strategies = strategies;
    }

    public KnowledgeUploadStrategy getStrategy(KnowledgeFileUpload type) {
        return strategies.stream()
                .filter(strategy -> strategy.type(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知上传方式: " + type));
    }
}
