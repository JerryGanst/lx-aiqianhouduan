package org.example.ai_api.Service.Apis.Facade;

import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * 流式用例策略接口：
 * - 输入请求 RQ，输出 SSE 流载荷 RS
 * - 由门面根据请求类型进行策略分发
 */
public interface StreamUseCase<RQ, RS> {
    /**
     * 返回该策略支持的请求类型
     */
    Class<RQ> requestType();

    /**
     * 执行流式用例，返回对应的 SSE 流
     */
    Flux<ServerSentEvent<RS>> execute(RQ request) throws Exception;
}
