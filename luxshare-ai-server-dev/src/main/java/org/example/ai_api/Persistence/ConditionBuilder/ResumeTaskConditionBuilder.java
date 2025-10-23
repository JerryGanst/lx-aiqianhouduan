package org.example.ai_api.Persistence.ConditionBuilder;

public class ResumeTaskConditionBuilder extends BaseConditionBuilder<ResumeTaskConditionBuilder>{
    public ResumeTaskConditionBuilder byUserId(String userid) {
        return addCondition("userId", userid);
    }

    public ResumeTaskConditionBuilder byBatchId(String batchId) {
        return addCondition("resumeRepeat.batchId", batchId);
    }

    public ResumeTaskConditionBuilder byId(String id) {
        return addCondition("id", id);
    }

    public BaseConditionBuilder<?> byTitle(String title) {
        return addCondition("title", title);
    }
}
