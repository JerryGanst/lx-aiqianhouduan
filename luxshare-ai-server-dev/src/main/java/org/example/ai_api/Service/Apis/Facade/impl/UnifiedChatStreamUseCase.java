package org.example.ai_api.Service.Apis.Facade.impl;

import lombok.RequiredArgsConstructor;
import org.example.ai_api.Bean.ApiRepeat.UnifiedChatRepeat;
import org.example.ai_api.Bean.ApiRepeat.UnifiedStreamEvent;
import org.example.ai_api.Bean.ApiRepeat.UpstreamSseEvent;
import org.example.ai_api.Bean.WebRequest.UnifiedChatStream;
import org.example.ai_api.Service.Apis.Applications.UnifiedChatCase;
import org.example.ai_api.Service.Apis.Facade.StreamUseCase;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 统一通用对话流式用例策略：将 UnifiedChatStream 请求委托给 UnifiedChatCase 执行。
 * @author 10353965
 */
@Component
@RequiredArgsConstructor
public class UnifiedChatStreamUseCase implements StreamUseCase<UnifiedChatStream, UnifiedStreamEvent> {

    private final UnifiedChatCase unifiedChatCase;

    @Override
    public Class<UnifiedChatStream> requestType() {
        return UnifiedChatStream.class;
    }

    @Override
    public Flux<ServerSentEvent<UnifiedStreamEvent>> execute(UnifiedChatStream request) throws Exception {
        return unifiedChatCase.unifiedChat(request);
    }
}
