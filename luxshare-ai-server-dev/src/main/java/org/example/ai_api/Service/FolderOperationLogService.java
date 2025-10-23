package org.example.ai_api.Service;

import org.example.ai_api.Bean.Entity.FolderOperationLog;
import org.example.ai_api.Bean.Entity.UserInfo;
import org.example.ai_api.Bean.Model.DeptItem;
import org.example.ai_api.Bean.Enum.KnowledgeFileAction;
import org.example.ai_api.Persistence.Dao.FolderListDao;
import org.example.ai_api.Persistence.Repository.DepartmentItemRepository;
import org.example.ai_api.Persistence.Repository.FolderOperationLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class FolderOperationLogService {

    @Autowired
    private FolderOperationLogRepository repository;
    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private FolderListDao folderListDao;
    @Autowired
    private DepartmentItemRepository departmentItemRepository;

    public void logFolderCheck(String userId, String folderId, String departmentId,
                               KnowledgeFileAction action, boolean allowed, String detail, String source) {
        try {
            FolderOperationLog log = new FolderOperationLog();
            log.setId(UUID.randomUUID().toString());
            log.setUserId(userId);
            try {
                UserInfo ui = userInfoService.findById(userId);
                if (ui != null) { log.setUserName(ui.getName()); }
            } catch (Exception ignored) {}
            log.setFolderId(folderId);
            try {
                if (folderId != null) {
                    String folderName = folderListDao.findById(folderId) == null ? null : folderListDao.findById(folderId).getFolderName();
                    log.setFolderName(folderName);
                }
            } catch (Exception ignored) {}
            log.setDepartmentId(departmentId);
            try {
                if (departmentId != null) {
                    DeptItem di = departmentItemRepository.findDeptItem(departmentId);
                    if (di != null) { log.setDepartmentName(di.getName()); }
                }
            } catch (Exception ignored) {}
            log.setTargetType("FOLDER");
            log.setAction(action == null ? null : action.name());
            log.setAllowed(allowed);
            log.setDetail(detail);
            log.setSource(source);
            log.setCreatedAt(LocalDateTime.now());
            repository.save(log);
        } catch (Exception ignored) {
            // Do not affect main flow
        }
    }

    public void logDepartmentCheck(String userId, String departmentId, KnowledgeFileAction action,
                                   boolean allowed, String detail, String source) {
        try {
            FolderOperationLog log = new FolderOperationLog();
            log.setId(UUID.randomUUID().toString());
            log.setUserId(userId);
            try {
                UserInfo ui = userInfoService.findById(userId);
                if (ui != null) { log.setUserName(ui.getName()); }
            } catch (Exception ignored) {}
            log.setDepartmentId(departmentId);
            try {
                if (departmentId != null) {
                    DeptItem di = departmentItemRepository.findDeptItem(departmentId);
                    if (di != null) { log.setDepartmentName(di.getName()); }
                }
            } catch (Exception ignored) {}
            log.setTargetType("DEPARTMENT");
            log.setAction(action == null ? null : action.name());
            log.setAllowed(allowed);
            log.setDetail(detail);
            log.setSource(source);
            log.setCreatedAt(LocalDateTime.now());
            repository.save(log);
        } catch (Exception ignored) {
            // Do not affect main flow
        }
    }

    public void logDepartmentFileCheck(String userId, String departmentId, String fileId, String fileName,
                                       KnowledgeFileAction action, boolean allowed, String detail, String source) {
        try {
            FolderOperationLog log = new FolderOperationLog();
            log.setId(UUID.randomUUID().toString());
            log.setUserId(userId);
            try {
                UserInfo ui = userInfoService.findById(userId);
                if (ui != null) { log.setUserName(ui.getName()); }
            } catch (Exception ignored) {}
            log.setDepartmentId(departmentId);
            try {
                if (departmentId != null) {
                    DeptItem di = departmentItemRepository.findDeptItem(departmentId);
                    if (di != null) { log.setDepartmentName(di.getName()); }
                }
            } catch (Exception ignored) {}
            log.setFileId(fileId);
            log.setFileName(fileName);
            log.setTargetType("DEPARTMENT_FILE");
            log.setAction(action == null ? null : action.name());
            log.setAllowed(allowed);
            log.setDetail(detail);
            log.setSource(source);
            log.setCreatedAt(LocalDateTime.now());
            repository.save(log);
        } catch (Exception ignored) {
            // Do not affect main flow
        }
    }
}
