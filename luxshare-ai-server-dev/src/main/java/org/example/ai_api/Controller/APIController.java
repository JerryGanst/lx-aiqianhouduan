package org.example.ai_api.Controller;

import org.bson.types.ObjectId;
import org.example.ai_api.Annotation.FunctionCount;
import org.example.ai_api.Annotation.RateLimiter;
import org.example.ai_api.Bean.ApiRequests.FileSynRequest;
import org.example.ai_api.Bean.Entity.FileUpload;
import org.example.ai_api.Bean.Entity.ResumeTask;
import org.example.ai_api.Bean.Model.ResultData;
import org.example.ai_api.Bean.WebRequest.*;
import org.example.ai_api.Bean.ApiRepeat.*;
import org.example.ai_api.Exception.BadRequestException;
import org.example.ai_api.Service.*;
import org.example.ai_api.Service.AIPlatformSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ai_api.Service.Apis.Commons.StreamHub;
import org.example.ai_api.Service.Apis.AiFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

/**
 * AI模型功能接口.
 *
 * @author 10353965
 */
@RestController
@CrossOrigin(maxAge = 3600)
@RequestMapping("/AI")
public class APIController {
    private static final Logger logger = LoggerFactory.getLogger(APIController.class.getName());
    @Autowired
    private FileUploadInfoService fileUploadInfoService;
    @Autowired
    private AiFacade aiFacade;
    @Autowired
    private StreamHub streamHub;
    @Autowired
    private MultipartUploadService multipartUploadService;
    @Autowired
    private ResumeService resumeService;
    @Autowired
    private AIPlatformSyncService aiPlatformSyncService;

    /**
     * 知识库问答
     *
     * @param query 问答请求体
     * @return AI模型结果(流式返回)
     */
    @PostMapping(value = "/query")
    @ResponseBody
    @RateLimiter()
    @FunctionCount("知识库问答")
    public Flux<ServerSentEvent<QueryRepeat>> query(@RequestBody Query query) throws Exception {
        logger.info("query:{}", query.toString());
        return aiFacade.query(query);
    }

    /**
     * AI总结.
     *
     * @param summary 总结请求体
     * @return 模型总结后的结果
     */
    @PostMapping(value = "/summarize")
    @ResponseBody
    @RateLimiter()
    @FunctionCount("总结")
    public ResultData<SummaryRepeat> summarize(@RequestBody Summary summary) throws Exception {
        logger.info("summarize:{}", summary.getUserId());
        return ResultData.success("总结完成", aiFacade.summary(summary));
    }

    /**
     * AI翻译.(流式）
     *
     * @param translate 翻译请求体
     * @return 模型翻译后的结果
     */
    @PostMapping(value = "/translateStream")
    @ResponseBody
    @RateLimiter()
    @FunctionCount("翻译")
    public Flux<ServerSentEvent<UnifiedStreamEvent>> translateStream(@RequestBody Translate translate) throws Exception {
        logger.info("translate:{} {}", translate.getUserId(),translate.getFile().getFileId());
        return aiFacade.translateStream(translate);
    }

    /**
     * excel翻译
     * @param translate  翻译请求体
     * @return  翻译后的文件下载链接
     * @throws Exception  异常
     */
    @PostMapping("/excelTranslate")
    @ResponseBody
    @RateLimiter()
    @FunctionCount("翻译")
    public Flux<ServerSentEvent<UnifiedChatRepeat>> excelTranslate(@RequestBody ExcelTranslate translate) throws Exception {
        logger.info("excelTranslate");
        return aiFacade.excelTranslate(translate);
    }

    /**
     * 智能体对话
     *
     * @param agentChat 智能体对话请求体
     * @return 智能体对话结果(流式)
     * @throws Exception 异常
     */
    @PostMapping("/agentChat")
    @RateLimiter()
    @FunctionCount("智能体对话")
    public Flux<ServerSentEvent<UnifiedStreamEvent>> agentChat(@RequestBody AgentChat agentChat) throws Exception {
        logger.info("agentChat");
        // 校验 sessionId
        if (agentChat.getSessionId() == null || agentChat.getSessionId().isEmpty()) {
            throw new BadRequestException("SessionId cannot be null or empty.");
        }
        return  aiFacade.agentChat(agentChat);
    }

    /**
     * 停止流式返回.
     *
     * @param userId 用户id
     * @return 是否成功停止
     */
    @PostMapping("/stop")
    @RateLimiter()
    public ResultData<String> stopRequest(
            @RequestParam("userId") String userId,
            @RequestParam(value = "sessionId", required = false) String sessionId
    ){
        logger.info("stop userId:{} sessionId:{}", userId, sessionId);
        String key = streamHub.keyOf(userId, sessionId);
        boolean cancelled = streamHub.cancel(key);
        String msg = cancelled ? "请求已停止" : "请求已停止（尚未建立连接或已取消）";
        return ResultData.success(msg);
    }

