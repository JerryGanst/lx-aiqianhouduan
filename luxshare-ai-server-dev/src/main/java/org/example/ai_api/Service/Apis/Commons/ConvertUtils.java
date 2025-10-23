package org.example.ai_api.Service.Apis.Commons;

import org.example.ai_api.Bean.Model.AIChatMessage;
import org.example.ai_api.Bean.Model.ChatMessage;
import org.example.ai_api.Bean.Model.ContentItem.BaseContentItem;
import org.example.ai_api.Bean.Model.ContentItem.TextContentItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ConvertUtils{

    public AIChatMessage convertChatMessage(ChatMessage chatMessage) {
        AIChatMessage aiChatMessage = new AIChatMessage();
        if (chatMessage == null) {
            aiChatMessage.setContent(new ArrayList<>());
            return aiChatMessage;
        }
        aiChatMessage.setRole(chatMessage.getRole());
        aiChatMessage.setUploads(chatMessage.getUploads());
        aiChatMessage.setPersonalKnowledge(chatMessage.getPersonalKnowledge());

        String text = chatMessage.getContent() != null ? chatMessage.getContent() : "";
        List<BaseContentItem> contentItems = new ArrayList<>();
        contentItems.add(new TextContentItem(text));
        aiChatMessage.setContent(contentItems);
        return aiChatMessage;
    }

    public int calculateCurrentIter(List<AIChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return 1;
        }
        long userCount = history.stream()
                .filter(message -> "user".equals(message.getRole()))
                .count();
        return (int) userCount + 1;
    }

    public List<AIChatMessage> createHistory(List<AIChatMessage> convertedMessages){
        int lastIndex = convertedMessages.size() - 1;
        return lastIndex > 0
                ? new ArrayList<>(convertedMessages.subList(0, lastIndex))
                : new ArrayList<>();
    }

    public List<AIChatMessage> createMessages(List<AIChatMessage> convertedMessages) {
        int lastIndex = convertedMessages.size() - 1;
        AIChatMessage latestMessage = convertedMessages.get(lastIndex);
        List<AIChatMessage> messages = new ArrayList<>();
        messages.add(latestMessage);
        return messages;
    }
}
