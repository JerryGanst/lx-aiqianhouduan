package org.example.ai_api.Service;

import io.minio.*;
import org.example.ai_api.Bean.ApiRepeat.UnifiedChatRepeat;
import org.example.ai_api.Bean.Entity.*;
import org.example.ai_api.Bean.Enum.FileHeaderGenerator;
import org.example.ai_api.Bean.Enum.KnowledgeFileAction;
import org.example.ai_api.Bean.Model.FileInfoFormSystem;
import org.example.ai_api.Bean.Model.Source;
import org.example.ai_api.Exception.DataNotComplianceException;
import org.example.ai_api.Exception.NotAccessedException;
import org.example.ai_api.Exception.NotFoundException;
import org.example.ai_api.Persistence.Dao.DepartmentFileDao;
import org.example.ai_api.Persistence.Dao.FolderListDao;
import org.example.ai_api.Persistence.Dao.KnowledgeFileDao;
import org.example.ai_api.Persistence.Repository.FileConversionTaskRepository;
import org.example.ai_api.Persistence.Repository.FilesRepository;
import org.example.ai_api.Persistence.Repository.KnowledgeFileRepository;
import org.example.ai_api.Persistence.Repository.TargetRepository;
import org.example.ai_api.Strategy.KnowledgeFileSort.FileSortContext;
import org.example.ai_api.Strategy.KnowledgeFileSort.FileSortStrategy;
import org.example.ai_api.Utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.*;
import java.util.concurrent.Executor;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.ParameterizedTypeReference;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库文件相关服务.
 * @author 10353965
 */
@Service
public class FileService {
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    @Autowired
    private FileUploadUtils fileUploadUtils;
    @Autowired
    private SystemFileService systemFileService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private FilesRepository filesRepository;
    @Autowired
    private FileConverter fileConverter;
    @Autowired
    private KnowledgeFileRepository knowledgeFileRepository;
    @Autowired
    private FileConversionTaskRepository fileConversionTaskRepository;
    @Autowired
    private TargetRepository targetRepository;
    @Autowired
    private DepartmentFileDao departmentFileDao;
    @Autowired
    private FileSortContext fileSortContext;
    @Autowired
    private UserPermissionService userPermissionService;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private FileContentReader fileContentReader;
    @Autowired
    private MinioOperations minioOperations;
    @Autowired
    @Qualifier("StreamWebClient")
    private WebClient webClient;
    @Autowired
    @Qualifier("taskExecutor")
    private Executor taskExecutor;
    @Autowired
    private AIPlatformSyncService aiPlatformSyncService;
    @Value("${minio.bucketName}")
    private String bucketName;
    @Value("${minio.endpoint}")
    private String endpoint;
    @Value("${libreoffice_tasks}")
    private String libreOfficeTasks;
    @Value("${libreoffice_publicConvert}")
    private String libreOfficePublicConvert;
    @Value("${downloadTimeout}")
    private int downloadTimeout;
    @Value("${minio-proxy}")
    private String minioProxy;
    @Value("${local}")
    private String local;
    @Value("${minio.bucketName}")
    private String minioBucketName;
    @Autowired
    private KnowledgeFileDao knowledgeFileDao;
    @Autowired
    private FolderListDao  folderListDao;

    /**
     * 使用静态 final Map 来存储转换规则，确保只初始化一次且不可变
     */
    private static final Map<String, String> CONVERSION_RULES = new HashMap<>();

    // 静态初始化块，用于填充转换规则
    static {
        CONVERSION_RULES.put("ppt", "pptx");
        CONVERSION_RULES.put("pptx", "pptx");
        CONVERSION_RULES.put("xls", "pdf");
        CONVERSION_RULES.put("xlsx", "pdf");
        CONVERSION_RULES.put("doc", "pdf");
        CONVERSION_RULES.put("docx", "pdf");
    }

    /**
     * 根据文件名获得文件链接.
     *
     * @param fileName 文件名
     * @return 文件信息结构体
     */
    public FileInfo findByFileName(String fileName) {
        return filesRepository.findByFileName(fileName);
    }

    public List<KnowledgeFileInfo> saveAll(List<KnowledgeFileInfo> knowledgeFileInfos) {
        return knowledgeFileRepository.saveAll(knowledgeFileInfos);
    }

    public KnowledgeFileInfo getFileById(String fileId) {
        return knowledgeFileRepository.findById(fileId).orElseThrow(() -> new NotFoundException("文件不存在"));
    }

