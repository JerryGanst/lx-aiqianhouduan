package org.example.ai_api.Service.Apis.Facade.impl;

import lombok.RequiredArgsConstructor;
import org.example.ai_api.Bean.ApiRepeat.UnifiedStreamEvent;
import org.example.ai_api.Bean.WebRequest.AgentChat;
import org.example.ai_api.Service.Apis.Applications.AgentChatCase;
import org.example.ai_api.Service.Apis.Facade.StreamUseCase;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 智能体对话流式用例策略：将 AgentChat 请求委托给 AgentChatCase 执行。
 * @author 10353965
 */
@Component
@RequiredArgsConstructor
public class AgentChatStreamUseCase implements StreamUseCase<AgentChat, UnifiedStreamEvent> {

    private final AgentChatCase agentChatCase;

    @Override
    public Class<AgentChat> requestType() {
        return AgentChat.class;
    }

    @Override
    public Flux<ServerSentEvent<UnifiedStreamEvent>> execute(AgentChat request) throws Exception {
        return agentChatCase.agentChat(request);
    }
}
