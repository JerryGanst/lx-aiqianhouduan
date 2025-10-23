package org.example.ai_api.Strategy.KnowledgeBase;

import org.example.ai_api.Config.AIConfig;

/**
 * 知识库路径选择策略模式接口.
 * @author 10353965
 */
public interface KnowledgeBaseStrategy {
    /**
     *  获取策略对应的类型标识
     * @return  String
     */
    String getType();

    /**
     *  获取URL
     * @param aiConfig  AIConfig
     * @return   String
     */
    String getUrl(AIConfig aiConfig);
}
