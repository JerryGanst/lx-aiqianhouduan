package org.example.ai_api.Service;

import org.example.ai_api.Bean.Entity.DepartmentFile;
import org.example.ai_api.Bean.Entity.FolderList;
import org.example.ai_api.Bean.Entity.KnowledgeFileInfo;
import org.example.ai_api.Bean.Entity.TargetFolderItem;
import org.example.ai_api.Bean.Enum.KnowledgeFileAction;
import org.example.ai_api.Bean.WebRequest.FolderSave;
import org.example.ai_api.Exception.BadRequestException;
import org.example.ai_api.Exception.NotAccessedException;
import org.example.ai_api.Exception.NotFoundException;
import org.example.ai_api.Persistence.Dao.DepartmentFileDao;
import org.example.ai_api.Persistence.Dao.FolderListDao;
import org.example.ai_api.Persistence.Dao.KnowledgeFileDao;
import org.example.ai_api.Persistence.Dao.TargetFolderDao;
import org.example.ai_api.Utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileFolderService {

    private static final Logger logger = LoggerFactory.getLogger(FileFolderService.class);

    @Autowired
    private FolderListDao folderListDao;
    @Autowired
    private KnowledgeFileDao knowledgeFileDao;
    @Autowired
    private DepartmentFileDao departmentFileDao;
    @Autowired
    private TargetFolderDao targetFolderDao;
    @Autowired
    private FileService fileService;
    @Autowired
    private DepartmentFileService departmentFileService;
    @Autowired
    private PermissionService permissionService;

    /**
     * 统一入口，根据请求内容创建或更新文件夹
     */
    public FolderList saveFolder(FolderSave request) {
        String folderId = request.getId();
        String folderName = request.getFolderName();
        String userId = request.getUserId();
        String departmentId = request.getDepartmentId();
        boolean departmentFolder = departmentId != null && !departmentId.isEmpty();
        if (departmentFolder) {
            if (folderId == null || folderId.isEmpty()) {
                if (!getDepartmentFolderByName(folderName, departmentId).isEmpty()) {
                    throw new BadRequestException("部门文件夹名称已存在");
                }
                return createDepartmentFolder(folderName, userId, departmentId);
            }
            return updateDepartmentFolderName(folderId, folderName, userId, departmentId);
        }
        if (folderId == null || folderId.isEmpty()) {
            if (!getUserFolderByName(folderName, userId).isEmpty()) {
                throw new BadRequestException("文件夹名称已存在");
            }
            return createFolder(folderName, userId);
        }
        return updateFolderName(folderId, folderName, userId);
    }

    /**
     * 创建个人文件夹
     */
    public FolderList createFolder(String folderName, String userId) {
        FolderList folderList = new FolderList();
        folderList.setFolderName(folderName);
        folderList.setUserId(userId);
        folderList.setCreateTime(Utils.getNowDate());
        folderList.setUpdateTime(Utils.getNowDate());
        return folderListDao.save(folderList);
    }

    /**
     * 删除文件夹（个人或部门）
     */
    public void deleteFolder(String folderId, String userId) {
        FolderList folderList = folderListDao.findById(folderId);
        if (folderList == null) {
            throw new NotFoundException("当前id对应文件夹不存在");
        }
        // 权限：删除文件夹（个人/部门）
        if (!permissionService.can(userId, folderId, KnowledgeFileAction.FOLDER_DELETE)) {
            throw new NotAccessedException("无权限删除该文件夹");
        }
        if (!folderList.isPublic()) {
            if (!userId.equals(folderList.getUserId())) {
                throw new NotAccessedException("不能删除非自己创建的文件夹");
            }
            deleteFilesInFolder(folderId, userId);
        } else {
            String departmentId = folderList.getDepartmentId();
            deleteFilesInDepartmentFolder(folderId, departmentId, userId);
        }
        folderListDao.deleteById(folderId);
    }

    /**
     * 修改个人文件夹名称
     */
    public FolderList updateFolderName(String folderId, String folderName, String userId) {
        FolderList folderList = folderListDao.findById(folderId);
        if (folderList == null) {
            throw new NotFoundException("当前id对应文件夹不存在");
        }
        // 权限：修改文件夹（个人）
        if (!permissionService.can(userId, folderId, KnowledgeFileAction.FOLDER_UPDATE)) {
            throw new NotAccessedException("无权限修改该文件夹");
        }
        if (!userId.equals(folderList.getUserId())) {
            throw new NotAccessedException("不能修改非自己创建的文件夹");
        }
        if (!folderName.equals(folderList.getFolderName()) && !getUserFolderByName(folderName, userId).isEmpty()) {
            throw new BadRequestException("文件夹名称已存在");
        }
        folderList.setFolderName(folderName);
        folderList.setUpdateTime(Utils.getNowDate());
        return folderListDao.save(folderList);
    }

    /**
     * 获取个人文件夹下文件
     */
    public List<KnowledgeFileInfo> getFilesInFolder(String folderId, String userId, String keyword, List<String> tagList) {
        FolderList folderList = folderListDao.findById(folderId);
        if (folderList == null) {
            throw new NotFoundException("当前id对应文件夹不存在");
        }
        // 权限：查看个人文件夹
        if (!permissionService.can(userId, folderId, KnowledgeFileAction.READ)) {
            throw new NotAccessedException("无权限查看该文件夹");
        }
        if (!userId.equals(folderList.getUserId())) {
            throw new NotAccessedException("不能查看非自己创建的文件夹");
        }

        List<KnowledgeFileInfo> result = knowledgeFileDao.findByFolderIdAndUploaderIdAndFileNameRegex(folderId, userId, keyword);
        if (tagList != null && !tagList.isEmpty()) {
            result = result.stream()
                    .filter(
                            file ->
                                    file.getTargetItemIds() != null &&
                                            file.getTargetItemIds().stream().anyMatch(tagList::contains)
                    )
                    .collect(Collectors.toList());
        }
       return result.stream()
                .peek(file -> {
                    if (file.getTargetItemIds() != null) {
                        file.setTargetItems(targetFolderDao.findAllById(file.getTargetItemIds()));
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取部门文件夹下文件
     */
    public List<DepartmentFile> getFilesInDepartmentFolder(String folderId, String userId, String departmentId, List<String> tagList, String keyword) {
        FolderList folderList = folderListDao.findById(folderId);
        if (folderList == null) {
            throw new NotFoundException("当前id对应文件夹不存在");
        }
        // 权限：查看部门文件夹
        if (!permissionService.can(userId, folderId, KnowledgeFileAction.READ)) {
            throw new NotAccessedException("无权限查看该部门文件夹");
        }
        List<DepartmentFile> result = departmentFileDao.findByFolderIdAndUploaderIdAndFileNameRegex(folderId, keyword);
        if (tagList != null && !tagList.isEmpty()) {
            result = result.stream()
                    .filter(
                            file ->
                                    file.getTargetItemIds() != null &&
                                            file.getTargetItemIds().stream().anyMatch(tagList::contains)
                    )
                    .collect(Collectors.toList());
        }
        return result.stream()
                .peek(file -> {
                    if (file.getTargetItemIds() != null) {
                        file.setTargetItems(targetFolderDao.findAllById(file.getTargetItemIds()));
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 移动文件，兼容个人/部门类型
     */
    public void moveFileToFolder(String fileId, String folderId, String userId, String departmentId) throws Exception {
        FolderList folderList = folderListDao.findById(folderId);
        if (folderList == null) {
            throw new NotFoundException("当前id对应文件夹不存在");
        }
        if (folderList.isPublic()) {
            if (departmentId == null || departmentId.isEmpty()) {
                throw new BadRequestException("部门ID不可为空");
            }
            // 权限：在目标部门文件夹上传（用于移动文件到该文件夹）
            if (!permissionService.can(userId, folderId, KnowledgeFileAction.FILE_UPLOAD)) {
                throw new NotAccessedException("无权限在目标文件夹上传");
            }
            if (folderList.getDepartmentId() == null || !folderList.getDepartmentId().equals(departmentId)) {
                throw new BadRequestException("目标文件夹不属于该部门");
            }
            DepartmentFile departmentFile = departmentFileDao.findById(fileId);
            if (departmentFile == null) {
                throw new NotFoundException("当前id对应文件不存在");
            }
            if (!departmentId.equals(departmentFile.getDepartmentId())) {
                throw new NotAccessedException("不能移动非本部门的文件");
            }
            departmentFileService.moveFileToFolder(departmentFile, folderId);
            departmentFileDao.save(departmentFile);
            return;
        }
        KnowledgeFileInfo knowledgeFileInfo = knowledgeFileDao.findByFileId(fileId);
        if (knowledgeFileInfo == null) {
            throw new NotFoundException("当前id对应文件不存在");
        }
        if (!userId.equals(knowledgeFileInfo.getUploaderId())) {
            throw new NotAccessedException("不能移动非自己上传的文件");
        }
        if (!userId.equals(folderList.getUserId())) {
            throw new NotAccessedException("不能移动到非自己创建的文件夹");
        }
        knowledgeFileInfo.setFolderId(folderId);
        fileService.updateMinioPathsAndMoveFile(knowledgeFileInfo, folderId);
        knowledgeFileDao.save(knowledgeFileInfo);
    }

    /**
     * 获取个人文件夹列表
     */
    public List<FolderList> getPrivateFolders(String userId, String keyword) throws Exception {
        if (userId == null || userId.isEmpty()) {
            throw new BadRequestException("用户ID不可为空");
        }
        List<FolderList> folders = getFolderList(userId).stream()
                .filter(folderList -> !folderList.isPublic())
                .collect(Collectors.toList());
        //获取用户当前所有文件
        List<KnowledgeFileInfo> files = fileService.findKnowledgeFileByUserId(userId);
        //如果用户文件夹列表为空，则新建一个根文件夹保存所有文件
        if (folders.isEmpty() && !files.isEmpty()) {
            FolderList defaultFolder = handleDefaultFolderCreation(userId, files);
            folders.add(defaultFolder);
        }
        //文件移动结束后，检查文件转换情况
        fileService.reconvert(files);
        if (keyword != null && !keyword.isEmpty()) {
            folders = folders.stream()
                    .filter(folderList -> folderList.getFolderName().contains(keyword))
                    .collect(Collectors.toList());
        }
        return folders;
    }

    /**
     * 获取部门文件夹列表
     */
    public List<FolderList> getDepartmentFolders(String departmentId, String keyword) {
        //获取当前部门下所有文件
        List<DepartmentFile> departmentFiles = departmentFileDao.findByDepartmentId(departmentId);
        List<DepartmentFile> files = departmentFiles.stream()
                .filter(departmentFile -> departmentFile.getAiFileId() == null||departmentFile.getFileAbstract() == null)
                .collect(Collectors.toList());
        departmentFileService.covertDepartmentFiles(files);
        return folderListDao.findByDepartmentId(departmentId, keyword);
    }

    /**
     * 处理个人默认文件夹创建
     */
    public FolderList handleDefaultFolderCreation(String userId, List<KnowledgeFileInfo> files) {
        FolderList defaultFolder = createFolder("我的文件", userId);
        defaultFolder.setDefault(true);
        for (KnowledgeFileInfo file : files) {
            file.setFolderId(defaultFolder.getId());
        }
        fileService.moveFilesToDefaultFolderInMinio(userId, defaultFolder.getId(), files);
        fileService.saveAll(files);
        return defaultFolder;
    }

    private List<FolderList> getFolderList(String userId) {
        return folderListDao.findByUserId(userId);
    }

    /**
     * 删除个人文件夹下全部文件
     */
    private void deleteFilesInFolder(String folderId, String userId) {
        knowledgeFileDao.findByFolderId(folderId).stream()
                .map(KnowledgeFileInfo::getId)
                .forEach(fileId -> {
                    try {
                        fileService.knowledgeFileDelete(fileId, userId);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    /**
     * 删除部门文件夹下全部文件
     */
    private void deleteFilesInDepartmentFolder(String folderId, String departmentId, String userId) {
        departmentFileDao.findByFolderId(folderId).stream()
                .map(DepartmentFile::getId)
                .forEach(fileId -> {
                    try {
                        departmentFileService.departmentFileDelete(departmentId, fileId, userId);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    /**
     * 根据名称获取个人文件夹
     */
    public List<FolderList> getUserFolderByName(String folderName, String userId) {
        return folderListDao.findByFolderNameAndUserId(folderName, userId).stream()
                .filter(folderList -> !folderList.isPublic())
                .collect(Collectors.toList());
    }

    /**
     * 根据名称获取部门文件夹
     */
    public List<FolderList> getDepartmentFolderByName(String folderName, String departmentId) {
        return folderListDao.findByFolderNameAndDepartmentId(folderName, departmentId);
    }

    /**
     * 创建部门文件夹
     */
    public FolderList createDepartmentFolder(String folderName, String userId, String departmentId) {
        if (!permissionService.canOnDepartment(userId, departmentId, KnowledgeFileAction.FOLDER_CREATE)) {
            throw new NotAccessedException("无权限创建文件夹");
        }
        FolderList folderList = new FolderList();
        folderList.setFolderName(folderName);
        folderList.setUserId(userId);
        folderList.setDepartmentId(departmentId);
        folderList.setCreateTime(Utils.getNowDate());
        folderList.setUpdateTime(Utils.getNowDate());
        folderList.setPublic(true);
        return folderListDao.save(folderList);
    }

    /**
     * 修改部门文件夹名称
     */
    public FolderList updateDepartmentFolderName(String folderId, String folderName, String userId, String departmentId) {
        FolderList folderList = folderListDao.findById(folderId);
        if (folderList == null) {
            throw new NotFoundException("当前id对应文件夹不存在");
        }
        if (!permissionService.can(userId, folderId, KnowledgeFileAction.FOLDER_UPDATE)) {
            throw new NotAccessedException("无权限修改该部门文件夹");
        }
        if (!folderName.equals(folderList.getFolderName())
                && !folderListDao.findByFolderNameAndDepartmentId(folderName, departmentId).isEmpty()) {
            throw new BadRequestException("文件夹名称已存在");
        }
        logger.info("修改文件夹名称,userId:{},folderId:{},folderName:{}", userId, folderId, folderName);
        folderList.setFolderName(folderName);
        folderList.setUpdateTime(Utils.getNowDate());
        return folderListDao.save(folderList);
    }
}