    /**
     * 根据id获得知识库二进制文件.
     *
     * @param fileId 文件id
     * @return 根据文件信息构造的二进制文件
     * @throws Exception 异常
     */
    public ResponseEntity<Resource> getKnowledgeFileById(String fileId) throws Exception {
        String path;
        String fileName;
        try{
            logger.info("getKnowledgeFileById,尝试从个人知识库中获取,文件id:{}", fileId);
            KnowledgeFileInfo fileInfo = knowledgeFileRepository.findById(fileId).orElseThrow(() -> new NotFoundException("文件不存在"));
            path = fileInfo.getStoragePath().replace(bucketName + "/", "");
            fileName = fileInfo.getFileName();
        }catch (Exception e){
            logger.warn("个人知识库文件不存在，尝试从部门知识库中获取,文件id:{}", fileId);
            DepartmentFile departmentFile = departmentFileDao.findById(fileId);
            path = departmentFile.getStoragePath().replace(bucketName + "/", "");
            fileName = departmentFile.getFileName();
        }
        InputStream fileStream = minioOperations.getFileStream(path);
        return Utils.exchangeInputStreamToResource(fileStream, fileName);
    }

    /**
     * 根据需求排序文件列表.
     *
     * @param fileList 待排序文件列表
     * @param sortType 排序方式
     * @return 排序处理后的列表
     */
    public List<KnowledgeFileInfo> sortFileList(List<KnowledgeFileInfo> fileList, String sortType, boolean increase) {
        FileSortStrategy strategy = fileSortContext.getStrategy(sortType);
        List<KnowledgeFileInfo> result;
        if (strategy == null) {
            result = fileList;
        } else {
            result = strategy.sort(fileList);
            if (!increase) {
                Collections.reverse(result);
            }
        }
        return result;
    }

