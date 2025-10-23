package org.example.ai_api.Service.Apis.Infrastructure.Factory;

import org.example.ai_api.Bean.ApiRequests.ExcelTranslateRequest;
import org.example.ai_api.Bean.WebRequest.ExcelTranslate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ExcelTranslateRequestFactory {

    private static final Logger logger = LoggerFactory.getLogger(ExcelTranslateRequestFactory.class);
    @Value("${uploadExcelTranslate}")
    private String uploadExcelTranslate;

    public ExcelTranslateRequest processExcelTranslate(ExcelTranslate translate){
        logger.info("excel翻译");
        ExcelTranslateRequest request = new ExcelTranslateRequest();
        request.setSessionId(translate.getSessionId());
        request.setUserId(translate.getUserId());
        request.setTargetLanguage(translate.getTarget_language());
        request.setUploadApiUrl(uploadExcelTranslate);
        return request;
    }
}

