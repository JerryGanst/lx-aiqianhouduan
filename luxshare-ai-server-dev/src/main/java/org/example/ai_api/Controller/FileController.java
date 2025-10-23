package org.example.ai_api.Controller;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.example.ai_api.Annotation.FunctionCount;
import org.example.ai_api.Bean.Entity.DepartmentFile;
import org.example.ai_api.Bean.Entity.FileInfo;
import org.example.ai_api.Bean.Entity.KnowledgeFileInfo;
import org.example.ai_api.Bean.Entity.UserPermission;
import org.example.ai_api.Bean.Enum.KnowledgeFileAction;
import org.example.ai_api.Bean.Enum.KnowledgeFileUpload;
import org.example.ai_api.Bean.Model.FileId;
import org.example.ai_api.Bean.Model.FileInfoFormSystem;
import org.example.ai_api.Bean.Model.ResultData;
import org.example.ai_api.Bean.WebRequest.*;
import org.example.ai_api.Exception.BadRequestException;
import org.example.ai_api.Exception.NotAccessedException;
import org.example.ai_api.Exception.NotFoundException;
import org.example.ai_api.Persistence.Dao.DepartmentFileDao;
import org.example.ai_api.Persistence.Repository.KnowledgeFileRepository;
import org.example.ai_api.Service.*;
import org.example.ai_api.Strategy.KnowledgeBaseUpload.KnowledgeUploadStrategy;
import org.example.ai_api.Strategy.KnowledgeBaseUpload.UploadContext;
import org.example.ai_api.Utils.Utils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.ByteArrayResource;

import java.util.stream.Collectors;

import org.slf4j.Logger;

/**
 * 文件信息管理接口.
 *
 * @author 10353965
 */
