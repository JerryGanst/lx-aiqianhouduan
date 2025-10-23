package org.example.ai_api.Service.Apis.Facade.impl;

import lombok.RequiredArgsConstructor;
import org.example.ai_api.Bean.ApiRepeat.UnifiedStreamEvent;
import org.example.ai_api.Bean.WebRequest.ExcelChat;
import org.example.ai_api.Service.Apis.Applications.ExcelChatCase;
import org.example.ai_api.Service.Apis.Facade.StreamUseCase;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Excel 对话流式用例策略：将 ExcelChat 请求委托给 ExcelChatCase 执行。
 * @author 10353965
 */
@Component
@RequiredArgsConstructor
public class ExcelChatStreamUseCase implements StreamUseCase<ExcelChat, UnifiedStreamEvent> {

    private final ExcelChatCase excelChatCase;

    @Override
    public Class<ExcelChat> requestType() {
        return ExcelChat.class;
    }

    @Override
    public Flux<ServerSentEvent<UnifiedStreamEvent>> execute(ExcelChat request) {
        return excelChatCase.excelChat(request);
    }
}
