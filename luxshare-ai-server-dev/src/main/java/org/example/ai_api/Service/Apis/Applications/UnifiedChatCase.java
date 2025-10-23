package org.example.ai_api.Service.Apis.Applications;

import org.apache.commons.lang3.StringUtils;
import org.example.ai_api.Bean.ApiRepeat.UnifiedChatRepeat;
import org.example.ai_api.Bean.ApiRepeat.UnifiedStreamEvent;
import org.example.ai_api.Bean.ApiRepeat.UpstreamSseEvent;
import org.example.ai_api.Bean.ApiRequests.AIChatRequest;
import org.example.ai_api.Bean.Entity.UserInfo;
import org.example.ai_api.Bean.Enum.Action;
import org.example.ai_api.Bean.Model.AgentConfig;
import org.example.ai_api.Bean.WebRequest.UnifiedChatStream;
import org.example.ai_api.Config.AIConfig;
import org.example.ai_api.Service.Apis.Commons.*;
import org.example.ai_api.Service.Apis.Infrastructure.AIClient;
import org.example.ai_api.Service.Apis.Infrastructure.Factory.UnifiedChatRequestFactory;
import org.example.ai_api.Service.UserInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Objects;

/**
 * 统一的通用对话组件
 * @author 10353965
 */
@Component
public class UnifiedChatCase {
    private static final Logger logger = LoggerFactory.getLogger(UnifiedChatCase.class);

    @Autowired
    private AIClient aiClient;
    @Autowired
    private AIConfig aiConfig;
    @Autowired
    private UnifiedChatRequestFactory unifiedChatRequestFactory;
    @Autowired
    private FileProcessor fileProcessor;
    @Autowired
    private KnowledgeEnricher  knowledgeEnricher;
    @Autowired
    private AgentConfigCreator  agentConfigCreator;
    @Autowired
    private StreamHub streamHub;
    @Autowired
    private UserInfoGetter userInfoGetter;

    public Flux<ServerSentEvent<UnifiedStreamEvent>> unifiedChat(UnifiedChatStream unifiedChatStream) throws Exception{
        logger.info("统一对话接口");
        AIChatRequest request = unifiedChatRequestFactory.processUnifiedChat(unifiedChatStream);
        //历史对话文件处理
        fileProcessor.addFileContentToMessage(request.getHistory());
        //新对话文件处理
        fileProcessor.addFileContentToMessage(request.getMessages());
        //todo:知识库处理
//        switch (unifiedChatStream.getChatType()){
//            case ALL_FILES:request.setAction(Action.RagPersonalAgent);break;
//            case PARTIAL_FILES:request.setAction(Action.RagPersonalAgent);break;
//            case SINGLE_FILE:request.setAction(Action.RagPersonalAgent);break;
//            case ALL_FILES:request.setAction(Action.RagPersonalAgent);break;
//        }
        //agent处理
        String agentId = unifiedChatStream.getAgentId();
        request.setUserDepartment(userInfoGetter.getDepartmentId(unifiedChatStream.getUserId()));
        if(agentId != null){
            AgentConfig agentConfig = agentConfigCreator.createAgentConfig(agentId);
            request.setAgentConfig(agentConfig);
        }
        String requestKey = streamHub.keyOf(unifiedChatStream.getUserId(), unifiedChatStream.getSessionId());
        streamHub.placeHolder(requestKey);
        return aiClient.handleStreamRequest(request, aiConfig.getCategories().get("aiChat"), requestKey, UpstreamSseEvent.class)
                .map(e -> ServerSentEvent.builder(SseStreamTransformer.transform(e.data())).build());

    }
}
