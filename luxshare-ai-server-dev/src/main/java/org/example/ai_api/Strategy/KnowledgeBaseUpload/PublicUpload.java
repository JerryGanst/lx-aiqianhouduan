package org.example.ai_api.Strategy.KnowledgeBaseUpload;

import org.example.ai_api.Bean.Entity.KnowledgeFileInfo;
import org.example.ai_api.Bean.Enum.KnowledgeFileUpload;
import org.example.ai_api.Bean.Model.FileInfoFormSystem;
import org.example.ai_api.Bean.WebRequest.FileUpload;
import org.example.ai_api.Exception.BadRequestException;
import org.example.ai_api.Exception.NotAccessedException;
import org.example.ai_api.Service.FileService;
import org.example.ai_api.Service.SystemFileService;
import org.example.ai_api.Service.UserPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PublicUpload implements  KnowledgeUploadStrategy {

    @Autowired
    private SystemFileService systemFileService;
    @Autowired
    private UserPermissionService userPermissionService;
    @Autowired
    private FileService fileService;

    @Override
    public boolean type(KnowledgeFileUpload type) {
        return  type == KnowledgeFileUpload.PublicType;
    }

    @Override
    public void upload(List<MultipartFile> files, FileUpload fileUpload) throws Exception {
        String userId = fileUpload.getUserId();
        String target = fileUpload.getTarget();
        if (userId == null || target == null) {
            throw new BadRequestException("用户id或领域不可为空");
        }
        List<KnowledgeFileInfo> knowledgeFileInfoList = fileService.knowledgeFileUpload(files, userId, target,null,true);
        List<KnowledgeFileInfo> result = fileService.saveAll(knowledgeFileInfoList);
        fileService.covertPublicKnowledgeFilesAsync(result);
//        if (!userPermissionService.checkUserPermission(userId, target).isUpload()) {
//            throw new NotAccessedException("无权限访问该领域");
//        }
        // 获取当前已存在的文件列表
//        List<String> fileInSystem = fileService.getFileByTarget(target).stream()
//                .map(FileInfoFormSystem::getCategory)
//                .collect(Collectors.toList());
        // 上传前检查文件名是否重复
//        systemFileService.fileNameCheck(files, fileInSystem);
        // 上传文件到管理系统
//        systemFileService.publicFileUpload(files, userId, target);
    }
}
