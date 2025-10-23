package org.example.ai_api.Service.Apis.Facade.impl;

import lombok.RequiredArgsConstructor;
import org.example.ai_api.Bean.WebRequest.AgentSetting;
import org.example.ai_api.Service.Apis.Applications.AgentSettingCase;
import org.example.ai_api.Service.Apis.Facade.SyncUseCase;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 *  智能体设定同步用例策略：将 AgentSetting 请求委托给 AgentSettingCase 执行。
 * @author 10353965
 */
@Component
@RequiredArgsConstructor
public class AgentSettingSyncUseCase implements SyncUseCase<AgentSetting, AgentSetting> {

    private final AgentSettingCase agentSettingCase;

    @Override
    public Class<AgentSetting> requestType() {
        return AgentSetting.class;
    }

    @Override
    public Mono<AgentSetting> execute(AgentSetting request) throws Exception {
        return Mono.fromCallable(() -> agentSettingCase.generateAgentSetting(request))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
