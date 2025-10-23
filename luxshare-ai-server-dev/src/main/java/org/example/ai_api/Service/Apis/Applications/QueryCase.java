package org.example.ai_api.Service.Apis.Applications;

import org.example.ai_api.Bean.ApiRepeat.QueryRepeat;
import org.example.ai_api.Bean.ApiRequests.QueryRequest;
import org.example.ai_api.Bean.WebRequest.Query;
import org.example.ai_api.Service.Apis.Commons.FileProcessor;
import org.example.ai_api.Service.Apis.Commons.ModelSelector;
import org.example.ai_api.Service.Apis.Commons.StreamHub;
import org.example.ai_api.Service.Apis.Infrastructure.AIClient;
import org.example.ai_api.Service.Apis.Infrastructure.Factory.QueryRequestFactory;
import org.example.ai_api.Strategy.KnowledgeBase.KnowledgeBaseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 公共知识库问答组件.
 * @author 10353965
 */
@Component
public class QueryCase {

    private static final Logger logger = LoggerFactory.getLogger(QueryCase.class);

    @Autowired
    private KnowledgeBaseContext  knowledgeBaseContext;
    @Autowired
    private AIClient aiClient;
    @Autowired
    private QueryRequestFactory queryRequestFactory;
    @Autowired
    private ModelSelector  modelSelector;
    @Autowired
    private FileProcessor fileProcessor;
    @Autowired
    private StreamHub streamHub;

    /**
     * 知识库问答.
     *
     * @param query 请求结构体
     * @return 问答结果(流式)
     */
    public Flux<ServerSentEvent<QueryRepeat>> query(Query query) {
        //提取前端请求体参数
        int model = query.getModel();
        String type = query.getType();
        String userId = query.getUserId();
        //构造基本的ai侧请求体
        QueryRequest queryRequest = queryRequestFactory.processQuery(query);
        //判断模型选择是否合法
        modelSelector.validateModelIndex(model);
        //设定ai侧请求体的模型
        queryRequest.setModel(modelSelector.getQueryModel(model, type));
        //根据策略模式分配url
        String url = knowledgeBaseContext.getUrl(query.getType());
        logger.info("queryRequest:{}", queryRequest);
        logger.info("url:{}", url);
        //通过工具类获取请求id，并进行占位
        String requestKey = streamHub.keyOf(userId,null);
        streamHub.placeHolder(requestKey);
        //调用AI接口发送请求
        return aiClient.handleStreamRequest(queryRequest, url, requestKey, QueryRepeat.class)
                .filter(qa -> {
                    assert qa.data() != null;
                    return !qa.data().getContent().contains("Result");
                })
                .map(event -> {
                    QueryRepeat data = event.data();
                    assert data != null;
                    // 只有当 type 为 "final_answer" 时才更新 fileUrl
                    if ("final_answer".equals(data.getType())) {
                        fileProcessor.addFileUrlToSource(data,query.getType());
                    }
                    return event;
                });
    }
}