    /**
     * 对话过程上传文件.(临时文件夹)
     *
     * @param files 文件本体
     * @return 上传结果，包含上传后的文件信息数组
     * @throws IOException the io exception
     */
    @PostMapping("/fileUpload")
    @FunctionCount("对话过程上传文件")
    public ResultData<List<FileUpload>> uploadFile(@RequestPart("files") List<MultipartFile> files,@RequestParam("local") boolean local) throws Exception {
        logger.info("uploadFile");
        List<FileUpload> fileUploads = new ArrayList<>();
        for (MultipartFile file : files) {
            FileUpload fileUpload = fileUploadInfoService.processFile(file,local);
            fileUploadInfoService.getFileUrlByFileId(fileUpload);
            fileUploads.add(fileUpload);
        }
        List<FileUpload> result = fileUploadInfoService.saveAll(fileUploads);
        return ResultData.success("上传成功", result);
    }

    /**
     * 获得某次AI请求的唯一id
     * @return 唯一id
     */
    @GetMapping("/getRequestId")
    public ResultData<String> newChatId() {
        ObjectId objectId = new ObjectId();
        return ResultData.success("获取成功", objectId.toString());
    }

    @PostMapping("/healthCheck")
    public ResultData<String> healthCheck() {
        return ResultData.success("ok");
    }

    /**
     * 通用对话(流式接口，返回结构与知识库问答统一)
     * @param request 通用对话请求体
     * @return 通用对话结果(流式)
     * @throws Exception 异常
     */
    @FunctionCount("通用对话")
    @PostMapping("/unifiedChat")
    public Flux<ServerSentEvent<UnifiedStreamEvent>> unifiedChat(@RequestBody UnifiedChatStream request) throws Exception {
        logger.info("unifiedChat");
        return aiFacade.unifiedChat(request);
    }

    /**
     * 图片对比接口
     * @param imageRecognition 图片对比请求体
     * @return 图片对比结果(流式)
     * @throws Exception 异常
     */
    @PostMapping("/imageRecognition")
    public Flux<ServerSentEvent<UnifiedStreamEvent>> imageRecognition(@RequestBody ImageRecognition imageRecognition) throws Exception {
        logger.info("imageRecognition");
        // 校验 sessionId
        if (imageRecognition.getSessionId() == null || imageRecognition.getSessionId().isEmpty()) {
            throw new BadRequestException("SessionId cannot be null or empty.");
        }
        return aiFacade.imgRecognition(imageRecognition);
    }

    /**
     * 流式 Excel 数据分析聊天接口
     * @param excelChat excel对话请求体
     * @return  Excel对话结果(流式)
     */
    @PostMapping("/excelChat")
    public Flux<ServerSentEvent<UnifiedStreamEvent>> excelAnalysis(@RequestBody ExcelChat excelChat) throws Exception {
        logger.info("excelAnalysis");
        // 校验 sessionId
        if (excelChat.getSessionId() == null || excelChat.getSessionId().isEmpty()) {
            throw new BadRequestException("SessionId cannot be null or empty.");
        }
        logger.info("Received excelChat request with sessionId: {}", excelChat.getSessionId());
        return aiFacade.excelChat(excelChat);
    }

    /**
     * 简历分析接口
     * @param resume  简历分析请求体
     * @return  简历分析结果
     * @throws Exception  异常
     */
    @PostMapping("/resumes")
    public ResultData<ResumeTask>  resume(@RequestBody Resume resume) throws Exception {
        logger.info("resume");
        ResumeRepeat result = aiFacade.resume(resume);
        ResumeTask resumeTask = resumeService.saveResumeTask(resume,result);
        return ResultData.success("操作成功", resumeTask);
    }

    /**
     * 一步准备：初始化并返回所有分片上传URL
     */
    @PostMapping("/upload/multipart/prepare")
    public ResultData<MultipartPrepareResponse> multipartPrepare(@RequestBody MultipartPrepareRequest request) throws Exception {
        logger.info("multipartPrepare filename={}, size={}", request.getFilename(), request.getSize());
        return ResultData.success("准备成功", multipartUploadService.prepare(request));
    }

    /**
     * 完成分片上传，合并对象并入库文件元数据
     */
    @PostMapping("/upload/multipart/complete")
    public ResultData<FileUpload> multipartComplete(@RequestBody MultipartCompleteRequest request) throws Exception {
        logger.info("multipartComplete (finalize metadata) objectKey={}", request.getObjectKey());
        FileUpload saved = multipartUploadService.finalizeComplete(request);
        return ResultData.success("上传成功", saved);
    }

    /**
     * 取消分片上传
     */
    @PostMapping("/upload/multipart/abort")
    public ResultData<String> multipartAbort(@RequestBody MultipartAbortRequest request) {
        logger.info("multipartAbort objectKey={}", request.getObjectKey());
        String msg = multipartUploadService.abort(request);
        return ResultData.success(msg);
    }
}
