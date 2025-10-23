package org.example.ai_api.Persistence.Dao;

import org.example.ai_api.Bean.Entity.ResumeTask;
import org.example.ai_api.Persistence.ConditionBuilder.ResumeTaskConditionBuilder;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ResumeTaskDao extends BaseMongoDao<ResumeTask> {

    @Override
    protected Class<ResumeTask> getEntityClass() {
        return ResumeTask.class;
    }

    public ResumeTask findByBatchId(String batchId) {
        return findOne(
                new ResumeTaskConditionBuilder()
                        .byBatchId(batchId)
                );
    }

    public List<ResumeTask> findByUserId(String userId) {
        // 按最近一次操作时间倒序返回
        return find(
                new ResumeTaskConditionBuilder()
                        .byUserId(userId)
                        .addDescSort("lastUpdateTime")
        );
    }

    // 基于用户ID并按最近操作时间排序，同时支持标题关键字模糊查询
    public List<ResumeTask> findByUserId(String userId, String titleKeyword) {
        return find(
                new ResumeTaskConditionBuilder()
                        .byUserId(userId)
                        .addKeywordCondition("title", titleKeyword)
                        .addDescSort("lastUpdateTime")
        );
    }

    public ResumeTask findById(String id) {
        return findOne(
                new ResumeTaskConditionBuilder()
                        .byId(id)
        );
    }

    public List<ResumeTask> findByUserIdAndTitle(String userId, String title) {
        return find(
                new ResumeTaskConditionBuilder()
                        .byUserId(userId)
                        .byTitle(title)
                );
    }
}
