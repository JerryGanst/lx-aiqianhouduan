package org.example.ai_api.Service.Apis.Applications;

import org.example.ai_api.Bean.ApiRepeat.SummaryRepeat;
import org.example.ai_api.Bean.ApiRequests.SummarizeRequest;
import org.example.ai_api.Bean.Model.FileId;
import org.example.ai_api.Bean.WebRequest.Summary;
import org.example.ai_api.Config.AIConfig;
import org.example.ai_api.Service.Apis.Commons.FileProcessor;
import org.example.ai_api.Service.Apis.Infrastructure.AIClient;
import org.example.ai_api.Service.Apis.Infrastructure.Factory.SummaryRequestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 总结组件
 * @author 10353965
 */
@Component
public class SummaryCase {

    @Autowired
    private AIClient aiClient;
    @Autowired
    private AIConfig aiConfig;
    @Autowired
    private SummaryRequestFactory summaryRequestFactory;
    @Autowired
    private FileProcessor fileProcessor;

    /**
     * AI总结.
     *
     * @param summary 请求体
     * @return 总结结果
     */
    public SummaryRepeat summary(Summary summary) throws Exception {
        //构造基本的ai侧请求体
        SummarizeRequest request = summaryRequestFactory.processSummary(summary);
        //处理文件文本内容
        FileId fileId = summary.getFile();
        String fileContent = fileProcessor.getFileContentOrDefault(fileId,summary.getQuestion());
        request.setQuestion(fileContent);
        //调用AI接口
        return aiClient.handleSyncRequest(request, aiConfig.getCategories().get("summary"), SummaryRepeat.class);
    }

}