    /**
     * 搜索文件信息
     *
     * @param keyword  用户输入的关键字(支持模糊查询)
     * @param target   查询的领域
     * @param userId   查询者id
     * @param isPublic 是否查询公共领域
     * @return 查询结果
     */
    public List<KnowledgeFileInfo> searchFile(String keyword, String target, String userId, boolean isPublic) {
        Criteria criteria = new Criteria();
        criteria.and("isPublic").is(isPublic);
        if (isPublic) {
            //前置权限检查
            if (!userPermissionService.checkUserPermission(userId, target).isRead()) {
                throw new NotAccessedException("无权限访问");
            }
            criteria.and("fileTarget").is(target);
        } else {
            criteria.and("uploaderId").is(userId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            criteria.and("fileName").regex(keyword);
        }
        return mongoTemplate.find(Query.query(criteria), KnowledgeFileInfo.class);
    }

    /**
     * 知识库公共文件上传.
     *
     * @param files  文件列表
     * @param userId 用户id
     * @return 上传后的文件信息列表
     */
    public List<KnowledgeFileInfo> knowledgeFileUpload(List<MultipartFile> files, String userId, String target,String folderId, boolean isPublic) throws Exception {
        //前置检查,返回转换后的在服务器合法的文件名列表
        List<String> fileNames = fileUploadUtils.checkBeforeUpload(files, userId);
        //检查文件是否重复，并获取hash值
        List<String> fileHash = checkFileHash(files, isPublic, userId, target, folderId);
        //检查是否存在文件重名
        checkFileExist(fileNames, target, userId, isPublic, folderId);
        //文件上传,返回文件路径列表
        List<Path> filePath = uploadFiles(files, fileNames, target, userId, folderId, isPublic);
        logger.info("用户{}上传{}个文件到领域{}", userId, files.size(), target);
        //构建并返回上传文件信息
        return buildUploadFileInfo(files, fileHash, userId, filePath, fileNames, target, folderId, isPublic);
    }

    /**
     * 公共知识库文件删除
     *
     * @param fileId 将删除的文件id
     * @param userId 操作者id
     * @throws Exception 操作过程报错
     */
    public void knowledgeFileDelete(String fileId, String userId) throws Exception {
        logger.info("用户{}尝试删除文件{}", userId, fileId);
        KnowledgeFileInfo fileInfo = knowledgeFileRepository.findById(fileId).orElseThrow(() -> new NotFoundException("文件不存在"));
        // 确保传递 folderId
        String folderId = fileInfo.getFolderId();
        String name = minioOperations.createKnowledgeFileName(fileInfo.getFileName(), fileInfo.getFileTarget(), fileInfo.getUploaderId(), folderId, fileInfo.isPublic());
        minioOperations.deleteFile(name);
        deleteConvertKnowledgeFile(fileId);
        knowledgeFileDao.deleteById(fileId);
        //同步删除ai平台文件
        logger.info("同步删除ai平台文件{}",fileInfo.getAiFileId());
        aiPlatformSyncService.deleteFileFromAIPlatform(fileInfo.getAiFileId())
                .doOnSuccess(v -> logger.info("已删除AI平台数据: {}", fileInfo.getAiFileId()))
                .doOnError(e -> logger.error("AI平台数据删除失败: {}", e.getMessage()))
                .subscribe();
    }

    /**
     * 根据id获得文件文本
     */
    public String getContentById(String id) throws Exception {
        logger.info("根据id{}获得文件文本", id);
        KnowledgeFileInfo fileInfo = knowledgeFileRepository.findById(id).orElseThrow(() -> new NotFoundException("文件不存在"));
        InputStream stream = minioOperations.getFileStream(fileInfo.getStoragePath().replace(bucketName + "/", ""));
        return fileContentReader.readFile(stream, fileInfo.getFileName());
    }

    /**
     * 删除源文件时，同步删除转换任务或转换后的文件
     *
     * @param fileId 原文件id
     * @throws Exception 操作过程报错
     */
    public void convertFilesDelete(String fileId) throws Exception {
        logger.info("开始删除文件{}转换后的文件", fileId);
        FileConversionTask task = fileConversionTaskRepository.findByFileId(fileId);
        if (task != null) {
            fileConversionTaskRepository.deleteById(task.getTaskId());
        } else {
            return;
        }
        if ("COMPLETED".equals(task.getStatus())) {
            minioOperations.deleteFile(task.getConvertedFilePath());
        }
        logger.info("文件{}转换文件已删除", fileId);
    }

    /**
     * 删除文件时，同步删除转换后的文件
     *
     * @param fileId 文件id
     * @throws Exception 操作过程报错
     */
    public void deleteConvertKnowledgeFile(String fileId) throws Exception {
        logger.info("删除文件{}转换后的文件", fileId);
        KnowledgeFileInfo info = knowledgeFileRepository.findById(fileId).orElseThrow(() -> new NotFoundException("文件不存在"));
        if(info.getConvertPath()!=null&&!info.getConvertPath().isEmpty()){
            minioOperations.deleteFile(info.getConvertPath());
        }
        logger.info("文件{}转换已删除", fileId);
    }

    /**
     * 公共知识库文件下载(3分钟链接过期)
     *
     * @param fileId 将下载的文件id
     * @return 文件下载链接
     * @throws Exception 操作过程报错
     */
    public String getDownloadUrl(String fileId) throws Exception {
        KnowledgeFileInfo file = knowledgeFileRepository.findById(fileId).orElseThrow(() -> new NotFoundException("文件不存在"));
        String name = file.getStoragePath().replace("/" + bucketName + "/", "");
        // 添加强制下载的响应头
        Map<String, String> reqParams = Utils.buildFileHeaders(file.getFileName(), FileHeaderGenerator.DOWNLOAD);
        String url = minioOperations.getDownloadUrl(name, downloadTimeout , reqParams);
        return Utils.exchangeFileUrl(url, local, minioProxy);
    }

    /**
     * 检查是否有对某个文件的访问权限
     *
     * @param fileId 文件id
     * @param userId 操作者id
     */
    public void checkUserPermissionForFile(String fileId, String userId) {
        KnowledgeFileInfo fileInfo = getFileById(fileId);
        //权限检查
        if (fileInfo.isPublic()) {
            //属于公共领域权限检查
            if (!userPermissionService.checkUserPermission(userId, fileInfo.getFileTarget()).isRead()) {
                throw new NotAccessedException("无权限访问该领域");
            }
        } else {
            //属于私有领域权限检查
            if (!userId.equals(fileInfo.getUploaderId())) {
                throw new NotAccessedException("无权限访问该文件");
            }
        }
    }

    /**
     * 上传文件，同步创建转换任务
     *
     * @param files 完成上传后的文件列表
     */
    public void buildFileConversionTask(List<KnowledgeFileInfo> files) {
        List<FileConversionTask> result = new ArrayList<>();
        for (KnowledgeFileInfo file : files) {
            FileConversionTask task = new FileConversionTask();
            task.setFileId(file.getId());
            task.setCreateTime(LocalDateTime.now());
            task.setTargetFormat(getTargetFormat(file.getOriginalFileName()));
            task.setStatus("PENDING");
            result.add(task);
        }
        fileConversionTaskRepository.saveAll(result);
    }

    /**
     * 根据标签从文件系统获取文件列表
     *
     * @param target 文件标签
     * @return 文件列表
     * @throws Exception 异常
     */
    @Cacheable(value = "filesByTarget", key = "#target", unless = "#result == null")
    public List<FileInfoFormSystem> getFileByTarget(String target) throws Exception {
        List<FileInfoFormSystem> fileInfos = systemFileService.getFileInfoFromSystem();
        Target targetInfo = targetRepository.findByTargetName(target);
        if (targetInfo == null) {
            throw new NotFoundException("领域不存在");
        }
        String finalCategory = targetInfo.getCategory();
        //找到对应类别文件的父文件夹id
        String id = fileInfos.stream()
                .filter(file -> file.getCategory().equals(finalCategory))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("文件目录不存在"))
                .getId();
        //根据父文件夹id在列表中筛选数据
        return fileInfos.stream()
                .filter(file -> file.getFatherId().equals(id))
                .collect(Collectors.toList());
    }

