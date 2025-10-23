package org.example.ai_api.Service.Apis.Applications;

import org.example.ai_api.Bean.ApiRepeat.ChatStreamRepeat;
import org.example.ai_api.Bean.ApiRepeat.TranslateRepeat;
import org.example.ai_api.Bean.ApiRepeat.UnifiedStreamEvent;
import org.example.ai_api.Bean.ApiRepeat.UpstreamSseEvent;
import org.example.ai_api.Bean.ApiRequests.TranslateRequest;
import org.example.ai_api.Bean.Entity.UserInfo;
import org.example.ai_api.Bean.Model.FileId;
import org.example.ai_api.Bean.WebRequest.Translate;
import org.example.ai_api.Bean.WebRequest.UnifiedChatStream;
import org.example.ai_api.Config.AIConfig;
import org.example.ai_api.Service.Apis.Commons.FileProcessor;
import org.example.ai_api.Service.Apis.Commons.SseStreamTransformer;
import org.example.ai_api.Service.Apis.Commons.StreamHub;
import org.example.ai_api.Service.Apis.Infrastructure.AIClient;
import org.example.ai_api.Service.Apis.Infrastructure.Factory.TranslateRequestFactory;
import org.example.ai_api.Service.UserInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Objects;

/**
 * 翻译组件
 * @author 10353965
 */
@Component
public class TranslateCase {

    @Autowired
    private AIClient aiClient;
    @Autowired
    private AIConfig aiConfig;
    @Autowired
    private TranslateRequestFactory translateRequestFactory;
    @Autowired
    private FileProcessor fileProcessor;
    @Autowired
    private StreamHub streamHub;

    @Autowired
    private UserInfoService userInfoService;

    private static final Logger log = LoggerFactory.getLogger(TranslateCase.class);

    /**
     * AI翻译.(流式响应)
     *
     * @param translate 请求体
     * @return 翻译结果
     */
    public Flux<ServerSentEvent<UnifiedStreamEvent>> translateStream(Translate translate) throws Exception {
        FileId fileId = translate.getFile();
        String fileContent = fileProcessor.getFileContentOrDefault(fileId, translate.getSource_text());
        TranslateRequest request = translateRequestFactory.processTranslate(translate, fileContent);
        String requestKey = streamHub.keyOf(request.getUserId(), request.getSessionId());
        streamHub.placeHolder(requestKey);

        String translateUrl = aiConfig.getCategories().get("aiChat");
        if (!StringUtils.hasText(translateUrl)) {
            throw new IllegalStateException("aiChat url is not configured");
        }

        if (org.apache.commons.lang3.StringUtils.isNotBlank(translate.getUserId())) {
            UserInfo userInfo = userInfoService.findById(translate.getUserId());
            if (Objects.nonNull(userInfo)) {
                request.setUserDepartment(userInfo.getDepartmentId());
            }
        }

        return aiClient.handleStreamRequest(request, translateUrl, requestKey, UpstreamSseEvent.class)
                .map(e -> ServerSentEvent.builder(SseStreamTransformer.transform(e.data())).build());
    }
}
