package org.example.ai_api.Persistence.ConditionBuilder;

public class ImageRecognitionConditionBuilder extends BaseConditionBuilder<ImageRecognitionConditionBuilder>{
    public ImageRecognitionConditionBuilder byUserId(String userId) {
        return addCondition("userId", userId);
    }
    public ImageRecognitionConditionBuilder byImageId(String imageId) {
        return addCondition("id", imageId);
    }
    public ImageRecognitionConditionBuilder byTitleWithKeyword(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return this;
        }
        return addKeywordCondition("title", keyword);
    }
}