    /**
     * 将文件管理系统中的文件信息转换为KnowledgeFileInfo
     *
     * @param files 文件信息列表
     * @return KnowledgeFileInfo列表
     */
    public List<KnowledgeFileInfo> changeToKnowledgeFile(List<FileInfoFormSystem> files) {
        List<KnowledgeFileInfo> result = new ArrayList<>();
        for (FileInfoFormSystem file : files) {
            KnowledgeFileInfo fileInfo = new KnowledgeFileInfo();
            fileInfo.setFileName(file.getCategory());
            fileInfo.setFileType(Utils.getFileExtension(file.getCategory()));
            fileInfo.setPublic(true);
            fileInfo.setId(file.getFileKey());
            fileInfo.setCreateTime(file.getCreateTime());
            result.add(fileInfo);
        }
        return result;
    }

    /**
     * 根据文件id列表，将个人知识库文件转换为pdf或pptx格式
     *
     * @param tasks 需要转换的文件列表
     */
    public void covertPrivateKnowledgeFiles(List<KnowledgeFileInfo> tasks){
        webClient.post()
                .uri(libreOfficeTasks)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(tasks)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSubscribe(s -> logger.info("开始异步提交文件转换任务，数量：{}", tasks.size()))
                .then(Mono.defer(() -> {
                    logger.info("文件转换完成，开始同步到AI平台");
                    List<String> taskIds = tasks.stream()
                            .map(KnowledgeFileInfo::getId)
                            .collect(Collectors.toList());
                    return aiPlatformSyncService.syncPersonalFile(taskIds);
                }))
                .doOnSuccess(v -> logger.info("文件转换任务已提交"))
                .doOnError(e -> logger.error("提交文件转换任务失败: {}", e.getMessage(), e))
                .subscribe();
    }

    /**
     * 异步调用转换服务完成转换（使用任务执行器，避免同类自调用导致的 @Async 失效）
     *
     * @param tasks 需要转换的文件列表
     */
    public void covertPrivateKnowledgeFilesAsync(List<KnowledgeFileInfo> tasks){
        logger.info("文件转换");
        taskExecutor.execute(() -> covertPrivateKnowledgeFiles(tasks));
    }

    @Async
    public void covertPublicKnowledgeFilesAsync(List<KnowledgeFileInfo> tasks){
        webClient.post()
                .uri(libreOfficeTasks)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(tasks)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSubscribe(s -> logger.info("开始异步提交公共知识库文件转换任务，数量：{}", tasks.size()))
                .then(Mono.defer(() -> {
                    logger.info("公共知识库文件转换完成，开始同步到AI平台");
                    List<String> taskIds = tasks.stream()
                            .map(KnowledgeFileInfo::getId)
                            .collect(Collectors.toList());
                    return aiPlatformSyncService.syncEnterpriseFile(taskIds);
                }))
                .doOnSuccess(v -> logger.info("公共知识库文件转换任务已提交"))
                .doOnError(e -> logger.error("提交文件转换任务失败: {}", e.getMessage(), e))
                .subscribe();
    }

    /**
     * 根据上传者id获取当前用户个人知识库的文件列表
     * @param userId 上传者id
     * @return 文件列表
     */
    public List<KnowledgeFileInfo> findKnowledgeFileByUserId(String userId) {
        return knowledgeFileDao.findPrivateFilesByUploaderId(userId);
    }

    /**
     * 根据上传者id和标签获取当前用户个人知识库的文件列表
     * @param userId 上传者id
     * @param target 领域
     * @return 文件列表
     */
    public List<KnowledgeFileInfo> findKnowledgeFileByUserIdAndTarget(String userId,String target) {
        return knowledgeFileDao.findPrivateFilesByUploaderIdAndFileTarget(userId,target);
    }

    /**
     * 根据路径获取二进制文件。用于个人知识库问答后预览
     * @param path 文件路径
     * @return 二进制文件
     * @throws Exception 异常
     */
    public ResponseEntity<Resource> personalRagFile(String path) throws Exception {
        InputStream fileStream = minioOperations.getFileStream(path);
        // 从路径中提取文件名
        String fileName = path.substring(path.lastIndexOf("/") + 1);
        return Utils.exchangeInputStreamToResource(fileStream, fileName);
    }

    /**
     * 检查用户是否有个人知识库文件
     * @param userId 用户id
     * @return 是否拥有个人知识库文件
     */
    public boolean checkPersonalKnowledgeFile(String userId){
        return !knowledgeFileDao.findPrivateFilesByUploaderId(userId).isEmpty();
    }

