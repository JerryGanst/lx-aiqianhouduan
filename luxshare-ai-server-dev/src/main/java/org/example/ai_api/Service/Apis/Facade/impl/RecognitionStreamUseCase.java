package org.example.ai_api.Service.Apis.Facade.impl;

import lombok.RequiredArgsConstructor;
import org.example.ai_api.Bean.ApiRepeat.UnifiedStreamEvent;
import org.example.ai_api.Bean.WebRequest.ImageRecognition;
import org.example.ai_api.Service.Apis.Applications.RecognitionCase;
import org.example.ai_api.Service.Apis.Facade.StreamUseCase;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 图片识别/对比流式用例策略：将 ImageRecognition 请求委托给 RecognitionCase 执行。
 * @author 10353965
 */
@Component
@RequiredArgsConstructor
public class RecognitionStreamUseCase implements StreamUseCase<ImageRecognition, UnifiedStreamEvent> {

    private final RecognitionCase recognitionCase;

    @Override
    public Class<ImageRecognition> requestType() {
        return ImageRecognition.class;
    }

    @Override
    public Flux<ServerSentEvent<UnifiedStreamEvent>> execute(ImageRecognition request) throws Exception {
        return recognitionCase.imgRecognition(request);
    }
}
