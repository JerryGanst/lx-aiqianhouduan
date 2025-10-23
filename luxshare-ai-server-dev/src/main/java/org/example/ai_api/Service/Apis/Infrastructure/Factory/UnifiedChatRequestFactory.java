package org.example.ai_api.Service.Apis.Infrastructure.Factory;

import org.example.ai_api.Bean.ApiRequests.AIChatRequest;
import org.example.ai_api.Bean.ApiRequests.UnifiedChatRequest;
import org.example.ai_api.Bean.Enum.Action;
import org.example.ai_api.Bean.Model.AIChatMessage;
import org.example.ai_api.Bean.Model.AgentConfig;
import org.example.ai_api.Bean.WebRequest.UnifiedChatStream;
import org.example.ai_api.Service.Apis.Commons.ConvertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用对话接口请求体构造
 * @author 10353965
 */
@Component
public class UnifiedChatRequestFactory {

    private static final Logger logger = LoggerFactory.getLogger(UnifiedChatRequestFactory.class);

    @Autowired
    private ConvertUtils convertUtils;

    /**
     * 根据前端请求构造基本的统一问答请求对象
     *
     * @param unifiedChatStream 前端统一问答请求
     * @return 基本统一问答请求
     */
    public AIChatRequest processUnifiedChat(UnifiedChatStream unifiedChatStream) {
        logger.info("对前端的统一问答请求进行预处理");
        if (unifiedChatStream == null) {
            throw new IllegalArgumentException("unifiedChatStream must not be null");
        }
        List<AIChatMessage> chatMessages = unifiedChatStream.getMessages();
        if (chatMessages == null || chatMessages.isEmpty()) {
            throw new IllegalArgumentException("messages must not be null or empty");
        }

        AIChatRequest request = new AIChatRequest();
        request.setAction(Action.GENERAL_CHAT);
        request.setThinking(unifiedChatStream.getModel() == 1);
        request.setUserId(unifiedChatStream.getUserId());
        request.setSessionId(unifiedChatStream.getSessionId());
        List<AIChatMessage> history = convertUtils.createHistory(chatMessages);
        List<AIChatMessage> messages = convertUtils.createMessages(chatMessages);
        request.setHistory(history);
        request.setMessages(messages);
        request.setAgentConfig(new AgentConfig());
        request.setCurrentIter(convertUtils.calculateCurrentIter(history));
        return request;
    }

}
