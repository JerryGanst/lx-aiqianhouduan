package org.example.ai_api.Service.Apis.Facade.impl;

import lombok.RequiredArgsConstructor;
import org.example.ai_api.Bean.ApiRepeat.QueryRepeat;
import org.example.ai_api.Bean.WebRequest.Query;
import org.example.ai_api.Service.Apis.Applications.QueryCase;
import org.example.ai_api.Service.Apis.Facade.StreamUseCase;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 公共知识库问答流式用例策略：将 Query 请求委托给 QueryCase 执行。
 * @author 10353965
 */
@Component
@RequiredArgsConstructor
public class QueryStreamUseCase implements StreamUseCase<Query, QueryRepeat> {

    private final QueryCase queryCase;

    @Override
    public Class<Query> requestType() {
        return Query.class;
    }

    @Override
    public Flux<ServerSentEvent<QueryRepeat>> execute(Query request) {
        return queryCase.query(request);
    }
}
