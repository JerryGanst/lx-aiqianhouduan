package org.example.ai_api.Service.Apis.Applications;

import org.example.ai_api.Bean.ApiRepeat.UnifiedStreamEvent;
import org.example.ai_api.Bean.ApiRepeat.UpstreamSseEvent;
import org.example.ai_api.Bean.ApiRequests.AIChatRequest;
import org.example.ai_api.Bean.WebRequest.ExcelChat;
import org.example.ai_api.Config.AIConfig;
import org.example.ai_api.Exception.ThirdPartyDataException;
import org.example.ai_api.Service.Apis.Commons.*;
import org.example.ai_api.Service.Apis.Infrastructure.AIClient;
import org.example.ai_api.Service.Apis.Infrastructure.Factory.ExcelChatRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 *  excel对话组件
 * @author 10353965
 */
@Component
public class ExcelChatCase {

    private static final Logger logger = LoggerFactory.getLogger(ExcelChatCase.class);

    @Autowired
    private AIClient aiClient;
    @Autowired
    private AIConfig aiConfig;
    @Autowired
    private StreamHub streamHub;
    @Autowired
    private ServerSentEventCreator serverSentEventCreator;
    @Autowired
    private FileProcessor fileProcessor;
    @Autowired
    private ExcelChatRequestFactory excelChatRequestFactory;
    @Autowired
    private UserInfoGetter userInfoGetter;

    /**
     * excel问答
     * @param excelChat excel问答请求
     * @return excel问答结果(流式)
     */
    public Flux<ServerSentEvent<UnifiedStreamEvent>> excelChat(ExcelChat excelChat) {
        logger.info("excel问答 {}",excelChat.getSessionId());
        //构造基本的ai侧请求体
        AIChatRequest excelChatRequest = excelChatRequestFactory.processExcelChat(excelChat);
        fileProcessor.addExcelFilesToRequest(excelChatRequest,excelChat.getFiles());
        excelChatRequest.setUserDepartment(userInfoGetter.getDepartmentId(excelChat.getUserId()));
        //流式请求获取requestKey
        String requestKey = streamHub.keyOf(excelChatRequest.getUserId(), excelChatRequest.getSessionId());
        //流式请求占位
        streamHub.placeHolder(requestKey);
        //发送请求,获取上游返回结果
        Flux<ServerSentEvent<UpstreamSseEvent>> raw = aiClient.handleStreamRequest(excelChatRequest, aiConfig.getCategories().get("aiChat"), requestKey, UpstreamSseEvent.class);
        return raw.publish(shared -> {
            // 5.1 主流：把 error → 异常；遇到 complete_context 即结束
            Flux<ServerSentEvent<UpstreamSseEvent>> main =
                    shared.handle((evt, sink) -> {
                        UpstreamSseEvent d = evt.data();
                        if (d != null) {
                            if ("error".equals(d.getType())) {
                                sink.error(new ThirdPartyDataException("第三方错误: " + d.getContent()));
                                return;
                            }
                            if ("complete_context".equals(d.getType())) {
                                sink.next(evt);    // 把完结事件本身也传给前端
                                sink.complete();   // 主流到此为止
                                return;
                            }
                        }
                        sink.next(evt);
                    });

            // 5.2 是否见到完结事件
            Mono<Boolean> sawComplete =
                    shared.any(evt -> {
                        UpstreamSseEvent d = evt.data();
                        return d != null && "complete_context".equals(d.getType());
                    });

            // 5.3 只在 sawComplete==true 时，串联 MinIO 扫描结果（同一订阅，不会重拉上游）
            Flux<ServerSentEvent<UpstreamSseEvent>> tail =
                    sawComplete.flatMapMany(seen -> {
                        if (!seen){
                            return Flux.empty();
                        }
                        int iter = excelChat.getMessages().size() / 2 + 1;
                        return serverSentEventCreator.scanMinioForSessionFiles(excelChat.getSessionId(), excelChat.getUserId(), iter);
                    });

            // 5.4 拼接最终状态
            Flux<ServerSentEvent<UpstreamSseEvent>> finalStatus =
                    Flux.just(ServerSentEvent.<UpstreamSseEvent>builder()
                            .data(UpstreamSseEvent.builder()
                                    .type("final")
                                    .content("所有处理阶段已完成。")
                                    .timestamp(System.currentTimeMillis())
                                    .build())
                            .build());

            return main.concatWith(tail).concatWith(finalStatus);
        }).map(evt -> ServerSentEvent.builder(SseStreamTransformer.transform(evt.data())).build());
    }
}
