package org.example.ai_api.Service.Apis.Applications;

import org.example.ai_api.Bean.ApiRepeat.UnifiedStreamEvent;
import org.example.ai_api.Bean.ApiRepeat.UpstreamSseEvent;
import org.example.ai_api.Bean.ApiRequests.AIChatRequest;
import org.example.ai_api.Bean.Model.AgentConfig;
import org.example.ai_api.Bean.WebRequest.AgentChat;
import org.example.ai_api.Config.AIConfig;
import org.example.ai_api.Service.Apis.Commons.*;
import org.example.ai_api.Service.Apis.Infrastructure.AIClient;
import org.example.ai_api.Service.Apis.Infrastructure.Factory.AgentChatRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 *  智能体对话组件
 * @author 10353965
 */
@Component
public class AgentChatCase {

    private static final Logger logger = LoggerFactory.getLogger(AgentChatCase.class);

    @Autowired
    private AIClient aiClient;
    @Autowired
    private AIConfig aiConfig;
    @Autowired
    private StreamHub streamHub;
    @Autowired
    private ModelSelector modelSelector;
    @Autowired
    private AgentConfigCreator agentConfigCreator;
    @Autowired
    private AgentChatRequestFactory agentChatRequestFactory;
    @Autowired
    private FileProcessor fileProcessor;
    @Autowired
    private UserInfoGetter userInfoGetter;

    /**
     * 智能体对话.
     * @param agentChat 智能体对话请求体
     * @return 智能体对话结果(流式)
     */
    public Flux<ServerSentEvent<UnifiedStreamEvent>> agentChat(AgentChat agentChat) throws Exception {
        logger.info("agentChat");
        //获取基本请求体
        AIChatRequest agentChatRequest = agentChatRequestFactory.processAgentChat(agentChat);
        //文件处理
        fileProcessor.addFileContentToMessage(agentChatRequest.getHistory());
        fileProcessor.addFileContentToMessage(agentChatRequest.getMessages());
        if (agentChatRequest.getMessages() != null && !agentChatRequest.getMessages().isEmpty()) {
            fileProcessor.appendNewFilesToMessage(agentChat.getFileIds(), agentChatRequest.getMessages().get(0));
        }
        //检查模型合法性
        modelSelector.validateModelIndex(agentChat.getModel());
        //设定智能体配置
        AgentConfig agentConfig = agentConfigCreator.createAgentConfig(agentChat.getAgentId());
        agentChatRequest.setAgentConfig(agentConfig);
        agentChatRequest.setUserDepartment(userInfoGetter.getDepartmentId(agentChat.getUserId()));
        //流式请求，获取requestKey
        String requestKey = streamHub.keyOf(agentChat.getUserId(),agentChat.getSessionId());
        //流式请求占位
        streamHub.placeHolder(requestKey);
        //发送请求
        return aiClient.handleStreamRequest(agentChatRequest, aiConfig.getCategories().get("aiChat"),  requestKey, UpstreamSseEvent.class)
                .map(e -> ServerSentEvent.builder(SseStreamTransformer.transform(e.data())).build());
    }

}
