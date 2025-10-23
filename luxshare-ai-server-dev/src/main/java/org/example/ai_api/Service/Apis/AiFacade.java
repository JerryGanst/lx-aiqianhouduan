package org.example.ai_api.Service.Apis;

import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.ai_api.Bean.ApiRepeat.*;
import org.example.ai_api.Bean.WebRequest.*;
import org.example.ai_api.Service.Apis.Facade.StreamUseCase;
import org.example.ai_api.Service.Apis.Facade.SyncUseCase;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI功能门面
 * @author 10353965
 */
@Service
@RequiredArgsConstructor
public class AiFacade {
    private final List<StreamUseCase<?, ?>> streamUseCases;
    private final List<SyncUseCase<?, ?>> syncUseCases;

    private final Map<Class<?>, StreamUseCase<Object, Object>> streamRegistry = new HashMap<>();
    private final Map<Class<?>, SyncUseCase<Object, Object>> syncRegistry = new HashMap<>();

    /**
     * 初始化策略注册表：将所有流式/同步用例注册到内存 Map，
     * 供后续分发方法按请求类型进行路由。
     */
    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() {
        for (StreamUseCase<?, ?> uc : streamUseCases) {
            streamRegistry.put(uc.requestType(), (StreamUseCase<Object, Object>) uc);
        }
        for (SyncUseCase<?, ?> uc : syncUseCases) {
            syncRegistry.put(uc.requestType(), (SyncUseCase<Object, Object>) uc);
        }
    }

    /**
     * 基于请求对象类型的流式用例分发（SSE），显式指明返回数据类型。
     *
     * <p>当调用方希望避免泛型擦除带来的类型推断问题时，可使用该重载，
     * 通过 {@code responseType} 显式声明返回的数据类型。</p>
     *
     * @param <RQ>         入参请求类型
     * @param <RS>         响应数据载荷类型（SSE 的 data）
     * @param request      具体的请求对象，不能为空
     * @param responseType 响应数据载荷的类型字节码
     * @return 对应场景的 SSE 流
     * @throws Exception 策略执行过程中的异常
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <RQ, RS> Flux<ServerSentEvent<RS>> dispatchStream(RQ request, Class<RS> responseType) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        StreamUseCase<RQ, ?> uc = (StreamUseCase<RQ, ?>) streamRegistry.get(request.getClass());
        if (uc == null) {
            throw new IllegalArgumentException("No stream use case registered for request type: " + request.getClass().getName());
        }
        // The underlying stream use case already produces the correct RS;
        // due to registry erasure we safely cast the Flux type parameter here.
        return (Flux) uc.execute(request);
    }

    /**
     * 基于请求对象类型的同步用例分发（Mono），显式指明返回数据类型。
     *
     * <p>当调用方希望避免类型推断问题或需要更明确的返回类型时，
     * 可通过 {@code responseType} 显式声明。</p>
     *
     * @param <RQ>         入参请求类型
     * @param <RS>         响应数据类型
     * @param request      具体的请求对象，不能为空
     * @param responseType 响应数据类型字节码
     * @return 结果的 Mono 封装
     * @throws Exception 策略执行过程中的异常
     */
    @SuppressWarnings("unchecked")
    public <RQ, RS> Mono<RS> dispatchSync(RQ request, Class<RS> responseType) throws Exception {
        if (request == null) {
            return Mono.error(new IllegalArgumentException("request must not be null"));
        }
        SyncUseCase<RQ, ?> uc = (SyncUseCase<RQ, ?>) syncRegistry.get(request.getClass());
        if (uc == null) {
            return Mono.error(new IllegalArgumentException("No sync use case registered for request type: " + request.getClass().getName()));
        }
        return uc.execute(request).map(responseType::cast);
    }

    /**
     * 智能体对话（流式）。
     *
     * @param agentChat 智能体对话请求
     * @return 对话结果的 SSE 流
     * @throws Exception 策略执行过程中的异常
     */
    public Flux<ServerSentEvent<UnifiedStreamEvent>> agentChat(AgentChat agentChat) throws Exception {
        return this.dispatchStream(agentChat, UnifiedStreamEvent.class);
    }

    /**
     * Excel 数据分析聊天（流式）。
     *
     * @param excelChat Excel 对话请求
     * @return Excel 对话结果的 SSE 流
     * @throws Exception 策略执行过程中的异常
     */
    public Flux<ServerSentEvent<UnifiedStreamEvent>> excelChat(ExcelChat excelChat) throws Exception {
        return this.dispatchStream(excelChat, UnifiedStreamEvent.class);
    }

    /**
     * 公共知识库问答（流式）。
     *
     * @param query 问答请求
     * @return 问答结果的 SSE 流
     * @throws Exception 策略执行过程中的异常
     */
    public Flux<ServerSentEvent<QueryRepeat>> query(Query query) throws Exception {
        return this.dispatchStream(query, QueryRepeat.class);
    }

    /**
     * 图片识别/对比（流式）。
     *
     * @param imageRecognition 图片识别请求
     * @return 图片识别结果的 SSE 流
     * @throws Exception 策略执行过程中的异常
     */
    public Flux<ServerSentEvent<UnifiedStreamEvent>> imgRecognition(ImageRecognition imageRecognition) throws Exception {
        return this.dispatchStream(imageRecognition, UnifiedStreamEvent.class);
    }

    /**
     * 文本总结（同步）。
     *
     * <p>内部以响应式方式执行业务逻辑，并在门面层同步阻塞返回，
     * 以保持对外同步方法签名的兼容性。</p>
     *
     * @param summary 总结请求
     * @return 总结结果
     * @throws Exception 策略执行过程中的异常
     */
    public SummaryRepeat summary(Summary summary) throws Exception {
        return this.dispatchSync(summary, SummaryRepeat.class).block();
    }
    /**
     * 文本翻译（流式）。
     *
     * @param translate 翻译请求
     * @return 翻译结果的 SSE 流
     * @throws Exception 策略执行过程中的异常
     */
    public Flux<ServerSentEvent<UnifiedStreamEvent>> translateStream(Translate translate) throws Exception {
        return this.dispatchStream(translate, UnifiedStreamEvent.class);
    }

    /**
     * 统一通用对话（流式）。
     *
     * @param unifiedChatStream 通用对话请求
     * @return 对话结果的 SSE 流
     * @throws Exception 策略执行过程中的异常
     */
    public Flux<ServerSentEvent<UnifiedStreamEvent>> unifiedChat(UnifiedChatStream unifiedChatStream) throws Exception {
        return this.dispatchStream(unifiedChatStream, UnifiedStreamEvent.class);
    }

    /**
     *  智能体设置（同步）。
     * @param agentSetting  智能体设置请求
     * @return  智能体设置结果
     * @throws Exception  策略执行过程中的异常
     */
    public AgentSetting  agentSetting(AgentSetting agentSetting) throws Exception {
        return this.dispatchSync(agentSetting, AgentSetting.class).block();
    }

    /**
     *  Excel翻译（同步）。
     * @param translate  Excel翻译请求
     * @return  Excel翻译结果
     * @throws Exception  策略执行过程中的异常
     */
    public Flux<ServerSentEvent<UnifiedChatRepeat>> excelTranslate(ExcelTranslate translate) throws Exception {
        return this.dispatchStream(translate, UnifiedChatRepeat.class);
    }

    public ResumeRepeat resume(Resume resume) throws Exception {
        return this.dispatchSync(resume, ResumeRepeat.class).block();
    }

}
