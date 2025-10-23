package org.example.ai_api.Service;

import org.example.ai_api.Bean.Entity.DepartmentFile;
import org.example.ai_api.Bean.Entity.FolderList;
import org.example.ai_api.Bean.Enum.KnowledgeFileAction;
import org.example.ai_api.Exception.NotFoundException;
import org.example.ai_api.Persistence.Dao.DepartmentFileDao;
import org.example.ai_api.Persistence.Dao.FolderListDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {

    @Autowired
    private FolderListDao folderListDao;
    @Autowired
    private DepartmentFileDao departmentFileDao;
    @Autowired
    private FolderOperationLogService folderOperationLogService;

    public boolean can(String userId, String folderId, KnowledgeFileAction action) {
        // 审计模式：不做权限拦截，只记录日志并放行
        FolderList folder = folderListDao.findById(folderId);
        if (folder == null) {
            folderOperationLogService.logFolderCheck(userId, folderId, null, action, true, "audit_only: folder not found", "PermissionService.can");
            // 资源不存在仍作为业务异常抛出
            throw new NotFoundException("Folder not found");
        }
        String targetDeptId = folder.getDepartmentId();
        String detail;
        if (!folder.isPublic()) {
            detail = "audit_only: personal folder";
        } else if (targetDeptId == null || targetDeptId.isEmpty()) {
            detail = "audit_only: public without departmentId";
        } else {
            detail = "audit_only";
        }
        folderOperationLogService.logFolderCheck(userId, folderId, targetDeptId, action, true, detail, "PermissionService.can");
        return true;
    }

    public boolean canForDepartmentFile(String userId, String departmentFileId, KnowledgeFileAction action) {
        DepartmentFile df = departmentFileDao.findById(departmentFileId);
        if (df == null) {
            folderOperationLogService.logDepartmentFileCheck(userId, null, null, null, action, false, "Department file not found", "PermissionService.canForDepartmentFile");
            throw new NotFoundException("Department file not found");
        }
        String folderId = df.getFolderId();
        if (folderId != null && !folderId.isEmpty()) {
            // 审计+放行
            folderOperationLogService.logDepartmentFileCheck(userId, df.getDepartmentId(), df.getId(), df.getOriginalFileName(), action, true, "audit_only: via folder", "PermissionService.canForDepartmentFile");
            return true;
        }
        // If folderId is missing, evaluate department-level override + default policy directly
        String targetDeptId = df.getDepartmentId();
        // 审计+放行
        folderOperationLogService.logDepartmentFileCheck(userId, targetDeptId, df.getId(), df.getOriginalFileName(), action, true, "audit_only", "PermissionService.canForDepartmentFile");
        return true;
    }

    /**
     * Department-scoped permission check, used when there is no folder yet (e.g. creating a department folder).
     * Priority: department override -> default policy.
     */
    public boolean canOnDepartment(String userId, String departmentId, KnowledgeFileAction action) {
        if (departmentId == null || departmentId.isEmpty()) {
            folderOperationLogService.logDepartmentCheck(userId, departmentId, action, true, "audit_only: empty departmentId", "PermissionService.canOnDepartment");
            return true;
        }
        // 审计+放行
        folderOperationLogService.logDepartmentCheck(userId, departmentId, action, true, "audit_only", "PermissionService.canOnDepartment");
        return true;
    }

}
