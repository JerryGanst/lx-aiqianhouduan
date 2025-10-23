package org.example.ai_api.Service.Apis.Applications;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ai_api.Bean.ApiRepeat.UnifiedChatRepeat;
import org.example.ai_api.Bean.ApiRequests.ExcelTranslateRequest;
import org.example.ai_api.Bean.WebRequest.ExcelTranslate;
import org.example.ai_api.Config.AIConfig;
import org.example.ai_api.Service.Apis.Commons.FileProcessor;
import org.example.ai_api.Service.Apis.Commons.StreamHub;
import org.example.ai_api.Service.Apis.Infrastructure.AIClient;
import org.example.ai_api.Service.Apis.Infrastructure.Factory.ExcelTranslateRequestFactory;
import org.example.ai_api.Utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

/**
 *  Excel翻译组件
 *  @author 10353965
 */
@Component
public class ExcelTranslateCase {

    @Autowired
    private AIClient aiClient;
    @Autowired
    private AIConfig aiConfig;
    @Autowired
    private StreamHub streamHub;
    @Autowired
    private ExcelTranslateRequestFactory excelTranslateRequestFactory;
    @Autowired
    private FileProcessor fileProcessor;
    @Value("${minio-proxy}")
    private String minioProxy;
    @Value("${local}")
    private String local;
    private static final Logger log = LoggerFactory.getLogger(ExcelTranslateCase.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Flux<ServerSentEvent<UnifiedChatRepeat>> excelTranslate(ExcelTranslate translate) throws Exception {
        ExcelTranslateRequest request = excelTranslateRequestFactory.processExcelTranslate(translate);
        request.setFileUrl(fileProcessor.getFileDownloadUrl(translate.getFile().getFileId()));
        request.setFileName(fileProcessor.getOriginalFileName(translate.getFile().getFileId()));
        String requestKey = streamHub.keyOf(translate.getUserId(), request.getSessionId());
        return aiClient.handleStreamRequest(request,aiConfig.getCategories().get("excelTranslate"), requestKey,UnifiedChatRepeat.class)
                .map(event -> {
                    UnifiedChatRepeat data;
                    if (event != null) {
                        data = event.data();
                    } else {
                        data = null;
                    }
                    if (data != null && "final".equalsIgnoreCase(data.getType())) {
                        String content = data.getContent();
                        if (StringUtils.hasText(content)) {
                            try {
                                String objectName = MAPPER.readTree(content)
                                        .at("/data/objectName")
                                        .asText(null);
                                String fileName = MAPPER.readTree(content)
                                        .at("/data/file_name")
                                        .asText(null);
                                if (StringUtils.hasText(objectName)) {
                                    String downloadUrl = fileProcessor.getDownloadUrlFormMinioByObjectName(objectName, fileName);
                                    data.setContent(Utils.exchangeFileUrl(downloadUrl, local, minioProxy));
                                }
                            } catch (Exception ex) {
                                log.warn("Parse final content failed, keep original content. content={}", content, ex);
                            }
                        }
                    }
                    // 复用原事件的元信息
                    assert (event != null ? event.event() : null) != null;
                    assert event.id() != null;
                    assert event.comment() != null;
                    assert event.retry() != null;
                    assert data != null;
                    return ServerSentEvent.builder(data)
                            .event(event.event())
                            .id(event.id())
                            .comment(event.comment())
                            .retry(event.retry())
                            .build();
                });
    }

}
