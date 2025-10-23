package org.example.ai_api.Service.Apis.Facade.impl;

import lombok.RequiredArgsConstructor;
import org.example.ai_api.Bean.ApiRepeat.UnifiedChatRepeat;
import org.example.ai_api.Bean.WebRequest.ExcelTranslate;
import org.example.ai_api.Service.Apis.Applications.ExcelTranslateCase;
import org.example.ai_api.Service.Apis.Facade.StreamUseCase;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class ExcelTranslateUseCase implements StreamUseCase<ExcelTranslate, UnifiedChatRepeat> {

    private final ExcelTranslateCase excelTranslateCase;

    @Override
    public Class<ExcelTranslate> requestType() {
        return ExcelTranslate.class;
    }

    @Override
    public Flux<ServerSentEvent<UnifiedChatRepeat>> execute(ExcelTranslate request) throws Exception {
        return excelTranslateCase.excelTranslate(request);
    }
}