@RestController
@RequestMapping("/Files")
public class FileController {
    private static final Logger logger = LoggerFactory.getLogger(FileController.class.getName());
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "ppt", "pptx", "doc", "docx", "pdf", "txt", "xls", "xlsx"
    ));
    @Value("${systemFiles-view}")
    private String fileLink;
    @Autowired
    private UploadContext uploadContext;
    @Autowired
    private FileService fileService;
    @Autowired
    private FileUploadInfoService fileUploadInfoService;
    @Autowired
    private UserPermissionService userPermissionService;
    @Autowired
    private SystemFileService systemFileService;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private FileFolderService fileFolderService;
    @Autowired
    private DepartmentFileService departmentFileService;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private KnowledgeMultipartService knowledgeMultipartService;
    @Autowired
    private AIPlatformSyncService aiPlatformSyncService;
    @Value("${libreoffice_convert}")
    private String libreOfficeConvertUrl;
    @Value("${libreoffice_ready}")
    private String libreOfficeReadyUrl;
    @Autowired
    private DepartmentFileDao departmentFileDao;

    /**
     * 根据文件名获得文件链接.
     *
     * @param requestFileInfo the request file info
     * @return the file link by name
     */
    @PostMapping("/getFileInfoByName")
    @ResponseBody
    public ResultData<FileInfo> getFileLinkByName(@RequestBody FileInfo requestFileInfo) {
        logger.info("getFileLinkByName{}", requestFileInfo.getFileName());
        FileInfo fileInfo = fileService.findByFileName(requestFileInfo.getFileName());
        return ResultData.success(fileInfo);
    }

    /**
     * 根据id获得文件文本
     *
     * @param id 文件id
     * @return 文件文本
     * @throws Exception 异常
     */
    @PostMapping("/getContentById")
    @ResponseBody
    public ResultData<String> getContentById(@RequestParam("id") String id) throws Exception {
        logger.info("getContentById{}", id);
        String content = fileUploadInfoService.getContentById(id);
        logger.info("getContentById{}", content);
        return ResultData.success("获取成功", content);
    }

    /**
     * 根据id获得二进制文件.
     *
     * @param id 文件id
     * @return 根据文件信息构造的二进制文件
     * @throws Exception 异常
     */
    @PostMapping("/getFileById")
    public ResponseEntity<Resource> getFileById(@RequestBody FileId id) throws Exception {
        logger.info("getFileById{}", id);
        try {
            return fileUploadInfoService.getFile(id.getFileId());
        }catch (NotFoundException e){
            return fileService.getKnowledgeFileById(id.getFileId());
        }
    }

    /**
     * 根据id获得二进制知识库文件.
     *
     * @param id 文件id
     * @return 根据文件信息构造的二进制文件
     * @throws Exception 异常
     */
    @PostMapping("/knowledgeFileById")
    public ResponseEntity<Resource> knowledgeFileById(@RequestParam("id") String id) throws Exception {
        logger.info("getKnowledgeFileById{}", id);
        return fileService.getKnowledgeFileById(id);
    }

    /**
     * 个人知识库问答文件预览
     *
     * @param path 文件路径
     * @return 二进制预览文件
     * @throws Exception 异常
     */
    @PostMapping("/personalRagFile")
    public ResponseEntity<Resource> personalRagFile(@RequestParam("path") String path) throws Exception {
        logger.info("personalRagFile:{}", path);
        return fileService.personalRagFile(path);
    }

    /**
     * 知识库文件上传(策略模式区分上传方式)
     * @param files  上传文件列表
     * @param userId  上传者id
     * @param target 上传文件所属知识库领域
     * @param folderId  上传文件所属文件夹id
     * @param departmentId   上传文件所属部门id
     * @param type   上传文件类型
     * @return  上传后的文件信息列表
     * @throws Exception  异常
     */
    @PostMapping("/knowledgeFileUpload")
    @FunctionCount("知识库文件上传")
    public ResultData<Void> uploadTest(
            @RequestPart("file") List<MultipartFile> files,
            @RequestParam("userId") String userId,
            @RequestParam("target") String target,
            @RequestParam("folderId") String folderId,
            @RequestParam(value = "departmentId",required = false) String departmentId,
            @RequestParam(value = "subFolderId", required = false) String subFolderId,
            @RequestParam("type") KnowledgeFileUpload type
    ) throws Exception {
        logger.info("知识库文件上传，userId:{},type:{}", userId, type.getType());
        //前置检查文件格式
        Utils.validateUploadExtensions(files, ALLOWED_EXTENSIONS);
        KnowledgeUploadStrategy strategy = uploadContext.getStrategy(type);
        FileUpload fileUpload = new FileUpload(userId,target,folderId,departmentId,type,subFolderId);
        fileUpload.setSubFolderId(subFolderId);
        strategy.upload(files, fileUpload);
        return ResultData.success("上传成功");
    }

    /**
     *  部门知识库文件删除
     * @param fileId  文件id
     * @param userId   上传者id
     * @param departmentId   上传文件所属部门id
     * @return  删除结果
     * @throws Exception  无操作权限或代码运行错误时抛出异常
     */
    @PostMapping("/departmentFileDelete")
    public ResultData<String> departmentFileDelete(@RequestParam("fileId") String fileId, @RequestParam("userId") String userId, @RequestParam("departmentId") String departmentId) throws Exception {
        logger.info("删除部门知识库文件，fileId:{},userId:{},departmentId:{}", fileId, userId, departmentId);
        departmentFileService.departmentFileDelete(departmentId,fileId,userId);
        return ResultData.success("删除成功");
    }

    /**
     * 知识库文件删除
     *
     * @param knowledgeBase 相关请求体
     * @return 删除结果
     * @throws Exception 无操作权限或代码运行错误时抛出异常
     */
    @PostMapping("/knowledgeFileDelete")
    public ResultData<String> delete(@RequestBody KnowledgeBase<String> knowledgeBase) throws Exception {
        String userId = knowledgeBase.getUserId();
        String target = knowledgeBase.getTarget();
        boolean isPublic = knowledgeBase.isPublic();
        List<String> fileIds = knowledgeBase.getFile();
        logger.info("delete:{}", userId);
        //前置权限检查
        if (isPublic) {
            //权限检查
            if (!userPermissionService.checkUserPermission(userId, target).isDelete()) {
                throw new NotAccessedException("无权限访问该领域");
            }
        }
        if (!isPublic) {
            for (String fileId : fileIds) {
                fileService.knowledgeFileDelete(fileId, userId);
            }
        } else {
            for (String fileId : fileIds) {
                systemFileService.publicFileDelete(fileId, userId, target);
            }
        }
        return ResultData.success("删除成功");
    }

    /**
     * 权限检查
     *
     * @param userId 用户id
     * @return 用户有哪些领域的权限
     */
    @PostMapping("/permissionCheck")
    public ResultData<List<UserPermission>> permissionCheck(@RequestParam("userId") String userId) {
        logger.info("permissionCheck:{}", userId);
        List<UserPermission> permissionList = userPermissionService.getUserPermissionListByUserId(userId);
        //法务知识库先隐藏
        permissionList.removeIf(userPermission -> userPermission.getTarget().equals("Law"));
        return ResultData.success("操作成功", permissionList);
    }

    /**
     * 根据用户id获得文件信息列表.
     *
     * @param knowledgeBase 请求结构体
     * @return 文件信息列表
     */
    @PostMapping("/getFileListByUserId")
    public ResultData<Page<KnowledgeFileInfo>> getFileListByUserId(@RequestBody KnowledgeBase<Void> knowledgeBase) {
        logger.info("getFileListByUserId:{}", knowledgeBase);
        List<KnowledgeFileInfo> fileInfos = fileService.searchFile(knowledgeBase.getKeywords(), knowledgeBase.getTarget(), knowledgeBase.getUserId(), knowledgeBase.isPublic());
        if (knowledgeBase.getSortType() != null) {
            fileInfos = fileService.sortFileList(fileInfos, knowledgeBase.getSortType(), knowledgeBase.isIncrease());
        }
        return ResultData.success("操作成功", Utils.getFilesPage(knowledgeBase.getPage() - 1, knowledgeBase.getPageSize(), fileInfos));
    }

    /**
     * 根据需求排序文件列表.
     *
     * @param fileList 待排序文件列表
     * @param sortType 排序方式
     * @return 排序处理后的列表
     */
    @PostMapping("/sortFileList")
    public ResultData<List<KnowledgeFileInfo>> sortFileList(@RequestParam("fileList") List<KnowledgeFileInfo> fileList, @RequestParam("sortType") String sortType) {
        logger.info("sortType:{}", sortType);
        fileList = fileService.sortFileList(fileList, sortType, true);
        return ResultData.success("操作成功", fileList);
    }

    /**
     * 搜索文件信息
     *
     * @param userId   用户id
     * @param target   目标领域
     * @param keyword  搜索关键字
     * @param isPublic 是否公开
     * @return 搜索结果
     */
    @PostMapping("/searchFile")
    public ResultData<List<KnowledgeFileInfo>> searchFile(@RequestParam("userId") String userId, @RequestParam("target") String target, @RequestParam("keyword") String keyword, @RequestParam("isPublic") boolean isPublic) {
        logger.info("searchFile:{}", keyword);
        List<KnowledgeFileInfo> fileInfos = fileService.searchFile(keyword, target, userId, isPublic);
        return ResultData.success("操作成功", fileInfos);
    }

    /**
     * 获取知识库文件下载链接(三分钟有效)
     *
     * @param userId 用户名
     * @param fileId 文件id
     * @return 下载的url
     * @throws Exception 异常
     */
    @PostMapping("/getDownloadUrl")
    public ResultData<String> getDownloadUrl(@RequestParam("userId") String userId, @RequestParam("fileId") String fileId) throws Exception {
        if (userId == null || fileId == null) {
            throw new BadRequestException("用户id或文件id不可为空");
        }
        fileService.checkUserPermissionForFile(fileId, userId);
        String downloadUrl = fileService.getDownloadUrl(fileId);
        return ResultData.success("操作成功", downloadUrl);
    }

    /**
     * 获取部门知识库文件下载链接(三分钟有效)
     * @param userId  用户id
     * @param fileId  文件id
     * @return  下载的url
     * @throws Exception    异常
     */
    @PostMapping("/getDepartmentDownloadUrl")
    public ResultData<String> getDepartmentDownloadUrl(@RequestParam("userId") String userId,@RequestParam("fileId")String fileId) throws Exception {
        if(userId == null || fileId == null){
            throw new BadRequestException("用户id或文件id不可为空");
        }
        if (!permissionService.canForDepartmentFile(userId, fileId, KnowledgeFileAction.READ)) {
            throw new NotAccessedException("无权限下载该部门文件");
        }
        String downloadUrl = departmentFileService.getDownloadUrl(userId,fileId);
        return  ResultData.success("操作成功",downloadUrl);
    }

    /**
     * 获取临时文件下载链接 (三分钟有效)
     * @param fileId 文件id
     * @return 下载的url
     */
    @PostMapping("getDownloadUrlFromTemp")
    public ResultData<String> getDownloadUrlFromTemp(@RequestParam("fileId") String fileId) throws Exception {
        if (fileId == null) {
            throw new BadRequestException("文件id不可为空");
        }
        String downloadUrl = fileUploadInfoService.getDownloadUrlFromTemp(fileId);
        return ResultData.success("操作成功", downloadUrl);
    }

    /**
     * 获取文件信息列表(从文件管理系统中获取)
     * @param userId 用户id
     * @param target 目标领域
     * @return 文件信息列表
     * @throws Exception 异常
     */
    @PostMapping("/getFileInfoFromSystem")
    public ResultData<Page<KnowledgeFileInfo>> getFileInfoFromSystem(
            @RequestParam("userId") String userId,
            @RequestParam("target") String target,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "keyword", defaultValue = "") String keyword
    ) throws Exception {
        logger.info("用户{}查看知识库{}", userId, target);
        List<FileInfoFormSystem> fileInfo = fileService.getFileByTarget(target);
        List<KnowledgeFileInfo> knowledgeFileInfos = fileService.changeToKnowledgeFile(fileInfo);
        List<KnowledgeFileInfo> result;
        if ("".equals(keyword)) {
            result = knowledgeFileInfos;
        } else {
            result = knowledgeFileInfos.stream()
                    .filter(fileInfoFormSystem -> fileInfoFormSystem.getFileName().contains(keyword))
                    .collect(Collectors.toList());
        }
        result = result.stream()
                .sorted(Comparator.comparing(KnowledgeFileInfo::getCreateTime).reversed())
                .collect(Collectors.toList());
        logger.info("文件列表长度:{}", result.size());
        return ResultData.success("操作成功", Utils.getFilesPage(page - 1, size, result));
    }

    /**
     * 刷新文件列表缓存
     *
     * @return 操作结果
     */
    @PostMapping("/refreshSystemFileListCache")
    public ResultData<String> refreshSystemFileListCache() {
        cacheService.refreshSystemFileListCache();
        return ResultData.success("操作成功");
    }

    /**
     * 获取文件系统预览连接
     *
     * @param fileName 文件名
     * @param target   所属领域
     * @return 文件预览连接
     * @throws Exception 异常
     */
    @PostMapping("/getFileLinkByName")
    public ResultData<String> getFileLink(@RequestParam("fileName") String fileName, @RequestParam("target") String target) throws Exception {
        if (fileName == null || target == null) {
            throw new BadRequestException("文件名或目标领域不可为空");
        }
        List<FileInfoFormSystem> fileInfoFormSystems = fileService.getFileByTarget(target);
        for (FileInfoFormSystem fileInfoFormSystem : fileInfoFormSystems) {
            String name = Utils.removeFileExtension(fileInfoFormSystem.getCategory());
            if (fileName.equals(name)) {
                return ResultData.success("操作成功", fileLink + fileInfoFormSystem.getFileKey());
            }
        }
        throw new BadRequestException("文件名不存在");
    }

    /**
     * 根据用户id将当前用户个人知识库的文件进行格式转换
     * @param userId 用户id
     * @return 操作结果
     */
    @PostMapping("/convertAllFiles")
    public ResultData<Void> convertAllFiles(@RequestParam("userId") String userId) {
        List<KnowledgeFileInfo> files = fileService.findKnowledgeFileByUserId(userId);
        List<KnowledgeFileInfo> result = files.stream()
                        .filter(file -> file.getConvertPath() == null)
                        .collect(Collectors.toList());
        if (result.isEmpty()) {
            return ResultData.success("操作成功");
        }
        fileService.covertPrivateKnowledgeFiles(files);
        return ResultData.success("操作成功");
    }

    /**
     * 根据标签与id获得对应文件列表
     * @param userId 用户id
     * @param tag 标签
     * @return 文件列表
     */
    @PostMapping("/getPersonalFilesByTag")
    public ResultData<List<KnowledgeFileInfo>> getPersonalFilesByTag(@RequestParam("userId") String userId, @RequestParam("tag") String tag) {
        List<KnowledgeFileInfo> files = fileService.findKnowledgeFileByUserIdAndTarget(userId, tag);
        return ResultData.success("操作成功", files);
    }

    /**
     * 根据个人知识库文件名获得预览链接
     * @param objectName 文件名
     * @return 预览链接
     * @throws Exception 异常
     */
    @PostMapping("/getKnowledgeFileUrl")
    public ResultData<String> getKnowledgeFileUrl(@RequestParam("objectName") String objectName) throws Exception {
        logger.info("getKnowledgeFileUrl:{}", objectName);
        if (objectName == null) {
            throw new BadRequestException("文件名不可为空");
        }
        String url = fileService.getPersonalRagFileUrl(objectName);
        return ResultData.success("操作成功", url);
    }

    /**
     * 将文件移动到目标文件夹
     * @param request 移动文件请求，包含目标文件夹ID,用户ID和文件ID
     * @return 移动后的文件信息
     * @throws Exception 异常
     */
    @PostMapping("/moveFileToFolder")
    public ResultData<Void> moveFileToFolder(@RequestBody FileMove request) throws Exception {
        fileFolderService.moveFileToFolder(request.getFileId(), request.getTargetFolderId(), request.getUserId(), request.getDepartmentId());
        return ResultData.success("操作成功");
    }
    /**
     * 测试：调用第三方转换接口并将结果保存到桌面
     * 请求方式：multipart/form-data，字段：
     * - file: 待转换文件
     * - format: 目标格式（默认 pdf）
     */
    @PostMapping("/testConvertToDesktop")
    public ResultData<String> testConvertToDesktop(
            @RequestParam(name = "format", defaultValue = "pdf") String targetFormatExt,
            @RequestPart("file") MultipartFile inputMultipartFile
    ) throws Exception {
        if (inputMultipartFile == null || inputMultipartFile.isEmpty()) {
            throw new BadRequestException("请上传文件");
        }
        String originalName = Objects.requireNonNull(inputMultipartFile.getOriginalFilename(), "文件名不能为空");

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource fileResource = new ByteArrayResource(inputMultipartFile.getBytes()) {
                @Override
                public String getFilename() {
                    return originalName; // 保留原始文件名及扩展名，便于第三方识别
                }
            };
            body.add("file", fileResource);
            body.add("format", targetFormatExt);

            byte[] converted = Utils.LibreOfficeFileConverter(body, restTemplate, libreOfficeReadyUrl, libreOfficeConvertUrl, logger);
            if (converted == null || converted.length == 0) {
                throw new BadRequestException("第三方转换返回空内容");
            }

            String userHome = System.getProperty("user.home");
            Path desktop = Paths.get(userHome, "Desktop");
            if (!Files.exists(desktop)) {
                Files.createDirectories(desktop);
            }
            String baseName = originalName.contains(".") ? originalName.substring(0, originalName.lastIndexOf('.')) : originalName;
            String outName = baseName + "." + targetFormatExt;
            Path outPath = desktop.resolve(outName);
            Files.write(outPath, converted);

            return ResultData.success("转换成功", outPath.toString());
        } catch (Exception e) {
            logger.error("测试转换失败", e);
            throw e;
        }
    }

    /**
     * 将个人知识库文件分享到部门文件夹
     * @param userId  用户id
     * @param fileId  文件id
     * @param folderId  目标文件夹id
     * @return  操作结果
     * @throws Exception  异常
     */
    @PostMapping("/sharePrivateFileToDepartment")
    public ResultData<String>  sharePrivateFileToDepartment(@RequestParam("userId") String userId, @RequestParam("fileId") String fileId, @RequestParam("folderId") String folderId) throws Exception {
        if (fileId == null ||fileId.isEmpty()){
            throw new BadRequestException("文件id不可为空");
        }
        if (folderId == null ||folderId.isEmpty()){
            throw new BadRequestException("文件夹id不可为空");
        }
        if (userId == null ||userId.isEmpty()){
            throw new BadRequestException("用户id不可为空");
        }
        DepartmentFile departmentFile = fileService.sharePrivateFileToDepartmentFolder(userId,folderId,fileId);
        //同步消息到ai平台
        logger.info("分享个人文件到部门，同步消息到ai平台");
        List<String> fileIds = new ArrayList<>();
        fileIds.add(departmentFile.getId());
        aiPlatformSyncService.syncDepartmentFile(fileIds)
                .doOnSuccess(v->logger.info("同步文件到ai平台成功 {}",fileIds))
                .doOnError(e->logger.error("同步文件到ai平台失败 {}",e.getMessage()))
                .subscribe();
        return ResultData.success("操作成功");
    }

    /**
     * 知识库-分片上传：准备
     */
    @PostMapping("/knowledge/multipart/prepare")
    public ResultData<MultipartPrepareResponse> knowledgeMultipartPrepare(@RequestBody KnowledgeMultipartPrepareRequest request) throws Exception {
        logger.info("knowledgeMultipartPrepare filename={}, size={}, type={}", request.getFilename(), request.getSize(), request.getUploadType());
        return ResultData.success("准备成功", knowledgeMultipartService.prepare(request));
    }

    /**
     * 知识库-分片上传：完成
     */
    @PostMapping("/knowledge/multipart/complete")
    public ResultData<Object> knowledgeMultipartComplete(@RequestBody KnowledgeMultipartCompleteRequest request) throws Exception {
        logger.info("knowledgeMultipartComplete objectKey={}, type={}", request.getObjectKey(), request.getUploadType());
        Object saved = knowledgeMultipartService.complete(request);
        return ResultData.success("上传成功", saved);
    }

    /**
     * 知识库-分片上传：取消
     */
    @PostMapping("/knowledge/multipart/abort")
    public ResultData<String> knowledgeMultipartAbort(@RequestBody MultipartAbortRequest request) {
        logger.info("knowledgeMultipartAbort objectKey={}, uploadId={}", request.getObjectKey(), request.getUploadId());
        String msg = knowledgeMultipartService.abort(request);
        return ResultData.success(msg);
    }
}
