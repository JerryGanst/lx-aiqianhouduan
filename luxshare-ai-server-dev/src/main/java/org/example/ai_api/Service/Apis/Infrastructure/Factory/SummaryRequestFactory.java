package org.example.ai_api.Service.Apis.Infrastructure.Factory;

import org.example.ai_api.Bean.ApiRequests.SummarizeRequest;
import org.example.ai_api.Bean.WebRequest.Summary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 总结请求体构造
 * @author 10353965
 */
@Component
public class SummaryRequestFactory {

    private static final Logger logger = LoggerFactory.getLogger(SummaryRequestFactory.class);

    /**
     * 对前端的总结请求进行预处理.
     *
     * @param summary 前端总结请求
     * @return 预处理后的总结请求
     */
    public SummarizeRequest processSummary(Summary summary) {
        logger.info("构造基本总结请求体");
        SummarizeRequest summarizeRequest = new SummarizeRequest();
        summarizeRequest.setUser_id(summary.getUserId());
        return summarizeRequest;
    }
}
