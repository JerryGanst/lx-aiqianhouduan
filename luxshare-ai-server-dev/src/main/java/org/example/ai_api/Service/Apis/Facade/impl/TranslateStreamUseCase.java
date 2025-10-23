package org.example.ai_api.Service.Apis.Facade.impl;

import lombok.RequiredArgsConstructor;
import org.example.ai_api.Bean.ApiRepeat.TranslateRepeat;
import org.example.ai_api.Bean.ApiRepeat.UnifiedStreamEvent;
import org.example.ai_api.Bean.WebRequest.Translate;
import org.example.ai_api.Bean.WebRequest.UnifiedChatStream;
import org.example.ai_api.Service.Apis.Applications.TranslateCase;
import org.example.ai_api.Service.Apis.Facade.StreamUseCase;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 翻译流式用例策略：将 Translate 请求委托给 TranslateCase 执行。
 * @author 10353965
 */
@Component
@RequiredArgsConstructor
public class TranslateStreamUseCase implements StreamUseCase<Translate, UnifiedStreamEvent> {

    private final TranslateCase translateCase;

    @Override
    public Class<Translate> requestType() {
        return Translate.class;
    }

    @Override
    public Flux<ServerSentEvent<UnifiedStreamEvent>> execute(Translate request) throws Exception {
        return translateCase.translateStream(request);
    }
}
