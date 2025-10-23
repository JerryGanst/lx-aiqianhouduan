package org.example.ai_api.Strategy.KnowledgeMultipart;

import org.example.ai_api.Bean.Enum.KnowledgeFileUpload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KnowledgeMultipartContext {
    private final List<KnowledgeMultipartStrategy> strategies;

    @Autowired
    public KnowledgeMultipartContext(List<KnowledgeMultipartStrategy> strategies) {
        this.strategies = strategies;
    }

    public KnowledgeMultipartStrategy getStrategy(KnowledgeFileUpload type) {
        return strategies.stream()
                .filter(s -> s.supports(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知上传类型: " + type));
    }
}

