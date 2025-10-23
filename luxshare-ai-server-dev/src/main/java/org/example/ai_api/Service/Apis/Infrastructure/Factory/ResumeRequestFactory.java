package org.example.ai_api.Service.Apis.Infrastructure.Factory;

import org.example.ai_api.Bean.ApiRequests.ResumeRequest;
import org.example.ai_api.Bean.WebRequest.Resume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ResumeRequestFactory {

    private static final Logger logger = LoggerFactory.getLogger(ResumeRequestFactory.class);
    @Value("${resumeCallback}")
    private String resumeCallback;

    public ResumeRequest processResume(Resume resume) {
        logger.info("processResume {}", resume.getUserId());
        ResumeRequest request = new ResumeRequest();
        request.setCallbackUrl(resumeCallback);
        return request;
    }
}
