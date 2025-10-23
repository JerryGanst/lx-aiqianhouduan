package org.example.ai_api.Service.Apis.Facade;

import reactor.core.publisher.Mono;

/**
 * 同步用例策略接口：
 * - 输入请求 RQ，输出响应 RS 的 Mono 包装
 * - 适用于需要同步结果（如总结）的场景
 */
public interface SyncUseCase<RQ, RS> {
    /**
     * 返回该策略支持的请求类型
     */
    Class<RQ> requestType();

    /**
     * 执行同步用例，返回 Mono 包装的结果
     */
    Mono<RS> execute(RQ request) throws Exception;
}
