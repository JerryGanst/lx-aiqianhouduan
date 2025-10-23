package org.example.ai_api.Service.Apis.Infrastructure.Factory;

import org.example.ai_api.Bean.ApiRequests.AIChatRequest;
import org.example.ai_api.Bean.Enum.Action;
import org.example.ai_api.Bean.Model.AIChatMessage;
import org.example.ai_api.Bean.Model.ChatMessage;
import org.example.ai_api.Bean.WebRequest.AgentChat;
import org.example.ai_api.Service.Apis.Commons.ConvertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 智能体对话请求构造
 * @author 10353965
 */
@Component
public class AgentChatRequestFactory {

    private static final Logger logger = LoggerFactory.getLogger(AgentChatRequestFactory.class);

    @Autowired
    private ConvertUtils convertUtils;

    /**
     * 对前端的智能体对话请求进行预处理.
     *
     * @param agentChat 前端聊天请求
     * @return 预处理后的聊天请求
     */
    public AIChatRequest processAgentChat(AgentChat agentChat) {
        logger.info("对前端的智能体对话请求进行预处理");
        if (agentChat == null) {
            throw new IllegalArgumentException("agentChat must not be null");
        }
        List<ChatMessage> chatMessages = agentChat.getMessages();
        if (chatMessages == null || chatMessages.isEmpty()) {
            throw new IllegalArgumentException("messages must not be null or empty");
        }

        List<AIChatMessage> convertedMessages = chatMessages.stream()
                .filter(Objects::nonNull)
                .map(convertUtils::convertChatMessage)
                .collect(Collectors.toList());
        if (convertedMessages.isEmpty()) {
            throw new IllegalArgumentException("messages must contain at least one valid item");
        }

        List<AIChatMessage> history = convertUtils.createHistory(convertedMessages);
        List<AIChatMessage> messages = convertUtils.createMessages(convertedMessages);

        AIChatRequest request = new AIChatRequest();
        request.setAction(Action.GENERAL_CHAT);
        request.setThinking(agentChat.getModel() == 1);
        request.setUserId(agentChat.getUserId());
        request.setSessionId(agentChat.getSessionId());
        request.setHistory(history);
        request.setMessages(messages);
        request.setCurrentIter(convertUtils.calculateCurrentIter(history));
        return request;
    }
}
