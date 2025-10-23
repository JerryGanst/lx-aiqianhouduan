package org.example.ai_api.Service;

import io.minio.StatObjectResponse;
import org.example.ai_api.Bean.Entity.DepartmentFile;
import org.example.ai_api.Bean.Entity.UserInfo;
import org.example.ai_api.Bean.Enum.FileHeaderGenerator;
import org.example.ai_api.Bean.Enum.KnowledgeFileAction;
import org.example.ai_api.Exception.BadRequestException;
import org.example.ai_api.Exception.DataNotComplianceException;
import org.example.ai_api.Exception.NotAccessedException;
import org.example.ai_api.Exception.NotFoundException;
import org.example.ai_api.Persistence.Dao.DepartmentFileDao;
import org.example.ai_api.Strategy.KnowledgeFileSort.FileSortContext;
import org.example.ai_api.Strategy.KnowledgeFileSort.FileSortStrategy;
import org.example.ai_api.Utils.ContentTypeDetector;
import org.example.ai_api.Utils.FileUploadUtils;
import org.example.ai_api.Utils.MinioOperations;
import org.example.ai_api.Utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DepartmentFileService {

    private static final Logger logger = LoggerFactory.getLogger(DepartmentFileService.class.getName());

    @Autowired
    private DepartmentFileDao departmentFileDao;
    @Autowired
    private MinioOperations minioOperations;
    @Autowired
    private FileUploadUtils fileUploadUtils;
    @Autowired
    private FileSortContext fileSortContext;
    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private AIPlatformSyncService aiPlatformSyncService;
    @Autowired
    @Qualifier("StreamWebClient")
    private WebClient webClient;
    @Value("${minio.bucketName}")
    private String bucketName;
    @Value("${minio.endpoint}")
    private String endpoint;
    @Value("${libreoffice_departmentFile}")
    private String libreOfficeDepartmentFile;
    @Value("${downloadTimeout}")
    private int downloadTimeout;
    @Value("${minio-proxy}")
    private String minioProxy;
    @Value("${local}")
    private String local;


    /**
     *   保存部门文件
     * @param departmentFiles  部门文件信息
     * @return   保存后的部门文件信息
     */
    public List<DepartmentFile> saveAllDepartmentFile(List<DepartmentFile> departmentFiles) {
        return departmentFileDao.saveAll(departmentFiles);
    }

    /**
     *  上传部门知识库文件
     * @param files 文件列表
     * @param userId  上传者id
     * @param departmentId  部门id
     * @param folderId   文件夹id
     * @return  文件信息列表
     */
    public List<DepartmentFile> uploadDepartmentKnowledgeFile(List<MultipartFile> files, String userId, String departmentId,String folderId) throws Exception {
        //前置检查,返回转换后的在服务器合法的文件名列表
        List<String> fileNames = fileUploadUtils.checkBeforeUpload(files, userId);
        //检查文件是否重复，并获取hash值
        List<String> fileHash = checkHashForDepartmentFile(files, departmentId,folderId);
        //检查是否存在文件重名
        checkFileNameForDepartmentFile(fileNames, departmentId, folderId);
        //文件上传,返回文件路径列表
        List<Path> fileUploads = uploadDepartmentFiles(files, fileNames, departmentId, folderId);
        logger.info("用户{}上传{}个文件到部门{}", userId, files.size(), departmentId);
        //构建并返回上传文件信息
        return buildDepartmentUploadFile(files,fileHash,userId,fileUploads,fileNames,departmentId,folderId);
    }

    /**
     * 删除部门文件
     * @param departmentId  部门id
     * @param fileId    文件id
     * @throws Exception  操作过程报错
     */
    public void departmentFileDelete(String departmentId, String fileId,String userId) throws Exception {
        //前置权限检查
        if (!permissionService.canForDepartmentFile(userId, fileId, KnowledgeFileAction.DELETE)) {
            throw new NotAccessedException("无权限删除该部门文件");
        }
        DepartmentFile departmentFile = departmentFileDao.findById(fileId);
        if (departmentFile == null) {
            throw new NotFoundException("文件不存在");
        }
        UserInfo userInfo = userInfoService.findById(userId);
        if (userInfo == null){
            throw new NotFoundException("用户id" + userId + "不存在");
        }
        String folderId = departmentFile.getFolderId();
        String name = minioOperations.createDepartmentFileName(departmentFile.getFileName(), departmentId,folderId);
        minioOperations.deleteFile(name);
        deleteConvertDepartmentFile(fileId);
        departmentFileDao.deleteById(fileId);
        logger.info("用户{}删除部门{}文件{}", userId, departmentId, fileId);
        //同步删除ai平台文件
        logger.info("同步删除ai平台文件{}",departmentFile.getAiFileId());
        aiPlatformSyncService.deleteFileFromAIPlatform(departmentFile.getAiFileId())
                .doOnSuccess(v -> logger.info("已删除AI平台数据: {}", departmentFile.getAiFileId()))
                .doOnError(e -> logger.error("AI平台数据删除失败: {}", e.getMessage()))
                .subscribe();
    }

    /**
     * 将部门文件在MinIO中移动到指定文件夹，并更新存储/转换路径
     * @param file 部门文件实体
     * @param targetFolderId 目标文件夹ID
     * @throws Exception MinIO操作异常
     */
    public void moveFileToFolder(DepartmentFile file, String targetFolderId) throws Exception {
        if (file == null) {
            throw new NotFoundException("部门文件不存在");
        }
        if (targetFolderId == null || targetFolderId.isEmpty()) {
            throw new IllegalArgumentException("目标文件夹ID不可为空");
        }
        if (targetFolderId.equals(file.getFolderId())) {
            return;
        }
        String departmentId = file.getDepartmentId();
        String fileName = file.getFileName();
        String oldStoragePath = file.getStoragePath();
        if (oldStoragePath == null || oldStoragePath.isEmpty()) {
            throw new NotFoundException("部门文件存储路径不存在");
        }
        String oldObjectPath = extractObjectPath(oldStoragePath);
        String newObjectPath = minioOperations.createDepartmentFileName(fileName, departmentId, targetFolderId);
        try {
            minioOperations.copyObject(bucketName, oldObjectPath, bucketName, newObjectPath);
            minioOperations.deleteFile(bucketName, oldObjectPath);
            file.setStoragePath("/" + bucketName + "/" + newObjectPath);
            file.setFolderId(targetFolderId);
            file.setUpdateTime(LocalDateTime.now());
            String oldConvertPath = file.getConvertPath();
            if (oldConvertPath != null && !oldConvertPath.isEmpty()) {
                String convertedFileName = Utils.getFileNameFromPath(oldConvertPath);
                String newConvertPath = "/fileConvert/department/" + departmentId + "/" + targetFolderId + "/" + convertedFileName;
                String oldConvertObject = extractObjectPath(oldConvertPath);
                String newConvertObject = extractObjectPath(newConvertPath);
                if (!oldConvertObject.isEmpty() && minioOperations.objectExists(bucketName, oldConvertObject)) {
                    minioOperations.copyObject(bucketName, oldConvertObject, bucketName, newConvertObject);
                    minioOperations.deleteFile(bucketName, oldConvertObject);
                    file.setConvertPath(newConvertPath);
                } else {
                    logger.warn("Converted department file not found in MinIO for old path: {}", oldConvertPath);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to move department file {} to folder {}. Error: {}", file.getId(), targetFolderId, e.getMessage(), e);
            throw e;
        }
    }

    public String getDownloadUrl(String userId,String fileId) throws Exception {
        DepartmentFile departmentFile = departmentFileDao.findById(fileId);
        if (departmentFile == null) {
            throw new NotFoundException("文件不存在");
        }
        String departmentId = departmentFile.getDepartmentId();
        if(!permissionService.canOnDepartment(userId,departmentId,KnowledgeFileAction.READ)){
            throw new BadRequestException("不能下载非本部门文件");
        }
        String name = departmentFile.getStoragePath().replace("/" +  bucketName + "/", "");
        Map<String,String> reqParam = Utils.buildFileHeaders(departmentFile.getFileName(), FileHeaderGenerator.DOWNLOAD);
        String url = minioOperations.getDownloadUrl(name,downloadTimeout,reqParam);
        return Utils.exchangeFileUrl(url,local,minioProxy);
    }

    public boolean  isDepartmentFileNameExist(String departmentId, String folderId, String fileName) {
        List<DepartmentFile> existFiles = departmentFileDao.findDepartmentFilesByFolderIdAndDepartmentIdAndFileName(departmentId, folderId, fileName);
        return !existFiles.isEmpty();
    }

    public boolean isDepartmentFileHashExist(String departmentId, String folderId, String hash) {
        List<DepartmentFile> existFiles = departmentFileDao.findDepartmentFilesByFolderIdAndDepartmentIdAndHash(departmentId, folderId, hash);
        return !existFiles.isEmpty();
    }

    private String extractObjectPath(String storagePath) {
        String normalized = storagePath == null ? "" : storagePath.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith(bucketName + "/")) {
            normalized = normalized.substring((bucketName + "/").length());
        }
        return normalized;
    }

    /**
     * 根据文件id列表，将知识库文件转换为pdf或pptx格式
     *
     * @param tasks 需要转换的文件列表
     */
    @Async
    public void covertDepartmentFiles(List<DepartmentFile> tasks){
        webClient.post()
                .uri(libreOfficeDepartmentFile)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(tasks)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSubscribe(s -> logger.info("开始异步提交部门文件转换任务，数量：{}", tasks.size()))
                .then(Mono.defer(() -> {
                    logger.info("部门文件转换完成，开始同步到知识库");
                    List<String> taskIds = tasks.stream()
                            .map(DepartmentFile::getId)
                            .collect(Collectors.toList());
                    return aiPlatformSyncService.syncDepartmentFile(taskIds);
                }))
                .doOnSuccess(v -> logger.info("部门文件转换任务已完成"))
                .doOnError(e -> logger.error("提交部门文件转换任务失败: {}", e.getMessage(), e))
                .subscribe();
    }

    /**
     * 删除文件时，同步删除转换后的文件
     *
     * @param fileId 文件id
     * @throws Exception 操作过程报错
     */
    public void deleteConvertDepartmentFile(String fileId) throws Exception {
        logger.info("删除部门文件{}转换后的文件", fileId);
        DepartmentFile info = departmentFileDao.findById(fileId);
        if(info.getConvertPath()!=null&&!info.getConvertPath().isEmpty()){
            minioOperations.deleteFile(info.getConvertPath());
        }
        logger.info("部门文件{}转换已删除", fileId);
    }

    /**
     * 部门文件排序
     * @param departmentFiles  部门文件列表
     * @param type    排序类型
     * @param increase  是否升序
     * @return    排序后的部门文件列表
     */
    public List<DepartmentFile> sortDepartmentFiles(List<DepartmentFile> departmentFiles,String type,boolean increase){
        FileSortStrategy  strategy = fileSortContext.getStrategy(type);
        List<DepartmentFile>  result;
        if (strategy == null){
            result = departmentFiles;
        }else {
            result = strategy.sortDepartmentFile(departmentFiles);
            if(!increase){
                Collections.reverse(result);
            }
        }
        return result;
    }

    /**
     *  上传部门文件到服务器
     * @param files  文件列表
     * @param fileNames  文件名列表
     * @param departmentId   上传部门id
     * @param folderId   上传文件夹id
     * @return   上传文件路径列表
     */
    private List<Path> uploadDepartmentFiles(List<MultipartFile> files, List<String>  fileNames, String departmentId,String folderId) {
        List<Path> result = new ArrayList<>();
        List<String> fileUploads = new ArrayList<>();
        for (int index = 0; index < files.size(); index++) {
            String name = minioOperations.createDepartmentFileName(fileNames.get(index), departmentId,folderId);
            String contentType = ContentTypeDetector.getContentType(fileNames.get(index));
            try (InputStream inputStream = files.get(index).getInputStream()) {
                minioOperations.uploadFile(name, inputStream, files.get(index).getSize() ,contentType);
                fileUploads.add(name);
                URI uri = URI.create(String.format("%s/%s/%s", endpoint, bucketName, name));
                result.add(Paths.get(uri.getPath()));
            } catch (Exception e) {
                // 发生异常时删除已上传的所有文件
                fileUploadUtils.deleteUploadedFiles(fileUploads);
                logger.info("部门文件上传过程报错，错误前上传文件已回滚");
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    /**
     * 检查文件hash值是否重复
     * @param files  文件列表
     * @param departmentId   上传部门id
     * @param folderId     上传文件夹id
     * @return    文件hash值列表
     * @throws IOException   文件读取异常
     */
    private List<String> checkHashForDepartmentFile(List<MultipartFile> files,String departmentId,String folderId) throws IOException {
        List<String> fileHash = new ArrayList<>();
        for (MultipartFile file : files) {
            fileHash.add(Utils.getHash(file));
        }
        if (Utils.checkDuplicateStringInList(fileHash)) {
            throw new DataNotComplianceException("上传文件中有相同内容文件存在");
        }
        List<DepartmentFile> existHash;
        existHash = departmentFileDao.findDepartmentFilesByFolderIdAndDepartmentIdAndHashCodes(departmentId, folderId, fileHash);
        if (existHash != null && !existHash.isEmpty()) {
            throw new DataNotComplianceException("上传文件与已上传文件存在重复");
        }
        return fileHash;
    }

    /**
     *  检查文件名是否重复
     * @param fileNames    文件名列表
     * @param departmentId   上传部门id
     * @param folderId      上传文件夹id
     */
    private void checkFileNameForDepartmentFile(List<String> fileNames, String departmentId, String folderId) {
        logger.info("检查部门文件是否已经存在");
        if (Utils.checkDuplicateStringInList(fileNames)) {
            throw new DataNotComplianceException("文件名重复");
        }
        List<DepartmentFile> existFiles;
        existFiles = departmentFileDao.findDepartmentFilesByFolderIdAndDepartmentIdAndFileNames(departmentId, fileNames, folderId);
        if (existFiles != null && !existFiles.isEmpty()) {
            throw new DataNotComplianceException("文件名重复");
        }
    }


    /**
     * 构建并返回上传文件信息
     *
     * @param files     需要上传的文件列表
     * @param userId    上传用户的id
     * @param filePath  上传文件在服务器的路径列表
     * @param fileNames 上传文件在服务器合法的文件名列表
     * @param departmentId    上传文件所属部门
     * @param folderId      上传文件所属文件夹
     * @return 上传文件信息列表
     */
    private List<DepartmentFile> buildDepartmentUploadFile(List<MultipartFile> files, List<String> fileHash, String userId, List<Path> filePath, List<String> fileNames,String departmentId,String folderId) {
        logger.info("部门{}上传{}个文件,构建并返回上传文件信息", departmentId, files.size());
        List<DepartmentFile> result = new ArrayList<>();
        for (int index = 0; index < files.size(); index++) {
            DepartmentFile fileInfo = new DepartmentFile();
            fileInfo.setOriginalFileName(files.get(index).getOriginalFilename());
            fileInfo.setFileName(fileNames.get(index));
            fileInfo.setUploaderId(userId);
            fileInfo.setCreateTime(LocalDateTime.now());
            fileInfo.setUpdateTime(LocalDateTime.now());
            fileInfo.setDepartmentId(departmentId);
            fileInfo.setFolderId(folderId);
            fileInfo.setHashCode(fileHash.get(index));
            fileInfo.setFileType(
                    Utils.getFileExtension(
                            Objects.requireNonNull(
                                    files.get(index)
                                            .getOriginalFilename()
                            )
                    )
            );
            fileInfo.setFileSize(
                    getFileSizeOnServer(
                            filePath.get(index)
                                    .toString()
                                    .replace("\\", "/")
                                    .replace(bucketName + "/", "")
                    )
            );
            fileInfo.setStoragePath(
                    filePath.get(index)
                            .toString()
                            .replace("\\", "/")
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

}
