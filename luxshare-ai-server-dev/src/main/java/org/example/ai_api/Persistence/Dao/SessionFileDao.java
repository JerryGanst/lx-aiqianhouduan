package org.example.ai_api.Persistence.Dao;

import org.example.ai_api.Bean.Entity.SessionFile;
import org.example.ai_api.Persistence.ConditionBuilder.SessionFileConditionBuilder;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SessionFileDao extends BaseMongoDao<SessionFile> {

    @Override
    protected Class<SessionFile> getEntityClass() {
        return SessionFile.class;
    }

    /**
     * 根据 userId 和 sessionId 查询 SessionFile 列表。
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return 匹配的 SessionFile 列表
     */
    public List<SessionFile> findByUserIdAndSessionId(String userId, String sessionId) {
        return find(
                new SessionFileConditionBuilder()
                        .byFilePathKeyword(userId, sessionId)
        );
    }

    public SessionFile findByObjectName(String objectName) {
        return findOne(
                new SessionFileConditionBuilder()
                        .byObjectName(objectName)
        );
    }

    public List<SessionFile> findByUserIdAndSessionIdAndIteration(String userId, String sessionId, int iteration) {
        return find(
                new SessionFileConditionBuilder()
                        .byFilePathKeyword(userId, sessionId)
                        .byIteration(iteration)
        );
    }
} 