    /**
     * 根据文件名获取文件预览链接
     * @param objectName 文件名
     * @return 文件预览链接
     * @throws Exception 异常
     */
    public String getPersonalRagFileUrl(String objectName) throws Exception {
        String result = minioOperations.getDownloadUrl(objectName,3600,null);
        return Utils.exchangeFileUrl(result,local,minioProxy);
    }

    /**
     * 根据知识库文件id获取知识库文件minio路径
     * @param id 知识库文件id
     * @return 知识库文件minio路径
     */
    public String getFileObjectName(String id){
        return knowledgeFileDao.findFileByFileId(id).getStoragePath().replace(bucketName+"/","");
    }

    /**
     * 根据文件名获取文件预览链接,添加到流式问答结果中
     * @param data 流式问答结果
     */
    public void addFileUrlToSource(UnifiedChatRepeat data){
        if ("final_answer".equals(data.getType()) && data.getSources() != null) {
            for (Source source : data.getSources()) {
                try {
                    String url = minioOperations.getDownloadUrl(source.getDocument_title().substring(1), 24*60*60, null);
                    source.setFileUrl(Utils.exchangeFileUrl(url, local, minioProxy));
                } catch (Exception e) {
                    source.setFileUrl(null);
                }
            }
        }
    }

    /**
     * 用户文件重新转换
     */
    public void reconvert(List<KnowledgeFileInfo> files) throws Exception {
        List<KnowledgeFileInfo> filesToReconvert = new ArrayList<>();
        for (KnowledgeFileInfo file : files) {
            // 情况 A: 文件从未被转换过
            if (file.getConvertPath() == null || file.getConvertPath().isEmpty()) {
                filesToReconvert.add(file);
                continue; // 处理下一个文件
            }
            // 情况 B: 文件已转换过，但转换结果不符合新规则
            String originalExtension = getFileExtension(file.getStoragePath());
            String currentConvertedExtension = getFileExtension(file.getConvertPath());
            String expectedConvertedExtension = getExpectedConvertedExtension(originalExtension);
            // 如果预期扩展名存在且与当前转换扩展名不匹配，则需要重新转换
            // 或者如果原始文件有预期转换，但当前转换结果的扩展名无法识别 (currentConvertedExtension == null)
            if (expectedConvertedExtension != null && !expectedConvertedExtension.equals(currentConvertedExtension)) {
                filesToReconvert.add(file);
            }
        }
        // 先将队列中文件的原有转换结果删除
        for (KnowledgeFileInfo file : filesToReconvert) {
            // 只有当存在旧的转换路径时才尝试删除
            if (file.getConvertPath() != null && !file.getConvertPath().isEmpty()) {
                minioOperations.deleteFile(file.getConvertPath());
            }
        }
        // 调用转换函数实现转换（异步）
        covertPrivateKnowledgeFilesAsync(filesToReconvert);
    }

