package org.example.ai_api.Service.Apis.Applications;

import org.example.ai_api.Bean.ApiRepeat.ResumeRepeat;
import org.example.ai_api.Bean.ApiRequests.ResumeRequest;
import org.example.ai_api.Bean.WebRequest.Resume;
import org.example.ai_api.Config.AIConfig;
import org.example.ai_api.Service.Apis.Commons.FileProcessor;
import org.example.ai_api.Service.Apis.Infrastructure.AIClient;
import org.example.ai_api.Service.Apis.Infrastructure.Factory.ResumeRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *  简历分析组件
 *  @author 10353965
 */
@Component
public class ResumeCase {

    private static final Logger logger = LoggerFactory.getLogger(ResumeCase.class);

    @Autowired
    private ResumeRequestFactory resumeRequestFactory;
    @Autowired
    private FileProcessor fileProcessor;
    @Autowired
    private AIConfig aiConfig;
    @Autowired
    private AIClient aiClient;

    public ResumeRepeat resume(Resume resume) throws Exception {
        ResumeRequest request = resumeRequestFactory.processResume(resume);
        String jdBasic;
        if(resume.getJdText() == null ||  resume.getJdText().isEmpty()){
            logger.info("文本jd为空，开始检查jd文件");
            jdBasic = fileProcessor.getJDFileContent(resume.getJdFile());
        }else {
            jdBasic = resume.getJdText();
        }
        request.setJdBasic(jdBasic);
        fileProcessor.buildResumesForRequest(request,resume.getResumeFileIds());
        return aiClient.handleSyncRequest(request, aiConfig.getCategories().get("resume"), ResumeRepeat.class);
    }

}
