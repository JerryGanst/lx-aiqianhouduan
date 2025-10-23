package org.example.ai_api.Controller;

import org.example.ai_api.Bean.ApiRepeat.ResumeTaskRepeat;
import org.example.ai_api.Bean.ApiRequests.ResumeCallBackRequest;
import org.example.ai_api.Bean.Entity.ResumeTask;
import org.example.ai_api.Bean.Model.ResultData;
import org.example.ai_api.Bean.WebRequest.ResumeCallback;
import org.example.ai_api.Config.AIConfig;
import org.example.ai_api.Exception.BadRequestException;
import org.example.ai_api.Service.Apis.Infrastructure.AIClient;
import org.example.ai_api.Service.ResumeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/Resume")
public class ResumeController {

    private static final Logger logger = LoggerFactory.getLogger(ResumeController.class);

    @Autowired
    private AIClient aiClient;
    @Autowired
    private AIConfig aiConfig;
    @Autowired
    private ResumeService resumeService;

    //简历任务前端回调接口
    @PostMapping("/resumeTaskCallback")
    public ResultData<ResumeTaskRepeat> ResumeTaskCallback(@RequestBody ResumeCallBackRequest request) {
        logger.info("ResumeTaskCallback:{}", request);
        if(request.getTaskId() == null || request.getTaskId().isEmpty()){
            return ResultData.fail("任务id不能为空");
        }
        ResumeTaskRepeat result = aiClient.handleSyncRequest(request, aiConfig.getCategories().get("resumeCallback"), ResumeTaskRepeat.class);
        return ResultData.success("操作成功", result);
    }

    //简历任务AI回调接口
    @PostMapping("/resumeTaskComplete")
    public ResultData<String>  ResumeTaskComplete(@RequestBody ResumeCallback request) {
        logger.info("ResumeTaskComplete:{}", request);
        //构造回调请求，获取结果
        ResumeCallBackRequest callBackRequest = new ResumeCallBackRequest();
        callBackRequest.setTaskId(request.getBatchId());
        callBackRequest.setTaskType("batch_match_task");
        ResumeTaskRepeat result = aiClient.handleSyncRequest(callBackRequest, aiConfig.getCategories().get("resumeCallback"), ResumeTaskRepeat.class);
        //更新结果
        ResumeTask resumeTask =  resumeService.updateResumeTask(result);
        if(resumeTask == null){
            return ResultData.fail("任务不存在");
        }
        return ResultData.success("操作成功", "success");
    }

    /**
     *  保存简历任务
     * @param resumeTask  简历任务
     * @return   保存结果
     */
    @PostMapping("/saveResumeTask")
    public ResultData<ResumeTask> saveResumeTask(@RequestBody ResumeTask resumeTask) {
        if(resumeTask == null){
            throw new BadRequestException("任务不能为空");
        }
        ResumeTask result = resumeService.saveResumeTask(resumeTask);
        return ResultData.success("操作成功", result);
    }

    /**
     * 根据id获取简历任务
     * @param id  任务id
     * @return  任务结果
     */
    @PostMapping("/getResumeTaskById")
    public ResultData<ResumeTask> getResumeTaskById(String id) {
        if(id == null || id.isEmpty()){
            throw new BadRequestException("任务id不能为空");
        }
        ResumeTask resumeTask = resumeService.getResumeTaskById(id);
        if(resumeTask ==  null){
            throw new BadRequestException("任务不存在");
        }
        return ResultData.success("操作成功", resumeTask);
    }

    /**
     *  根据用户id获取简历任务
     * @param userId  用户id
     * @return  任务结果
     */
    @PostMapping("/getResumeTaskByUserId")
    public ResultData<List<ResumeTask>> getResumeTaskByUserId(
            @RequestParam("userId") String userId,
            @RequestParam(value = "keyword",defaultValue = "") String keyword
    ) {
        if(userId == null || userId.isEmpty()){
            throw new BadRequestException("用户id不能为空");
        }
        List<ResumeTask> resumeTask = resumeService.getResumeTaskByUserId(userId,keyword);
        return ResultData.success("操作成功", resumeTask);
    }

    /**
     *  修改简历任务标题
     * @param id  任务id
     * @param title  任务标题
     * @return  操作结果
     */
    @PostMapping("/changeResumeTaskTitleById")
    public ResultData<String> changeResumeTaskTitleById(String id, String title) {
        if(id == null || id.isEmpty()){
            throw new BadRequestException("任务id不能为空");
        }
        if(title == null || title.isEmpty()){
            throw new BadRequestException("任务标题不能为空");
        }
        resumeService.changeResumeTaskTitleById(id, title);
        return ResultData.success("操作成功", "success");
    }

    @PostMapping("/deleteResumeTaskById")
    public ResultData<String> deleteResumeTaskById(String id) {
        if(id == null || id.isEmpty()){
            throw new BadRequestException("任务id不能为空");
        }
        resumeService.deleteResumeTaskById(id);
        return ResultData.success("操作成功", "success");
    }

}
