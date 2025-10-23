package org.example.ai_api.Service.Apis.Facade.impl;

import lombok.RequiredArgsConstructor;
import org.example.ai_api.Bean.ApiRepeat.ResumeRepeat;
import org.example.ai_api.Bean.WebRequest.Resume;
import org.example.ai_api.Service.Apis.Applications.ResumeCase;
import org.example.ai_api.Service.Apis.Facade.SyncUseCase;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 *  简历同步用例策略
 */
@Component
@RequiredArgsConstructor
public class ResumeSyncUseCase implements SyncUseCase<Resume, ResumeRepeat> {

    private final ResumeCase resumeCase;

    @Override
    public Class<Resume> requestType() {
        return Resume.class;
    }

    @Override
    public Mono<ResumeRepeat> execute(Resume request) throws Exception {
        return Mono.fromCallable(() -> resumeCase.resume(request))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
