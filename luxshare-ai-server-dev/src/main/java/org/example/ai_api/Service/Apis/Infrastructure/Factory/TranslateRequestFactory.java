package org.example.ai_api.Service.Apis.Infrastructure.Factory;

import org.example.ai_api.Bean.ApiRequests.TranslateRequest;
import org.example.ai_api.Bean.Enum.Action;
import org.example.ai_api.Bean.Model.AIChatMessage;
import org.example.ai_api.Bean.Model.AgentConfig;
import org.example.ai_api.Bean.Model.ContentItem.BaseContentItem;
import org.example.ai_api.Bean.Model.ContentItem.TextContentItem;
import org.example.ai_api.Bean.WebRequest.Translate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 翻译请求体构造
 * @author 10353965
 */
@Component
public class TranslateRequestFactory {

    private static final Logger logger = LoggerFactory.getLogger(TranslateRequestFactory.class);

    private static final Map<String,String> map = new HashMap<>();

    static {
        map.put("中文","zh");
        map.put("英文","en");
        map.put("越南语","vi");
        map.put("西班牙语","es");
    }

    /**
     * 对前端的翻译请求进行预处理.
     *
     * @param translate 前端翻译请求
     * @param sourceText 翻译原文（包含文件解析后的内容）
     * @return 预处理后的翻译请求
     */
    public TranslateRequest processTranslate(Translate translate, String sourceText) {
        if (translate == null) {
            throw new IllegalArgumentException("translate must not be null");
        }
        if (!StringUtils.hasText(translate.getUserId())) {
            throw new IllegalArgumentException("user_id must not be blank");
        }
        if (!StringUtils.hasText(translate.getTarget_language())) {
            throw new IllegalArgumentException("target_language must not be blank");
        }
        logger.info("构造翻译工作流请求体");

        TranslateRequest translateRequest = new TranslateRequest();
        translateRequest.setAction(Action.TRANSLATE_WORKFLOW);
        translateRequest.setThinking(false);
        translateRequest.setUserId(translate.getUserId());
        translateRequest.setAgentConfig(new AgentConfig());
        String sessionId = UUID.randomUUID().toString();
        translateRequest.setSessionId(sessionId);
        translateRequest.setCurrentIter(1);

        Map<String, Object> metadataItem = new HashMap<>();
        metadataItem.put("target_language", map.get(translate.getTarget_language()));
        translateRequest.setMetaData(Collections.singletonList(metadataItem));

        List<BaseContentItem> contentItems = new ArrayList<>();
        contentItems.add(new TextContentItem(sourceText == null ? "" : sourceText));

        AIChatMessage message = new AIChatMessage();
        message.setRole("user");
        message.setContent(contentItems);

        translateRequest.setHistory(Collections.emptyList());
        translateRequest.setMessages(Collections.singletonList(message));
        return translateRequest;
    }
}
