package org.example.ai_api.Service;

import org.example.ai_api.Bean.Entity.Agent;
import org.example.ai_api.Bean.Entity.AgentChatInfo;
import org.example.ai_api.Bean.Entity.FileUpload;
import org.example.ai_api.Bean.Entity.KnowledgeFileInfo;
import org.example.ai_api.Bean.Model.AgentKnowledgeFile;
import org.example.ai_api.Bean.Model.Persona;
import org.example.ai_api.Exception.BadRequestException;
import org.example.ai_api.Exception.DataNotComplianceException;
import org.example.ai_api.Exception.NotFoundException;
import org.example.ai_api.Persistence.Dao.AgentChatDao;
import org.example.ai_api.Persistence.Dao.AgentDao;
import org.example.ai_api.Persistence.Repository.AgentChatRepository;
import org.example.ai_api.Persistence.Repository.AgentRepository;
import org.example.ai_api.Persistence.Repository.FileUploadInfoRepository;
import org.example.ai_api.Persistence.Repository.KnowledgeFileRepository;
import org.example.ai_api.Utils.Utils;
import org.example.ai_api.Utils.MinioOperations;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 智能体相关服务
 * @author 10353965
 */
@Service
public class AgentService {
    private static final Logger logger = LoggerFactory.getLogger(AgentService.class);
    @Autowired
    private AgentRepository agentRepository;
    @Autowired
    private AgentChatRepository agentChatRepository;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private AgentDao agentDao;
    @Autowired
    private AgentChatDao agentChatDao;
    @Autowired
    private MinioOperations minioOperations;
    @Autowired
    private KnowledgeFileRepository knowledgeFileRepository;
    @Autowired
    private FileUploadInfoRepository fileUploadInfoRepository;
    @Value("${minio-proxy}")
    private String minioProxy;
    @Value("${local}")
    private String local;

    /**
     * 保存或修改智能体
     * @param agent 智能体
     * @return 保存后的智能体
     */
    public Agent saveAgent(Agent agent) {
        if(agent == null){
            throw new BadRequestException("智能体信息为空");
        }
        if(!isAgentNameUnique(agent)){
            throw new DataNotComplianceException("智能体名称已存在");
        }
        if(agent.getId() == null){
            throw new BadRequestException("智能体id为空");
        }
        if (agent.getId().isEmpty()) {
            agent.setId(null);
        }
        agent.setUpdateTime(Utils.getNowDate());
        if (agent.getCreateTime() == null) {
            agent.setCreateTime(Utils.getNowDate());
        }
        agent.setLastOperationTime(Utils.getNowDate());
        if(agent.getAgentPic() != null && !agent.getAgentPic().isEmpty()){
            agent.setAgentPicUrl(getPicUrl(agent.getAgentPic()));
        }
        //更新智能体知识库文件
        updateAgentKnowledgeBase(agent);
        return agentRepository.save(agent);
    }

    /**
     * 根据id查询智能体
     * @param id 智能体id
     * @return 智能体信息
     */
    public Agent findAgentById(String id) {
        return agentRepository.findById(id).orElseThrow(() -> new NotFoundException("不存在对应id的智能体"));
    }

    /**
     * 根据id查询智能体详细信息（带字段别名）
     * @param id 智能体id
     * @return 智能体详细信息（Map格式，带字段别名）
     */
    public Map findAgentByIdWithFields(String id) {
        Map result = agentDao.findAgentByIdWithFields(id);
        if (result == null) {
            throw new NotFoundException("不存在对应id的智能体");
        }
        Utils.convertMongoIdToStringId(result,"agentId", true);
        // 为智能体生成新的预览链接并更新到数据库
        updateAgentPicUrl(result);
        return result;
    }

    /**
     * 根据用户id查询智能体
     * @param userId 用户id
     * @return 智能体信息
     */
    public List<Map> findAgentByUserId(String userId, String keyword) {
        List<Map> result = agentDao.findAgentByUserIdWithFields(userId, keyword);
        Utils.convertMongoIdToStringId(result,"agentId", true);
        // 为每个智能体生成新的预览链接并更新到数据库
        for (Map agentMap : result) {
            updateAgentPicUrl(agentMap);
        }
        return result;
    }

    /**
     * 删除智能体，并同步删除智能体聊天记录
     * @param id 智能体id
     */
    public void deleteAgentById(String id) throws Exception {
        Agent agent = findAgentById(id);
        //删除智能体
        agentDao.deleteById(id);
        //删除minio头像
        if (agent.getAgentPic()!= null&& !agent.getAgentPic().isEmpty()){
            minioOperations.deleteFile(agent.getAgentPic());
        }
        //同步删除智能体聊天记录
        agentChatDao.deleteByAgentId(id);
    }

    /**
     * 根据智能体id和用户id获得聊天记录
     * @param agentId 智能体id
     * @param userId  用户id
     * @return 聊天记录
     */
    public List<AgentChatInfo> findAgentChatByAgentIdAndUserId(String agentId, String userId) {
        return agentChatDao.findAgentChatByAgentIdAndUserId(agentId, userId);
    }

