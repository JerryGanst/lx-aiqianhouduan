package org.example.ai_api.Service.Apis.Infrastructure;

import org.example.ai_api.Exception.RetryableApiException;
import org.example.ai_api.Exception.StreamApiException;
import org.example.ai_api.Exception.SyncApiException;
import org.example.ai_api.Exception.ThirdPartyDataException;
import org.example.ai_api.Service.Apis.Commons.StreamHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * 专门负责AI请求的发送，包括流式与非流式请求
 * @author 10353965
 */
@Component
public class AIClient {

    private static final Logger logger = LoggerFactory.getLogger(AIClient.class);

    @Autowired
    @Qualifier("SyncWebClient")
    private WebClient syncWebClient;
    @Autowired
    @Qualifier("StreamWebClient")
    private WebClient streamWebClient;
    @Autowired
    private StreamHub streamHub;

    /**
     * 流式返回.
     * @param requestBody 请求结构体
     * @param url 请求url
     * @param requestKey 请求key
     * @param responseType 返回类型
     * @return 流式返回结果
     * @param <T> 返回类型
     */
    public <T> Flux<ServerSentEvent<T>> handleStreamRequest(
            Object requestBody,
            String url,
            String requestKey,
            Class<T> responseType
    ) {
        return streamWebClient
                .post()
                .uri(url)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(BodyInserters.fromValue(requestBody))
                .exchangeToFlux(response -> {
                    if (response.statusCode().isError()) {
                        return response.bodyToMono(String.class)
                                .flatMapMany(errorBody ->
                                        // 抛出异常，触发全局错误处理
                                        Flux.error(new StreamApiException(
                                                response.rawStatusCode(),
                                                errorBody
                                        ))
                                );
                    }
                    return response.bodyToFlux(responseType)
                            .map(item -> ServerSentEvent.builder(item).build());
                })
                .doOnSubscribe(subscription -> {
                    streamHub.bind(requestKey, subscription);
                    logger.info("Subscribe{}",requestKey);
                })
                .doOnTerminate(() -> {
                    logger.info("Terminate{}",requestKey);
                })
                .doOnCancel(() -> {
                    logger.info("Cancel{}",requestKey);
                })
                .doOnError(e -> {
                    logger.info("Error{}",requestKey);
                })
                .doFinally(signalType -> {
                    logger.info("Finally{}",requestKey);
                    if(streamHub.release(requestKey)){
                        logger.info("流成功取消，requestKey:{}",requestKey);
                    }else {
                        logger.info("流取消异常，requestKey:{}",requestKey);
                    }
                });
    }

    /**
     * 同步请求.
     * @param request 请求结构体
     * @param url 请求url
     * @param responseType 返回类型
     * @return 同步返回结果
     * @param <T> 返回类型
     */
    public <T> T handleSyncRequest(
            Object request,
            String url,
            Class<T> responseType
    ) {
        return syncWebClient
                .post()
                .uri(url)
                .body(BodyInserters.fromValue(request))
                .retrieve()
                .onStatus(HttpStatus::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(error -> {
                                    int statusCode = response.rawStatusCode();
                                    logger.warn("请求失败，错误码：{}, 错误信息：{}", statusCode, error);
                                    // 针对5xx错误创建可重试异常
                                    if (statusCode >= 500 && statusCode < 600) {
                                        return Mono.error(new RetryableApiException(statusCode, error));
                                    } else {
                                        return Mono.error(new SyncApiException(statusCode, error));
                                    }
                                })
                )
                .bodyToMono(responseType)
                // 添加重试机制 (只重试5xx错误和网络异常)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(throwable ->
                                throwable instanceof RetryableApiException || throwable instanceof WebClientRequestException
                        )
                        .doAfterRetry(retrySignal ->
                                logger.debug("重试次数: {}", retrySignal.totalRetries())
                        )
                )
                .blockOptional()
                .orElseThrow(() -> new ThirdPartyDataException("返回体为空"));
    }
}
