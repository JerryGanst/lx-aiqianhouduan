package org.example.ai_api.Strategy.KnowledgeBase;

import org.example.ai_api.Config.AIConfig;
import org.springframework.stereotype.Component;

/**
 * 知识库路径选择策略模式接口具体实现 - 董办专题.
 * @author 10353965
 */
@Component
public class ChmStrategy implements KnowledgeBaseStrategy{

    @Override
    public String getType() {
        return "董办专题";
    }

    @Override
    public String getUrl(AIConfig aiConfig) {
        return aiConfig.getCategories().get("CHM");
    }
}
