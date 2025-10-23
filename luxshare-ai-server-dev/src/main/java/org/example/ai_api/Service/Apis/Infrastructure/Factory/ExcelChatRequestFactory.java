package org.example.ai_api.Service.Apis.Infrastructure.Factory;

import org.example.ai_api.Bean.ApiRequests.AIChatRequest;
import org.example.ai_api.Bean.Enum.Action;
import org.example.ai_api.Bean.Model.AIChatMessage;
import org.example.ai_api.Bean.Model.AgentConfig;
import org.example.ai_api.Bean.Model.ContentItem.BaseContentItem;
import org.example.ai_api.Bean.Model.ContentItem.TextContentItem;
import org.example.ai_api.Bean.Model.ExcelChatMessage;
import org.example.ai_api.Bean.WebRequest.ExcelChat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * excel对话请求构造
 * @author 10353965
 */
@Component
public class ExcelChatRequestFactory {

    private static final Logger logger = LoggerFactory.getLogger(ExcelChatRequestFactory.class);

    /**
     * 处理excel对话请求中的excel对话文件
     * @param excelChat 前端excel对话请求
     * @return excel对话请求
     */
    public AIChatRequest processExcelChat(ExcelChat excelChat) {
        logger.info("构造基本的excel对话请求");
        if (excelChat == null) {
            throw new IllegalArgumentException("excelChat must not be null");
        }

        List<ExcelChatMessage> messages = excelChat.getMessages();
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("messages must not be null or empty");
        }

        List<AIChatMessage> convertedMessages = convertMessages(messages);
        AIChatMessage latestMessage = convertedMessages.get(convertedMessages.size() - 1);
        List<AIChatMessage> history = convertedMessages.size() > 1
                ? new ArrayList<>(convertedMessages.subList(0, convertedMessages.size() - 1))
                : new ArrayList<>();

        AIChatRequest request = new AIChatRequest();
        request.setAction(Action.EXCEL_AGENT);
        request.setThinking(excelChat.getModel() == 1);
        request.setUserId(excelChat.getUserId());
        request.setSessionId(excelChat.getSessionId());
        request.setHistory(history);
        request.setMessages(Collections.singletonList(latestMessage));
        request.setCurrentIter(calculateCurrentIter(history));
        request.setAgentConfig(new AgentConfig());
        return request;
    }

    private List<AIChatMessage> convertMessages(List<ExcelChatMessage> messages) {
        List<AIChatMessage> converted = new ArrayList<>(messages.size());
        for (ExcelChatMessage message : messages) {
            converted.add(convertMessage(message));
        }
        return converted;
    }

    private AIChatMessage convertMessage(ExcelChatMessage message) {
        AIChatMessage aiChatMessage = new AIChatMessage();
        if (message == null) {
            aiChatMessage.setContent(new ArrayList<>());
            return aiChatMessage;
        }
        aiChatMessage.setRole(message.getRole() != null ? message.getRole().getValue() : null);
        aiChatMessage.setUploads(message.getFiles());
        aiChatMessage.setToolCalls(message.getToolCalls());
        aiChatMessage.setToolCallId(message.getToolCallId());
        aiChatMessage.setName(message.getName());
        aiChatMessage.setMetadata(message.getMessageMetaData());

        String text = buildTextContent(message);
        List<BaseContentItem> contents = new ArrayList<>();
        contents.add(new TextContentItem(text));
        aiChatMessage.setContent(contents);
        return aiChatMessage;
    }

    private String buildTextContent(ExcelChatMessage message) {
        StringBuilder builder = new StringBuilder();
        if (message.getBefore() != null) {
            builder.append(message.getBefore());
        }
        if (message.getContent() != null) {
            builder.append(message.getContent());
        }
        if (message.getAfter() != null) {
            builder.append(message.getAfter());
        }
        return builder.toString();
    }

    private int calculateCurrentIter(List<AIChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return 1;
        }
        long userCount = history.stream()
                .filter(Objects::nonNull)
                .filter(message -> "user".equals(message.getRole()))
                .count();
        return (int) userCount + 1;
    }

}
