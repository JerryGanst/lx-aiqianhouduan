package org.example.ai_api.Persistence.ConditionBuilder;

public class SessionFileConditionBuilder extends BaseConditionBuilder<SessionFileConditionBuilder> {

    /**
     * 根据 userId 和 sessionId 构建 filePath 的前缀模糊查询条件。
     * 假设 filePath 的格式是 ai-artifacts/{userId}/{sessionId}/...
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return 当前构建器实例
     */
    public SessionFileConditionBuilder byFilePathKeyword(String userId, String sessionId) {
        return addKeywordCondition("objectName", String.format("%s/%s", userId, sessionId));
    }

    public SessionFileConditionBuilder byObjectName(String objectName) {
        return addCondition("objectName", objectName);
    }

    public SessionFileConditionBuilder byIteration(int iteration) {
        return addCondition("iteration", iteration);
    }
} 
