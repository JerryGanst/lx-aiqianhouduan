package org.example.ai_api.Service.Apis.Applications;

import org.example.ai_api.Bean.ApiRepeat.UnifiedStreamEvent;
import org.example.ai_api.Bean.ApiRepeat.UpstreamSseEvent;
import org.example.ai_api.Bean.ApiRequests.ImageRecognitionRequest;
import org.example.ai_api.Bean.Model.AIChatMessage;
import org.example.ai_api.Bean.WebRequest.ImageRecognition;
import org.example.ai_api.Config.AIConfig;
import org.example.ai_api.Service.Apis.Commons.ImgMessageCreator;
import org.example.ai_api.Service.Apis.Commons.StreamHub;
import org.example.ai_api.Service.Apis.Commons.UserInfoGetter;
import org.example.ai_api.Service.Apis.Infrastructure.AIClient;
import org.example.ai_api.Service.Apis.Infrastructure.Factory.RecognitionRequestFactory;
import org.example.ai_api.Service.Apis.Commons.SseStreamTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * 图像识别组件
 * @author 10353965
 */
@Component
public class RecognitionCase {

    private static final Logger logger = LoggerFactory.getLogger(RecognitionCase.class);

    @Autowired
    private AIClient aiClient;
    @Autowired
    private AIConfig aiConfig;
    @Autowired
    private StreamHub streamHub;
    @Autowired
    private ImgMessageCreator imgMessageCreator;
    @Autowired
    private RecognitionRequestFactory imageRecognitionRequestFactory;
    @Autowired
    private UserInfoGetter userInfoGetter;

    /**
     * 图片对比
     * @param  imageRecognition 图片对比请求
     * @return 图片对比结果(流式)
     */
    public Flux<ServerSentEvent<UnifiedStreamEvent>> imgRecognition(ImageRecognition imageRecognition) throws Exception {
        logger.info("imgRecognition {}",imageRecognition.getUserId());
        //构造基本的ai侧请求体
        ImageRecognitionRequest imageRecognitionRequest = imageRecognitionRequestFactory.processImageRecognition(imageRecognition);
        //封装message结构
        AIChatMessage userMessage = imgMessageCreator.createImgMessage(imageRecognition);
        //将新封装的message结构加入request
        List<AIChatMessage> messages = imageRecognitionRequest.getMessages();
        if(messages == null){
            messages = new ArrayList<>();
        }
        messages.add(userMessage);
        imageRecognitionRequest.setMessages(messages);
        // v1/chat统一入口
        String url = aiConfig.getCategories().get("aiChat");
        if (!StringUtils.hasText(url)) {
            throw new IllegalStateException("aiChat url is not configured");
        }
        imageRecognitionRequest.setUserDepartment(userInfoGetter.getDepartmentId(imageRecognition.getUserId()));
        //获取流式请求的requestKey
        String requestKey = streamHub.keyOf(imageRecognition.getUserId(),imageRecognition.getSessionId());
        //流式请求占位
        streamHub.placeHolder(requestKey);
        //调用AI接口发送请求
        return aiClient.handleStreamRequest(imageRecognitionRequest, url, requestKey, UpstreamSseEvent.class)
                .map(e -> {
                    logger.info("{}",e);
                    return ServerSentEvent.builder(SseStreamTransformer.transform(e.data())).build();
                });
    }

}
