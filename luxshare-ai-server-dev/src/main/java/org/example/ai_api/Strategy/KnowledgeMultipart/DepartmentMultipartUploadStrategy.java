package org.example.ai_api.Strategy.KnowledgeMultipart;

import org.example.ai_api.Bean.Entity.DepartmentFile;
import org.example.ai_api.Bean.Entity.FolderList;
import org.example.ai_api.Bean.Enum.KnowledgeFileAction;
import org.example.ai_api.Bean.Enum.KnowledgeFileUpload;
import org.example.ai_api.Bean.WebRequest.KnowledgeMultipartCompleteRequest;
import org.example.ai_api.Bean.WebRequest.KnowledgeMultipartPrepareRequest;
import org.example.ai_api.Bean.WebRequest.MultipartCompletedPart;
import org.example.ai_api.Exception.BadRequestException;
import org.example.ai_api.Persistence.Dao.FolderListDao;
import org.example.ai_api.Service.DepartmentFileService;
import org.example.ai_api.Service.PermissionService;
import org.example.ai_api.Strategy.KnowledgeMultipart.Commons.FileMetaDataBuilder;
import org.example.ai_api.Strategy.KnowledgeMultipart.Commons.ValidationHelpers;
import org.example.ai_api.Utils.MinioOperations;
import org.example.ai_api.Utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class DepartmentMultipartUploadStrategy implements KnowledgeMultipartStrategy {
    @Autowired
    private MinioOperations minioOperations;
    @Autowired
    private DepartmentFileService departmentFileService;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private FolderListDao folderListDao;
    @Autowired
    private FileMetaDataBuilder fileMetaDataBuilder;

    @Value("${minio.bucketName}")
    private String bucketName;
    @Value("${minio.endpoint}")
    private String endpoint;

    @Override
    public boolean supports(KnowledgeFileUpload type) {
        return type == KnowledgeFileUpload.DepartmentType;
    }

    @Override
    public void validatePrepare(KnowledgeMultipartPrepareRequest req) {
        //参数非空校验
        String userId = req.getUserId();
        String departmentId = req.getDepartmentId();
        String folderId = req.getFolderId();
        String filename = req.getFilename();
        String hash = req.getHash();
        ValidationHelpers.requireNonEmpty(userId, "用户id");
        ValidationHelpers.requireNonEmpty(departmentId, "部门id");
        ValidationHelpers.requireNonEmpty(folderId, "文件夹id");
        ValidationHelpers.requireNonEmpty(filename, "文件名");
        ValidationHelpers.requireNonEmpty(hash, "文件hash");
        //校验文件夹是否存在
        FolderList folderList = folderListDao.findById(folderId);
        if (folderList == null) {
            throw new BadRequestException("文件夹不存在");
        }
        //校验文件夹是否为部门文件夹
        if (!folderList.isPublic()) {
            throw new BadRequestException("所选文件夹不是部门文件夹");
        }
        //用户是否属于该部门
        if (!permissionService.canOnDepartment(userId,departmentId, KnowledgeFileAction.FILE_UPLOAD)){
            throw new BadRequestException("无权限操作");
        }
        //文件是否重名
        String normalizedName = Utils.renameFileToUbuntu(filename);
        if (departmentFileService.isDepartmentFileNameExist(departmentId, folderId, normalizedName)){
            throw new BadRequestException("文件名重复");
        }
        //文件是否重复
        if (departmentFileService.isDepartmentFileHashExist(departmentId, folderId, hash)){
            throw new BadRequestException("文件已存在");
        }
    }

    @Override
    public void validateComplete(KnowledgeMultipartCompleteRequest req) {
        //参数非空校验
        String uploadId = req.getUploadId();
        String userId = req.getUserId();
        String departmentId = req.getDepartmentId();
        String folderId = req.getFolderId();
        String objectKey = req.getObjectKey();
        String originalFilename = req.getOriginalFilename();
        String hash = req.getHash();
        List<MultipartCompletedPart> parts = req.getParts();
        ValidationHelpers.requireNonEmpty(uploadId,"上传id");
        ValidationHelpers.requireNonEmpty(userId, "用户id");
        ValidationHelpers.requireNonEmpty(departmentId, "部门id");
        ValidationHelpers.requireNonEmpty(folderId, "文件夹id");
        ValidationHelpers.requireNonEmpty(objectKey, "minio对象键");
        ValidationHelpers.requireNonEmpty(originalFilename, "文件名");
        ValidationHelpers.requireNonEmpty(hash, "文件哈希");
        ValidationHelpers.requirePartsNotEmpty(parts);
    }

    @Override
    public String buildObjectKey(KnowledgeMultipartPrepareRequest req) throws Exception {
        String normalizedName = Utils.renameFileToUbuntu(req.getFilename());
        return minioOperations.createDepartmentFileName(normalizedName, req.getDepartmentId(), req.getFolderId());
    }

    @Override
    public Object registerComplete(KnowledgeMultipartCompleteRequest request) throws Exception {
        String objectKey = request.getObjectKey();
        long sizeOnServer = minioOperations.getObjectStat(objectKey).size();

        DepartmentFile departmentFile = fileMetaDataBuilder.buildDepartmentFile(request,sizeOnServer);
        List<DepartmentFile> saved = departmentFileService.saveAllDepartmentFile(Collections.singletonList(departmentFile));
        departmentFileService.covertDepartmentFiles(saved);
        return saved.get(0);
    }

}

