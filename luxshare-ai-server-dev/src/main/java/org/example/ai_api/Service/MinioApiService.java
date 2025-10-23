package org.example.ai_api.Service;

import lombok.extern.slf4j.Slf4j;
import org.example.ai_api.Bean.Entity.FolderList;
import org.example.ai_api.Bean.Entity.KnowledgeFileInfo;
import org.example.ai_api.Bean.Entity.SessionFile;
import org.example.ai_api.Bean.Enum.FileHeaderGenerator;
import org.example.ai_api.Bean.Model.FileDownloadResponse;
import org.example.ai_api.Bean.Model.MCPFileInfo;
import org.example.ai_api.Exception.BadRequestException;
import org.example.ai_api.Exception.NotFoundException;
import org.example.ai_api.Persistence.Dao.FolderListDao;
import org.example.ai_api.Persistence.Dao.KnowledgeFileDao;
import org.example.ai_api.Persistence.Dao.SessionFileDao;
import org.example.ai_api.Utils.ContentTypeDetector;
import org.example.ai_api.Utils.MinioOperations;
import org.example.ai_api.Utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 10353965
 */
@Slf4j
@Service
public class MinioApiService {
    @Autowired
    private KnowledgeFileDao knowledgeFileDao;
    @Autowired
    private SessionFileDao sessionFileDao;
    @Autowired
    private MinioOperations minioOperations;
    @Autowired
    private FolderListDao folderListDao;
    @Value("${minio.bucketName}")
    private String bucketName;
    @Value("${minio-proxy}")
    private String minioProxy;
    @Value("${local}")
    private String local;
    private static final String BUCKET_NAME = "ai-artifacts";
    private static final String EXCEL_TRANSLATED = "excelTranslated";

    /**
     * 根据用户id列出用户知识库的文件,支持参数指定文件类型
     * @param userId 用户id
     * @param fileTypes 文件类型，默认列出所有文件
     * @return 文件列表
     */
    public List<MCPFileInfo> listFiles(String userId, List<String> fileTypes){
        log.info("userId:{},fileTypes:{}",userId,fileTypes);
        List<KnowledgeFileInfo> knowledgeFileInfos = knowledgeFileDao.findPrivateFilesByUserIdAndFileType(userId,fileTypes);
        // 将KnowledgeFileInfo转换为MCPFileInfo
        return knowledgeFileInfos.stream().map(knowledgeFileInfo -> {
            MCPFileInfo mcpFileInfo = new MCPFileInfo();
            mcpFileInfo.setFileName(knowledgeFileInfo.getFileName());
            String folderName = folderListDao.findById(knowledgeFileInfo.getFolderId()).getFolderName();
            mcpFileInfo.setFolderName(folderName);
            return mcpFileInfo;
        }).collect(Collectors.toList());
    }

    /**
     * 根据用户id列出会话历史文件
     * @param userId 用户id
     * @param sessionId 会话id
     * @return 文件列表
     */
    public List<MCPFileInfo> listFilesBySessionId(String userId, String sessionId){
        log.info("userId:{},sessionId:{}",userId,sessionId);
        List<SessionFile> sessionFiles =  sessionFileDao.findByUserIdAndSessionId(userId,sessionId);
        return sessionFiles.stream().map(sessionFile -> {
            MCPFileInfo mcpFileInfo = new MCPFileInfo();
            mcpFileInfo.setFileName(sessionFile.getFileName());
            mcpFileInfo.setIteration(sessionFile.getIteration());
            String folderName = folderListDao.findById(sessionFile.getFolderId()).getFolderName();
            mcpFileInfo.setFolderName(folderName);
            return mcpFileInfo;
        }).collect(Collectors.toList());
    }

    /**
     * 根据文件名下载文件
     * @param userId 用户id
     * @param sessionId 会话id
     * @param fileName 文件名
     * @param iteration 对话轮数
     * @return 文件下载链接
     * @throws Exception 异常
     */
    public FileDownloadResponse downloadFile(String userId,String sessionId,String fileName,int iteration,String folderName) throws Exception {
        log.info("userId:{},sessionId:{},fileName:{},iteration:{},folderName:{}",userId,sessionId,fileName,iteration,folderName);
        //将文件夹名称替换为文件夹id
        String folderId = getFolderIdByFolderName(folderName,userId);
        String objectName = String.format("%s/%s/%s/%s/%s",userId,sessionId,iteration,folderId,fileName);
        SessionFile sessionFile = sessionFileDao.findByObjectName(objectName);
        if (sessionFile == null) {
            sessionFile = handleMissingSessionFileAndIterationZero(userId, fileName, iteration,folderId, objectName);
        }
        Map<String,String> downloadReq = Utils.buildFileHeaders(fileName, FileHeaderGenerator.DOWNLOAD);
        String downloadUrl = minioOperations.getDownloadUrl(BUCKET_NAME,objectName,3600,downloadReq);
        downloadUrl = Utils.exchangeFileUrl(downloadUrl,local,minioProxy);
        return new FileDownloadResponse(downloadUrl,fileName,BUCKET_NAME,objectName,iteration,sessionFile.isReadOnly(),sessionFile.getFileSize());
    }

