package org.example.ai_api.Persistence.ConditionBuilder;

public class HistoryConditionBuilder extends BaseConditionBuilder<HistoryConditionBuilder>{
    public HistoryConditionBuilder byUserId(String userid) {
        return addCondition("userid", userid);
    }

    public HistoryConditionBuilder byType(String type) {
        return addCondition("type", type);
    }

    public HistoryConditionBuilder byDateRange(String start, String end) {
        return addRange("date", start, end);
    }

    public HistoryConditionBuilder byDateAfter(String start) {
        return addRange("date", start, null);
    }

    public HistoryConditionBuilder byDateBefore(String end) {
        return addRange("date", null, end);
    }

    public HistoryConditionBuilder byTitleKeyword(String keyword){
        return addKeywordCondition("title", keyword);
    }
}