    /**
     * 根据id获得智能体聊天记录
     */
    public AgentChatInfo findAgentChatById(String id) {
        return agentChatRepository.findById(id).orElseThrow(() -> new NotFoundException("不存在对应id的智能体聊天记录"));
    }

    /**
     * 删除智能体聊天记录
     * @param agentChatId 聊天记录id
     * @param userId   用户id
     */
    public void deleteAgentChatById(String agentChatId, String userId) {
        AgentChatInfo agentChatInfo = agentChatRepository.findById(agentChatId).orElseThrow(() -> new NotFoundException("不存在对应id的智能体聊天记录"));
        if (!agentChatInfo.getUserId().equals(userId)) {
            throw new NotFoundException("聊天记录不属于该用户");
        }
        agentChatDao.deleteById(agentChatId);
    }

    /**
     * 批量删除智能体聊天记录
     * @param agentChatIds 聊天记录id
     */
    public void deleteAgentChatByIds(List<String> agentChatIds) {
        for (String agentChatId : agentChatIds) {
            AgentChatInfo agentChatInfo = agentChatRepository.findById(agentChatId).orElseThrow(() -> new NotFoundException("不存在对应id的智能体聊天记录"));
            agentChatDao.deleteById(agentChatId);
        }
    }

    /**
     * 保存智能体聊天记录
     * @param agentChatInfo 智能体聊天记录
     * @return 保存结果
     */
    public AgentChatInfo saveAgentChat(AgentChatInfo agentChatInfo) {
        if (agentChatInfo.getId() == null||agentChatInfo.getId().isEmpty()) {
            agentChatInfo.setId(null);
            agentChatInfo.setCreateTime(Utils.getNowDate());
        }else {
            agentChatInfo.setUpdateTime(Utils.getNowDate());
        }
        agentChatInfo.setLastOperationTime(Utils.getNowDate());
        if (agentChatInfo.getAgentId() == null) {
            throw new BadRequestException("智能体id为空");
        }
        Agent agent = agentRepository.findById(agentChatInfo.getAgentId()).orElseThrow(() -> new NotFoundException("不存在对应id的智能体"));
        agent.setLastOperationTime(Utils.getNowDate());
        agentRepository.save(agent);
        return agentChatRepository.save(agentChatInfo);
    }

