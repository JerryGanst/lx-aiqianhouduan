package org.example.ai_api.Service.Apis.Facade.impl;

import lombok.RequiredArgsConstructor;
import org.example.ai_api.Bean.ApiRepeat.SummaryRepeat;
import org.example.ai_api.Bean.WebRequest.Summary;
import org.example.ai_api.Service.Apis.Applications.SummaryCase;
import org.example.ai_api.Service.Apis.Facade.SyncUseCase;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 文本总结同步用例策略：将 Summary 请求委托给 SummaryCase 执行，
 * 并用 Mono 封装以便在响应式管线中组合。
 * @author 10353965
 */
@Component
@RequiredArgsConstructor
public class SummarySyncUseCase implements SyncUseCase<Summary, SummaryRepeat> {

    private final SummaryCase summaryCase;

    @Override
    public Class<Summary> requestType() {
        return Summary.class;
    }

    @Override
    public Mono<SummaryRepeat> execute(Summary request) throws Exception {
        return Mono.fromCallable(() -> summaryCase.summary(request))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