    /**
     * 辅助函数：获取文件扩展名
     */
    private String getFileExtension(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }
        int lastDotIndex = filePath.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == filePath.length() - 1) {
            // 没有点或者点是最后一个字符
            return null;
        }
        // 确保点号后面没有路径分隔符，避免 "/path/to.a/file" 这种误判
        int lastSlashIndex = filePath.lastIndexOf("/");
        if (lastDotIndex < lastSlashIndex) {
            // 点在最后一个斜杠之前，说明点是文件名的一部分而不是扩展名
            return null;
        }
        // 统一转为小写，方便比较
        return filePath.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 辅助函数：根据原始文件扩展名获取预期的转换后扩展名
     */
    private String getExpectedConvertedExtension(String originalExtension) {
        if (originalExtension == null) {
            return null;
        }
        // 从 Map 中获取对应的目标扩展名
        // 如果 Map 中没有对应的规则，则默认转换为pdf
        return CONVERSION_RULES.getOrDefault(originalExtension.toLowerCase(), "pdf");
    }

    /**
     * 根据文件名获取转换后的文件格式
     *
     * @param fileName 文件名
     * @return 转换后的文件格式
     */
    private String getTargetFormat(String fileName) {
        if (fileName.endsWith("ppt") || fileName.endsWith("pptx")) {
            return "pptx";
        } else {
            return "pdf";
        }
    }

    /**
     * 调用文件转换的工具类，返回转换后的二进制文件
     *
     * @param file 需要转换的文件
     * @return 转换后的二进制文件
     * @throws Exception 转换过程中的异常
     */
    private byte[] convert(MultipartFile file) throws Exception {
        String extension = Utils.getFileExtension(Objects.requireNonNull(file.getOriginalFilename()));
        logger.info("文件{}进行格式转换", file.getOriginalFilename());
        if ("ppt".equals(extension)) {
            return fileConverter.convert(file, "pptx");
        } else {
            return fileConverter.convert(file, "pdf");
        }
    }

    /**
     * 文件上传,返回文件路径列表
     *
     * @param files     需要上传的文件列表
     * @param fileNames 上传文件在服务器合法的文件名列表
     * @param target    上传文件所属领域
     * @return 上传文件在服务器的路径列表
     */
    private List<Path> uploadFiles(List<MultipartFile> files, List<String> fileNames, String target, String userId, String folderId, boolean isPublic) { // 添加 folderId 参数
        List<Path> result = new ArrayList<>();
        List<String> fileUploads = new ArrayList<>();
        for (int index = 0; index < files.size(); index++) {
            String name = minioOperations.createKnowledgeFileName(fileNames.get(index), target, userId, folderId, isPublic);
            String contentType = ContentTypeDetector.getContentType(fileNames.get(index));
            try (InputStream inputStream = files.get(index).getInputStream()) {
                minioOperations.uploadFile(name, inputStream, files.get(index).getSize() ,contentType);
                fileUploads.add(name);
                URI uri = URI.create(String.format("%s/%s/%s", endpoint, bucketName, name));
                result.add(Paths.get(uri.getPath()));
            } catch (Exception e) {
                // 发生异常时删除已上传的所有文件
                fileUploadUtils.deleteUploadedFiles(fileUploads);
                logger.info("文件上传过程报错，错误前上传文件已回滚");
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    /**
     * 检查文件是否会重名
     *
     * @param fileNames 上传的文件名
     * @param target    目标领域
     * @param userId    用户id
     * @param isPublic  是否公有
     * @param folderId  文件夹id (新增)
     */
    private void checkFileExist(List<String> fileNames, String target, String userId, boolean isPublic, String folderId) { // 添加 folderId 参数
        logger.info("检查文件是否已经存在");
        if (Utils.checkDuplicateStringInList(fileNames)) {
            throw new DataNotComplianceException("文件名重复");
        }
        List<KnowledgeFileInfo> existFiles;
        if (isPublic) {
            existFiles = knowledgeFileDao.findPublicFilesByFileTargetAndFileNames(target, fileNames);
        } else {
            // 私有文件，按文件夹ID检查重名
            existFiles = knowledgeFileDao.findPrivateFilesByUploaderIdAndFolderIdAndFileNames(userId, folderId, fileNames);
        }
        if (existFiles != null && !existFiles.isEmpty()) {
            throw new DataNotComplianceException("文件名重复");
        }
    }

    /**
     * 根据内容哈希判定文件是否重复
     *
     * @param files    待上传文件数组
     * @param isPublic 是否公有
     * @param userId   上传者id
     * @param target   所属领域
     * @param folderId 文件夹id (新增)
     * @throws IOException 存在重复时以错误形式抛出
     */
    private List<String> checkFileHash(List<MultipartFile> files, boolean isPublic, String userId, String target, String folderId) throws IOException { // 添加 folderId 参数
        List<String> fileHash = new ArrayList<>();
        for (MultipartFile file : files) {
            fileHash.add(Utils.getHash(file));
        }
        if (Utils.checkDuplicateStringInList(fileHash)) {
            throw new DataNotComplianceException("上传文件中有相同内容文件存在");
        }
        List<KnowledgeFileInfo> existHash;
        if (isPublic) {
            existHash = knowledgeFileDao.findPublicFilesByFileTargetAndHashCodes(target, fileHash);
        } else {
            // 私有文件，按文件夹ID检查哈希重复
            existHash = knowledgeFileDao.findPrivateFilesByUploaderIdAndFolderIdAndHashCodes(userId, folderId, fileHash);
        }
        if (existHash != null && !existHash.isEmpty()) {
            throw new DataNotComplianceException("上传文件与已上传文件存在重复");
        }
        return fileHash;
    }

    /**
     * 构建并返回上传文件信息
     *
     * @param files     需要上传的文件列表
     * @param userId    上传用户的id
     * @param filePath  上传文件在服务器的路径列表
     * @param fileNames 上传文件在服务器合法的文件名列表
     * @param target    上传文件所属领域
     * @return 上传文件信息列表
     */
    private List<KnowledgeFileInfo> buildUploadFileInfo(List<MultipartFile> files, List<String> fileHash, String userId, List<Path> filePath, List<String> fileNames, String target, String folderId,boolean isPublic) {
        logger.info("用户{}上传{}个文件,构建并返回上传文件信息", userId, files.size());
        List<KnowledgeFileInfo> result = new ArrayList<>();
        for (int index = 0; index < files.size(); index++) {
            KnowledgeFileInfo fileInfo = new KnowledgeFileInfo();
            fileInfo.setOriginalFileName(files.get(index).getOriginalFilename());
            fileInfo.setFileName(fileNames.get(index));
            fileInfo.setUploaderId(userId);
            fileInfo.setCreateTime(LocalDateTime.now());
            fileInfo.setUpdateTime(LocalDateTime.now());
            fileInfo.setFileTarget(isPublic ? target : "");
            fileInfo.setFolderId(folderId);
            fileInfo.setPublic(isPublic);
            fileInfo.setHashCode(fileHash.get(index));
            fileInfo.setFileType(
                    Utils.getFileExtension(
                            Objects.requireNonNull(
                                    files.get(index)
                                            .getOriginalFilename()
                            )
                    )
            );
            fileInfo.setStoragePath(
                    filePath.get(index)
                            .toString()
                            .replace("\\", "/")
            );
            fileInfo.setFileSize(
                    getFileSizeOnServer(
                            filePath.get(index)
                                    .toString()
                                    .replace("\\", "/")
                                    .replace(bucketName + "/", "")
                    )
            );
            result.add(fileInfo);
        }
        return result;
    }

    /**
     * 获取minio服务器上的文件大小
     *
     * @param path 文件在服务器上的路径
     * @return 文件大小
     */
    private long getFileSizeOnServer(String path) {
        try {
            StatObjectResponse stat = minioOperations.getObjectStat(path);
            return stat.size();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 更新MinIO中的文件路径并移动文件。此方法不保存KnowledgeFileInfo到数据库。
     * @param file 需要更新和移动的文件信息
     * @param newFolderId 目标文件夹ID
     * @throws Exception MinIO操作异常
     */
    public void updateMinioPathsAndMoveFile(KnowledgeFileInfo file, String newFolderId) throws Exception {
        String oldStoragePath = file.getStoragePath();
        String currentFileName = file.getFileName();
        String userId = file.getUploaderId();
        // 构造新的 MinIO 源文件路径
        String newStoragePath = "/" + minioBucketName + "/private/" + userId + "/" + newFolderId + "/" + currentFileName;
        // 获取旧的 MinIO 转换文件路径
        String oldConvertPath = file.getConvertPath();
        try {
            // MinIO操作通常需要的是相对于桶名的对象路径
            String oldObjectPath = oldStoragePath.substring(("/" + minioBucketName + "/").length());
            String newObjectPath = newStoragePath.substring(("/" + minioBucketName + "/").length());
            // 1. 移动源文件
            minioOperations.copyObject(minioBucketName, oldObjectPath, minioBucketName, newObjectPath);
            minioOperations.deleteFile(minioBucketName, oldObjectPath);
            // 更新源文件路径
            file.setStoragePath(newStoragePath);
            // 2. 移动转换后的文件（如果存在）
            if (oldConvertPath != null && !oldConvertPath.isEmpty()) {
                // 提取转换后的文件名
                String convertedFileName = Utils.getFileNameFromPath(oldConvertPath);
                // 构造新的 MinIO 转换文件路径
                String newConvertPath = "/fileConvert" + "/private/" + userId + "/" + newFolderId + "/" + convertedFileName;
                // 检查转换文件是否存在，避免不必要的MinIO操作和错误
                if (minioOperations.objectExists(minioBucketName, oldConvertPath)) {
                    minioOperations.copyObject(minioBucketName, oldConvertPath, minioBucketName, newConvertPath);
                    minioOperations.deleteFile(minioBucketName, oldConvertPath);
                    // 更新转换文件路径
                    file.setConvertPath(newConvertPath);
                } else {
                    logger.warn("Converted file not found in MinIO for old path: {}", oldConvertPath);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to move files in MinIO for old storage path: {}. Error: {}", oldStoragePath, e.getMessage(), e);
            // 重新抛出异常，由调用方处理
            throw e;
        }
    }

    /**
     * 在MinIO中移动文件到默认文件夹并更新storagePath (重构为调用updateMinioPathsAndMoveFile)
     * @param userId 用户ID
     * @param defaultFolderId 默认文件夹ID
     * @param files 需要移动的文件列表
     */
    public void moveFilesToDefaultFolderInMinio(String userId, String defaultFolderId, List<KnowledgeFileInfo> files) {
        for (KnowledgeFileInfo file : files) {
            try {
                // 调用新的通用方法
                updateMinioPathsAndMoveFile(file, defaultFolderId);
            } catch (Exception e) {
                // 批量操作中，单个文件失败时记录日志，并继续处理其他文件
                logger.error("Failed to move a file in batch operation to default folder. File ID: {}, User ID: {}, Default Folder ID: {}. Error: {}",
                             file.getId(), userId, defaultFolderId, e.getMessage(), e);
            }
        }
        // 批量更新所有已移动文件的storagePath和convertPath到数据库
        knowledgeFileRepository.saveAll(files);
    }

    /**
     * 调用第三方服务进行公共知识库文件转换
     * @param knowledgeFileInfos 待转换的文件信息列表
     * @return 转换结果列表
     */
    public List<PublicFileResult> convertFiles(List<KnowledgeFileInfo> knowledgeFileInfos) {
        return webClient.post()
                .uri(libreOfficePublicConvert)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(knowledgeFileInfos)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<PublicFileResult>>() {})
                .block();
    }

    /**
     * 个人文件分享文件到部门文件夹
     * @param userId 用户id
     * @param folderId 目标文件夹id
     * @param fileId 文件id
     */
    public DepartmentFile sharePrivateFileToDepartmentFolder(String userId,String folderId,String fileId) throws Exception {
        //获取文件与文件夹信息
        KnowledgeFileInfo fileInfo = knowledgeFileDao.findByFileId(fileId);
        FolderList folderList = folderListDao.findById(folderId);
        if(fileInfo == null){
            throw new NotFoundException("文件不存在");
        }
        if(folderList == null){
            throw new NotFoundException("文件夹不存在");
        }
        //是否是部门文件夹
        String departmentId = folderList.getDepartmentId();
        if(departmentId == null){
            throw new DataNotComplianceException("不是部门文件夹");
        }
        //文件是否属于用户
        if(!fileInfo.getUploaderId().equals(userId)){
            throw new DataNotComplianceException("文件不属于用户");
        }
        //用户是否属于部门
        if(!permissionService.canOnDepartment(userId,departmentId, KnowledgeFileAction.FILE_UPLOAD)){
            throw new DataNotComplianceException("无权限操作");
        }
        //文件重名校验
        String fileName = fileInfo.getFileName();
        if(!departmentFileDao.findFilesByFolderIdAndFileName(folderId,fileName).isEmpty()){
            throw new DataNotComplianceException("文件名重复");
        }
        //文件hash校验
        String fileHash = fileInfo.getHashCode();
        if(!departmentFileDao.findFilesByFolderIdAndHash(folderId,fileHash).isEmpty()){
            throw new DataNotComplianceException("文件hash重复");
        }
        //移动minio文件并入库
        return moveFileToDepartmentFolder(userId,folderId,fileId);
    }

    /**
     * 移动minio私人文件到部门文件夹
     * @param userId  用户id
     * @param folderId 目标文件夹id
     * @param fileId  文件id
     * @throws Exception 移动文件异常
     */
    private DepartmentFile moveFileToDepartmentFolder(String userId,String folderId,String fileId) throws Exception {
        //原文件信息
        KnowledgeFileInfo fileInfo = knowledgeFileDao.findByFileId(fileId);
        String oldFolderId = fileInfo.getFolderId();
        //目标文件夹信息
        FolderList folderList = folderListDao.findById(folderId);
        String departmentId = folderList.getDepartmentId();
        //原始文件相关信息
        //文件名
        String fileName = fileInfo.getFileName();
        //移动前的对象键
        String oldObjectKey = fileInfo.getStoragePath().replace("/" + bucketName + "/", "");
        //移动后的对象键
        String newObjectKey = minioOperations.createDepartmentFileName(fileName,folderList.getDepartmentId(),folderId);
        //移动原文件
        minioOperations.copyObject(bucketName,oldObjectKey,bucketName,newObjectKey);
        //移动前的转换文件对象键
        String oldConvertObjectKey = fileInfo.getConvertPath();
        //移动后的转换文件对象键
        String newConvertObjectKey = oldConvertObjectKey
                .replace("/private/","/department/")
                .replace("/"+userId+"/","/"+departmentId+"/")
                .replace("/"+oldFolderId+"/","/"+folderId+"/");
        //移动转换文件
        minioOperations.copyObject(bucketName,oldConvertObjectKey,bucketName,newConvertObjectKey);
        //构造部门知识库文件信息入库
        DepartmentFile departmentFile = new DepartmentFile();
        departmentFile.setStoragePath("/" + bucketName + "/" + newObjectKey);
        departmentFile.setConvertPath(newConvertObjectKey);
        departmentFile.setUploaderId(userId);
        departmentFile.setDepartmentId(departmentId);
        departmentFile.setFolderId(folderId);
        departmentFile.setFileName(fileName);
        departmentFile.setOriginalFileName(fileInfo.getOriginalFileName());
        departmentFile.setFileSize(fileInfo.getFileSize());
        departmentFile.setCreateTime(LocalDateTime.now());
        departmentFile.setUpdateTime(LocalDateTime.now());
        departmentFile.setFileType(fileInfo.getFileType());
        departmentFile.setHashCode(fileInfo.getHashCode());
        return departmentFileDao.save(departmentFile);
    }
}
