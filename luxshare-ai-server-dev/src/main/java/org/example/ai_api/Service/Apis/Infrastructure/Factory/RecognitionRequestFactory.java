package org.example.ai_api.Service.Apis.Infrastructure.Factory;

import org.example.ai_api.Bean.ApiRequests.ImageRecognitionRequest;
import org.example.ai_api.Bean.Enum.Action;
import org.example.ai_api.Bean.Model.*;
import org.example.ai_api.Bean.Model.ContentItem.BaseContentItem;
import org.example.ai_api.Bean.Model.ContentItem.ImageUrlContentItem;
import org.example.ai_api.Bean.Model.ContentItem.TextContentItem;
import org.example.ai_api.Bean.WebRequest.ImageRecognition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 图像识别请求体构造
 *
 * @author 10353965
 */
@Component
public class RecognitionRequestFactory {

    private static final Logger logger = LoggerFactory.getLogger(RecognitionRequestFactory.class);

    /**
     * 处理图片识别请求，将图片识别请求封装为request
     *
     * @param imageRecognition 图片识别请求
     * @return 封装后的request
     */
    public ImageRecognitionRequest processImageRecognition(ImageRecognition imageRecognition) {
        if (imageRecognition == null) {
            throw new IllegalArgumentException("imageRecognition must not be null");
        }
        if (!StringUtils.hasText(imageRecognition.getUserId())) {
            throw new IllegalArgumentException("user_id must not be blank");
        }
        if (!StringUtils.hasText(imageRecognition.getSessionId())) {
            throw new IllegalArgumentException("session_id must not be blank");
        }

        logger.info("处理图片识别请求");
        ImageRecognitionRequest imageRecognitionRequest = new ImageRecognitionRequest();
        imageRecognitionRequest.setAction(Action.IMAGE_COMPARE);
        imageRecognitionRequest.setThinking(false);
        imageRecognitionRequest.setUserId(imageRecognition.getUserId());
        imageRecognitionRequest.setSessionId(imageRecognition.getSessionId());
        imageRecognitionRequest.setAgentConfig(new AgentConfig());

        List<AIChatMessage> history = convertHistory(imageRecognition.getMessages());
        imageRecognitionRequest.setHistory(history);
        imageRecognitionRequest.setMessages(new ArrayList<>());
        imageRecognitionRequest.setCurrentIter(calculateCurrentIter(history));
        return imageRecognitionRequest;
    }

    private List<AIChatMessage> convertHistory(List<BaseMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }
        List<AIChatMessage> converted = new ArrayList<>();
        for (BaseMessage message : messages) {
            AIChatMessage aiChatMessage = convertMessage(message);
            if (aiChatMessage != null) {
                converted.add(aiChatMessage);
            }
        }
        return converted;
    }

    private AIChatMessage convertMessage(BaseMessage message) {
        if (message == null) {
            return null;
        }
        if (message instanceof UserMessage) {
            return convertUserMessage((UserMessage) message);
        }
        if (message instanceof AssistantMessage) {
            return convertAssistantMessage((AssistantMessage) message);
        }
        logger.warn("未知的消息类型: {}", message.getClass().getName());
        return null;
    }

    private AIChatMessage convertUserMessage(UserMessage userMessage) {
        AIChatMessage aiChatMessage = new AIChatMessage();
        aiChatMessage.setRole("user");
        List<BaseContentItem> contentItems = new ArrayList<>();
        if (userMessage.getContent() != null) {
            for (ImgContent item : userMessage.getContent()) {
                BaseContentItem convertedItem = convertImgContent(item);
                if (convertedItem != null) {
                    contentItems.add(convertedItem);
                }
            }
        }
        if (contentItems.isEmpty()) {
            contentItems.add(new TextContentItem(""));
        }
        aiChatMessage.setContent(contentItems);
        return aiChatMessage;
    }

    private AIChatMessage convertAssistantMessage(AssistantMessage assistantMessage) {
        AIChatMessage aiChatMessage = new AIChatMessage();
        aiChatMessage.setRole("assistant");
        List<BaseContentItem> contentItems = new ArrayList<>();
        if (assistantMessage.getContent() != null) {
            for (TextContent textContent : assistantMessage.getContent()) {
                String text = textContent != null ? textContent.getText() : "";
                contentItems.add(new TextContentItem(text == null ? "" : text));
            }
        }
        if (contentItems.isEmpty()) {
            contentItems.add(new TextContentItem(""));
        }
        aiChatMessage.setContent(contentItems);
        return aiChatMessage;
    }

    private BaseContentItem convertImgContent(ImgContent content) {
        if (content == null) {
            return null;
        }
        if (content instanceof TextContent) {
            String text = ((TextContent) content).getText();
            return new TextContentItem(text == null ? "" : text);
        }
        if (content instanceof org.example.ai_api.Bean.Model.ImageUrl) {
            org.example.ai_api.Bean.Model.ImageUrl legacyImageUrl = (org.example.ai_api.Bean.Model.ImageUrl) content;
            Url url = legacyImageUrl.getUrl();
            if (url != null && StringUtils.hasText(url.getUrl())) {
                ImageUrlContentItem item = new ImageUrlContentItem();
                org.example.ai_api.Bean.Model.ContentItem.ImageUrl imageUrl =
                        new org.example.ai_api.Bean.Model.ContentItem.ImageUrl();
                imageUrl.setUrl(url.getUrl());
                item.setImageUrl(imageUrl);
                return item;
            }
        }
        return null;
    }

    private int calculateCurrentIter(List<AIChatMessage> history) {
        if (CollectionUtils.isEmpty(history)) {
            return 1;
        }
        long userCount = history.stream()
                .filter(Objects::nonNull)
                .filter(message -> "user".equals(message.getRole()))
                .count();
        return (int) userCount + 1;
    }
}
