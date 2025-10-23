package org.example.ai_api.Strategy.KnowledgeBaseUpload;

import org.example.ai_api.Bean.Entity.DepartmentFile;
import org.example.ai_api.Bean.Enum.KnowledgeFileAction;
import org.example.ai_api.Bean.Enum.KnowledgeFileUpload;
import org.example.ai_api.Bean.WebRequest.FileUpload;
import org.example.ai_api.Exception.BadRequestException;
import org.example.ai_api.Exception.NotAccessedException;
import org.example.ai_api.Service.DepartmentFileService;
import org.example.ai_api.Service.TargetFolderService;
import org.example.ai_api.Persistence.Dao.SubFolderDao;
import org.example.ai_api.Bean.Entity.SubFolderItem;
import org.example.ai_api.Bean.WebRequest.TagRef;
import org.example.ai_api.Bean.WebRequest.UpdateFileTagsRequest;
import org.example.ai_api.Service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class DepartmentUpload implements  KnowledgeUploadStrategy{

    @Autowired
    private DepartmentFileService departmentFileService;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private TargetFolderService targetFolderService;
    @Autowired
    private SubFolderDao subFolderDao;

    @Override
    public boolean type(KnowledgeFileUpload type) {
        return  type == KnowledgeFileUpload.DepartmentType;
    }

    @Override
    public void upload(List<MultipartFile> files, FileUpload fileUpload) throws Exception {
        String userId = fileUpload.getUserId();
        String departmentId = fileUpload.getDepartmentId();
        String folderId = fileUpload.getFolderId();
        if (userId == null || departmentId == null || folderId == null) {
            throw new BadRequestException("userId, departmentId, folderId must not be null");
        }
        // 权限：上传至部门文件夹
        if (!permissionService.can(userId, folderId, KnowledgeFileAction.FILE_UPLOAD)) {
            throw new NotAccessedException("无权限在该文件夹上传");
        }
        List<DepartmentFile>  departmentFiles = departmentFileService.uploadDepartmentKnowledgeFile(files, userId, departmentId,folderId);
        List<DepartmentFile> result = departmentFileService.saveAllDepartmentFile(departmentFiles);
        departmentFileService.covertDepartmentFiles(result);

        // Bind to subfolder tag if provided
        String subFolderId = fileUpload.getSubFolderId();
        if (subFolderId != null && !subFolderId.trim().isEmpty()) {
            SubFolderItem sub = subFolderDao.findById(subFolderId);
            if (sub != null && folderId.equals(sub.getFolderId()) && sub.getTagId() != null) {
                TagRef tagRef = new TagRef();
                tagRef.setId(sub.getTagId());
                for (DepartmentFile f : result) {
                    UpdateFileTagsRequest req = new UpdateFileTagsRequest();
                    req.setUserId(userId);
                    req.setFileId(f.getId());
                    req.setDepartment(true);
                    req.setTags(Collections.singletonList(tagRef));
                    targetFolderService.updateFileTags(req);
                }
            }
        }
    }
}