    /**
     * 上传文件
     * @param userId 用户id
     * @param sessionId 会话id
     * @param fileName 文件名
     * @param iteration 对话轮数
     * @param file 文件
     * @return 完成上传的文件信息
     * @throws Exception 异常
     */
    public SessionFile uploadFile(String userId, String sessionId, String fileName, int iteration, String folderName, MultipartFile file) throws Exception{
        log.info("userId:{},sessionId:{},fileName:{},iteration:{},folderName:{}",userId,sessionId,fileName,iteration,folderName);
        //将文件夹名称替换为文件夹id
        String folderId = getFolderIdByFolderName(folderName,userId);
        // 构造Minio对象名
        String objectName = String.format("%s/%s/%s/%s/%s",userId,sessionId,iteration,folderId,fileName);
        // 检查桶名是否存在
        if (!minioOperations.bucketExists(BUCKET_NAME)) {
            minioOperations.createBucket(BUCKET_NAME);
        }
        // 将文件上传到Minio
        minioOperations.uploadFile(BUCKET_NAME,objectName,file.getInputStream(),file.getSize(), ContentTypeDetector.getContentType(fileName));
        // 根据对象名获取文件
        SessionFile sessionFile = sessionFileDao.findByObjectName(objectName);
        if(sessionFile == null){
            // 如果数据库中不存在该文件，则新建一个
            sessionFile = new SessionFile(null,fileName,iteration,file.getSize(),folderId,objectName,Utils.getNowDate(),false);
        }else {
            // 如果数据库中存在该文件，则更新文件信息
            sessionFile.setFileName(fileName);
            sessionFile.setIteration(iteration);
            sessionFile.setFileSize(file.getSize());
        }
        return sessionFileDao.save(sessionFile);
    }

    //将已经完成翻译的文件上传到minio对应路径下
    public String uploadExcelTranslated(String sessionId,MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        String objectName = String.format("%s/%s/%s",EXCEL_TRANSLATED,sessionId,fileName);
        minioOperations.uploadFile(objectName,file.getInputStream(),file.getSize(),ContentTypeDetector.getContentType(fileName));
        return objectName;
    }

    /**
     * 处理 SessionFile 不存在且 iteration 为 0 的特殊逻辑
     * @param userId 用户ID
     * @param fileName 文件名
     * @param iteration 对话轮数
     * @param objectName Minio对象名
     * @return 处理后的 SessionFile 对象
     * @throws Exception 抛出异常
     */
    private SessionFile handleMissingSessionFileAndIterationZero(String userId, String fileName, int iteration, String folderId, String objectName) throws Exception {
        log.info("objectName:{}",objectName);
        if (iteration != 0) {
            throw new BadRequestException("文件在ai-artifacts桶中不存在且iteration不为0");
        }
        // iteration == 0, and sessionFile == null
        // 构造知识库中的文件对象名
        String objectNameInBucket = String.format("private/%s/%s/%s", userId, folderId ,fileName);
        // 检查知识库中文件存在性
        if (!minioOperations.objectExists(bucketName, objectNameInBucket)) {
            throw new NotFoundException("文件不存在");
        }
        // 将知识库中的文件复制到ai-artifacts桶中
        minioOperations.copyObject(bucketName, objectNameInBucket, BUCKET_NAME, objectName);
        // 获取文件大小
        long fileSize = minioOperations.getObjectStat(bucketName, objectNameInBucket).size();
        // 将复制的文件信息写入数据库，标记readOnly为true
        return sessionFileDao.save(new SessionFile(null, fileName, 0, fileSize, folderId ,objectName, Utils.getNowDate(), true));
    }

    private String getFolderIdByFolderName(String folderName,String userId){
        List<FolderList> folderLists = folderListDao.findByFolderNameAndUserId(folderName,userId);
        if(folderLists == null||folderLists.isEmpty()){
            throw new NotFoundException("不存在对应文件夹");
        }
        return folderLists.get(0).getId();
    }

}
