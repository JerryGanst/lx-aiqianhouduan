package org.example.ai_api.Controller;

import org.example.ai_api.Bean.Entity.Agent;
import org.example.ai_api.Bean.Entity.AgentChatInfo;
import org.example.ai_api.Bean.Model.AgentConfig;
import org.example.ai_api.Bean.Model.FileId;
import org.example.ai_api.Bean.Model.ResultData;
import org.example.ai_api.Bean.WebRequest.AgentSetting;
import org.example.ai_api.Bean.WebRequest.FeedBack;
import org.example.ai_api.Exception.BadRequestException;
import org.example.ai_api.Service.AgentService;
import org.example.ai_api.Service.ApiService;
import org.example.ai_api.Service.Apis.AiFacade;
import org.example.ai_api.Service.FileService;
import org.example.ai_api.Service.FileUploadInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.List;

/**
 * 智能体相关接口
 *
 * @author 10353965
 */
@RestController
@RequestMapping("/Agent")
public class AgentController {
    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);
    @Autowired
    private AgentService agentService;
    @Autowired
    private ApiService apiService;
    @Autowired
    private FileService fileService;
    @Autowired
    private FileUploadInfoService fileUploadInfoService;
    @Autowired
    private AiFacade aiFacade;

    /**
     * 上传智能体头像
     *
     * @param file 上传的文件
     * @return 上传结果
     * @throws Exception 异常
     */
    @PostMapping("/uploadPic")
    public ResultData<String> uploadPic(@RequestParam("file") MultipartFile file) throws Exception {
        logger.info("上传智能体头像");
        String fileName = agentService.uploadPic(file);
        return ResultData.success("上传成功", fileName);
    }

    /**
     * 根据图片文件的对象名获得预览链接
     *
     * @param objectName 文件名
     * @return 文件链接
     */
    @Deprecated
    @PostMapping("/getPicUrl")
    public ResultData<String> getPicUrl(@RequestParam("objectName") String objectName) {
        logger.info("根据图片文件的对象名获得预览链接");
        return ResultData.success("操作成功", agentService.getPicUrl(objectName));
    }

    /**
     * 保存或修改智能体
     *
     * @param agent 智能体
     * @return 保存后的智能体
     */
    @PostMapping("/saveAgent")
    public ResultData<Agent> saveAgent(@RequestBody Agent agent) {
        logger.info("保存或修改智能体");
        return ResultData.success("保存成功", agentService.saveAgent(agent));
    }

    /**
     * 根据智能体id删除智能体
     *
     * @param agentId 智能体id
     * @return 删除结果
     */
    @PostMapping("/deleteAgentById")
    public ResultData<Agent> deleteAgent(@RequestParam("agentId") String agentId) throws Exception {
        logger.info("根据智能体id删除智能体");
        agentService.deleteAgentById(agentId);
        return ResultData.success("删除成功");
    }

    /**
     * 根据智能体id查询智能体
     *
     * @param agentId 智能体id
     * @return 智能体信息
     */
    @PostMapping("/findAgentById")
    public ResultData<Map> findAgentById(@RequestParam("agentId") String agentId) {
        logger.info("根据智能体id查询智能体");
        Map result = agentService.findAgentByIdWithFields(agentId);
        return ResultData.success("查询成功", result);
    }

    /**
     * 根据用户id查询智能体(提供部分必要字段)
     *
     * @param userId 用户id
     * @param keyword 关键词(非必须)
     * @return 智能体信息
     */
    @PostMapping("/findAgentByUserId")
    public ResultData<List<Map>> findAgentByUserId(
            @RequestParam("userId") String userId,
            @RequestParam(value = "keyword",defaultValue = "") String keyword
    ) {
        logger.info("根据用户id查询智能体");
        List<Map> result = agentService.findAgentByUserId(userId,keyword);
        return ResultData.success("查询成功", result);
    }

    /**
     * 根据用户id和智能体id获得聊天记录
     *
     * @param agentId 智能体id
     * @param userId  用户id
     * @return 聊天记录
     */
    @PostMapping("/findAgentChat")
    public ResultData<List<AgentChatInfo>> findAgentChat(@RequestParam("agentId") String agentId, @RequestParam("userId") String userId) {
        logger.info("根据用户id和智能体id获得聊天记录");
        return ResultData.success("查询成功", agentService.findAgentChatByAgentIdAndUserId(agentId, userId));
    }

    /**
     * 根据id删除聊天记录
     *
     * @param agentChatId 聊天记录id
     * @param userId      用户id
     * @return 删除结果
     */
    @PostMapping("/deleteAgentChatById")
    public ResultData<String> deleteAgentChatById(@RequestParam("agentChatId") String agentChatId, @RequestParam("userId") String userId) {
        logger.info("根据id删除聊天记录{}", agentChatId);
        agentService.deleteAgentChatById(agentChatId, userId);
        return ResultData.success("删除成功");
    }

    /**
     * 根据id批量删除聊天记录
     *
     * @param agentChatIds 聊天记录id
     * @return 删除结果
     */
    @PostMapping("/deleteAgentChatByIds")
    public ResultData<String> deleteAgentChatByIds(@RequestBody List<String> agentChatIds) {
        logger.info("根据id批量删除聊天记录");
        agentService.deleteAgentChatByIds(agentChatIds);
        return ResultData.success("删除成功");
    }

    /**
     * 保存聊天记录
     *
     * @param agentChatInfo 聊天记录
     * @return 保存结果
     */
    @PostMapping("/saveAgentChat")
    public ResultData<AgentChatInfo> saveAgentChat(@RequestBody AgentChatInfo agentChatInfo) {
        logger.info("保存聊天记录");
        return ResultData.success("保存成功", agentService.saveAgentChat(agentChatInfo));
    }

    /**
     * 智能体设定生成
     * @param agentSetting 已有智能体设定
     * @return 智能体设定
     */
    @PostMapping("/generateAgentDescription")
    public ResultData<AgentSetting> generateAgentSetting(@RequestBody AgentSetting agentSetting) throws Exception {
        logger.info("智能体设定生成");
//        AgentConfig agentConfig = new AgentConfig(agentSetting);
//        AgentConfig result = apiService.generateAgentSetting(agentConfig);
//        AgentSetting resultSetting = new AgentSetting(result);
//        return ResultData.success("生成成功", resultSetting);
        AgentSetting resultSetting = aiFacade.agentSetting(agentSetting);
        return ResultData.success("生成成功", resultSetting);
    }

    /**
     * 智能体对话评价
     * @param feedBack 评价
     * @return 评价结果
     */
    @PostMapping("/feedback")
    @ResponseBody
    public ResultData<String> feedback(@RequestBody FeedBack feedBack) {
        logger.info("feedback request: {}", feedBack.toString());
        AgentChatInfo agentChatInfo = agentService.findAgentChatById(feedBack.getId());
        if (agentChatInfo == null) {
            return ResultData.fail("对应的消息记录不存在");
        }
        agentChatInfo.setFeedback(feedBack.getFeedback());
        agentService.saveAgentChat(agentChatInfo);
        return ResultData.success("评价成功");
    }

    /**
     * 根据智能体id获得聊天记录(提供部分必要字段)
     *
     * @param agentId 智能体id
     * @param keyword 关键词(非必须)
     * @return 聊天记录
     */
    @PostMapping("/findAgentChatByAgentId")
    public ResultData<List<Map>> findAgentChatByAgentId(
            @RequestParam("agentId") String agentId,
            @RequestParam(value = "keyword",defaultValue = "") String keyword
    ) {
        List<Map> result = agentService.findAgentChatByAgentId(agentId,keyword);
        return ResultData.success("查询成功", result);
    }

    /**
     * 根据聊天记录id获得聊天记录
     *
     * @param chatId 聊天记录id
     * @return 聊天记录
     */
    @PostMapping("/findAgentChatByChatId")
    public ResultData<AgentChatInfo> findAgentChatByChatId(@RequestParam("chatId") String chatId) {
        return ResultData.success("查询成功", agentService.findAgentChatByChatId(chatId));
    }

    /**
     * 根据id修改智能体对话标题
     *
     * @param agentChatId 智能体对话id
     * @param title       修改后的标题
     * @return 修改结果
     */
    @PostMapping("/updateAgentChatTitle")
    public ResultData<String> updateAgentChatTitle(@RequestParam("agentChatId") String agentChatId, @RequestParam("title") String title) {
        logger.info("根据智能体id修改标题");
        agentService.updateAgentChatTitle(agentChatId, title);
        return ResultData.success("修改成功");
    }

    /**
     * 根据智能体知识库文件id获得文件二进制文件
     * @param fileId 智能体知识库文件id
     * @return 文件二进制流
     * @throws Exception 异常
     */
    @PostMapping("/getAgentKnowledgeBaseByFileId")
    public ResponseEntity<Resource> getAgentKnowledgeBaseByFileId(@RequestParam("fileId") FileId fileId) throws Exception {
        if(fileId == null){
            throw new BadRequestException("参数不可为空");
        }
        return fileId.isLocal() ? fileUploadInfoService.getFile(fileId.getFileId()) : fileService.getKnowledgeFileById(fileId.getFileId());
    }


}
