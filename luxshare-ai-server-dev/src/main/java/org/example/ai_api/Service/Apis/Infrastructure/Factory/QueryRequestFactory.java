package org.example.ai_api.Service.Apis.Infrastructure.Factory;

import org.example.ai_api.Bean.ApiRequests.QueryRequest;
import org.example.ai_api.Bean.WebRequest.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 知识库问答请求体构造
 * @author 10353965
 */
@Component
public class QueryRequestFactory {

    private static final Logger logger = LoggerFactory.getLogger(QueryRequestFactory.class);

    public QueryRequest processQuery(Query query){
        logger.info("构造基本企业知识库问答请求体");
        QueryRequest queryRequest = new QueryRequest();
        queryRequest.setQuestion(query.getQuestion());
        queryRequest.setUserId(query.getUserId());
        return queryRequest;
    }
}
