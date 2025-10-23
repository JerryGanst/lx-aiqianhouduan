package org.example.ai_api.Persistence.ConditionBuilder;

public class ExcelChatConditionBuilder extends BaseConditionBuilder<ExcelChatConditionBuilder>{

    public ExcelChatConditionBuilder byUserId(String userId){
        return addCondition("userId", userId);
    }

    public ExcelChatConditionBuilder byChatId(String chatId){
        return addCondition("chatId", chatId);
    }

    public ExcelChatConditionBuilder byTitleKeyword(String keyword){
        return addKeywordCondition("title", keyword);
    }
}