    /**
     * 检查智能体名称是否唯一
     * @param agent 智能体
     * @return 是否唯一
     */
    public boolean isAgentNameUnique(Agent agent) {
        // 1. 参数检查
        if (agent.getUserId() == null || agent.getPersona() == null) {
            throw new IllegalArgumentException("userId和persona不能为空");
        }
        // 2. 构建查询条件（同一用户下相同名称）
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(agent.getUserId()));
        query.addCriteria(Criteria.where("persona.name").is(agent.getPersona().getName()));
        // 3. 排除当前对象（修改场景）
        if (agent.getId() != null && !agent.getId().isEmpty()) {
            query.addCriteria(Criteria.where("id").ne(agent.getId()));
        }
        // 4. 执行存在性检查
        return !mongoTemplate.exists(query, Agent.class);
    }

    /**
     * 根据智能体id获得智能体相关聊天记录
     * @param agentId 智能体id
     * @return 聊天记录
     */
    public List<Map> findAgentChatByAgentId(String agentId, String keyword) {
       List<Map> result = agentChatDao.findAgentChatByAgentIdWithFields(agentId, keyword);
       Utils.convertMongoIdToStringId(result, "agentChatId",true);
       return result;
    }

    public AgentChatInfo findAgentChatByChatId(String chatId) {
        return agentChatDao.findAgentChatByChatId(chatId);
    }

    /**
     * 上传用户头像图片到Minio，返回minio存储路径（objectName）
     * @param file 用户上传的头像文件
     * @return minio存储路径（如 AgentPic/xxx.jpg）
     * @throws Exception 上传失败抛出异常
     */
    public String uploadPic(MultipartFile file) throws Exception {
        // 1. 生成唯一文件名
        String originalFileName = file.getOriginalFilename();
        String fileName = Utils.generateUniqueFileName(originalFileName);
        validateMinioObjectName(fileName);
        // 2. 拼接头像子目录路径
        String objectName = "AgentPic/" + fileName;
        // 3. 上传到minio
        minioOperations.uploadFile(objectName, file.getInputStream(), file.getSize(), file.getContentType());
        // 4. 返回minio存储路径
        return objectName;
    }

    /**
     * 验证 MinIO 对象名的合法性
     * @param objectName MinIO 对象名
     * @throws IllegalArgumentException 如果对象名不合法
     */
    private void validateMinioObjectName(String objectName) {
        if (objectName == null || objectName.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        // 检查是否包含空格
        if (objectName.contains(" ")) {
            throw new IllegalArgumentException("文件名不能包含空格: " + objectName);
        }
        // 检查是否包含 MinIO 不允许的字符
        // MinIO 不允许的字符：\ / : * ? " < > |
        String invalidCharsPattern = "[\\\\/:*?\"<>|]";
        if (objectName.matches(".*" + invalidCharsPattern + ".*")) {
            throw new IllegalArgumentException("文件名包含非法字符: " + objectName);
        }
        // 检查是否以斜杠开头或结尾
        if (objectName.startsWith("/") || objectName.endsWith("/")) {
            throw new IllegalArgumentException("文件名不能以斜杠开头或结尾: " + objectName);
        }
        // 检查是否包含连续的斜杠
        if (objectName.contains("//")) {
            throw new IllegalArgumentException("文件名不能包含连续的斜杠: " + objectName);
        }
        // 检查长度限制（MinIO 对象名最大长度为 1024 字节）
        if (objectName.getBytes().length > 1024) {
            throw new IllegalArgumentException("文件名长度超过限制(最大1024字节): " + objectName);
        }
        // 检查是否包含控制字符
        for (char c : objectName.toCharArray()) {
            if (Character.isISOControl(c)) {
                throw new IllegalArgumentException("文件名包含控制字符: " + objectName);
            }
        }
    }

    /**
     * 根据minio对象名获取带有效期的预览链接
     * @param objectName minio对象名（如 AgentPic/xxx.jpg）
     * @return 临时可访问url
     */
    public String getPicUrl(String objectName) {
        int expireSeconds = 7*24*3600; // 一周有效期
        if (objectName == null || objectName.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        try {
            String url = minioOperations.getDownloadUrl(objectName, expireSeconds, null);
            return Utils.exchangeFileUrl(url,local,minioProxy);
        } catch (Exception e) {
            throw new RuntimeException("获取图片预览链接失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新智能体标题
     * @param agentChatId 智能体聊天记录id
     * @param title 标题
     */
    public void updateAgentChatTitle(String agentChatId, String title) {
        AgentChatInfo agentChatInfo = agentChatRepository.findById(agentChatId).orElseThrow(() -> new NotFoundException("不存在对应id的智能体聊天记录"));
        agentChatInfo.setTitle(title);
        agentChatInfo.setUpdateTime(Utils.getNowDate());
        agentChatInfo.setLastOperationTime(Utils.getNowDate());
        agentChatRepository.save(agentChatInfo);
    }

    /**
     * 更新智能体知识库信息
     */
    private void updateAgentKnowledgeBase(Agent agent) {
        // 1. 获取智能体知识库信息
        Persona persona = agent.getPersona();
        List<AgentKnowledgeFile> files = persona.getFiles();
        if (files == null || files.isEmpty()) {
            return;
        }
        // 2. 遍历更新智能体知识库信息
        for (AgentKnowledgeFile file : files) {
            //更新单个文件
            updateSingleAgentKnowledgeFile(file);
        }
    }

    /**
     * 更新单个智能体知识库文件信息
     */
    private void updateSingleAgentKnowledgeFile(AgentKnowledgeFile file) {
        String fileName;
        if(file.getIsLocal()){
            //本地文件更新
            FileUpload fileUpload = fileUploadInfoRepository.findById(file.getFileId()).orElseThrow(() -> new NotFoundException("不存在对应id的文件信息"));
            fileName = fileUpload.getOriginalFileName();
        }else{
            //个人知识库文件更新
            KnowledgeFileInfo fileInfo = knowledgeFileRepository.findById(file.getFileId()).orElseThrow(() -> new NotFoundException("不存在对应id的文件信息"));
            fileName = fileInfo.getOriginalFileName();
        }
        if (fileName == null) {
            throw new RuntimeException("文件名为空");
        }
        file.setFileName(fileName);
        file.setExtension(Utils.getFileExtension(fileName));
    }

    /**
     * 为智能体生成新的预览链接并更新到数据库
     * @param agentMap 智能体信息Map
     */
    private void updateAgentPicUrl(Map agentMap) {
        String agentId = (String) agentMap.get("agentId");
        String agentPic = (String) agentMap.get("agentPic");
        String currentAgentPicUrl = (String) agentMap.get("agentPicUrl"); // 获取当前链接

        if (agentPic != null && !agentPic.isEmpty()) {
            try {
                // 如果当前链接未过期且有效，则不刷新
                if (currentAgentPicUrl != null && !minioOperations.isMinioUrlExpiredOrInvalid(currentAgentPicUrl)) {
                    logger.debug("智能体 {} 的预览链接未过期，无需刷新。", agentId);
                    return;
                }

                // 生成新的预览链接
                String newPicUrl = getPicUrl(agentPic);
                // 更新数据库中的agentPicUrl字段
                Agent agent = findAgentById(agentId);
                agent.setAgentPicUrl(newPicUrl);
                agentRepository.save(agent);
                // 在返回结果中添加更新后的预览链接
                agentMap.put("agentPicUrl", newPicUrl);
            } catch (Exception e) {
                logger.error("为智能体 {} 生成预览链接失败: {}", agentId, e.getMessage());
                // 如果生成失败，保持原有逻辑，不中断整个查询
            }
        }
    }
}
