package org.example.ai_api.Service;

import org.example.ai_api.Bean.Entity.DepartmentFile;
import org.example.ai_api.Bean.Entity.FolderList;
import org.example.ai_api.Bean.Entity.KnowledgeFileInfo;
import org.example.ai_api.Bean.Entity.SubFolderItem;
import org.example.ai_api.Bean.Entity.TargetFolderItem;
import org.example.ai_api.Bean.Enum.KnowledgeFileAction;
import org.example.ai_api.Exception.BadRequestException;
import org.example.ai_api.Exception.NotFoundException;
import org.example.ai_api.Persistence.Dao.DepartmentFileDao;
import org.example.ai_api.Persistence.Dao.FolderListDao;
import org.example.ai_api.Persistence.Dao.KnowledgeFileDao;
import org.example.ai_api.Persistence.Dao.SubFolderDao;
import org.example.ai_api.Persistence.Dao.TargetFolderDao;
import org.example.ai_api.Utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SubFolderService {

    @Autowired
    private SubFolderDao subFolderDao;
    @Autowired
    private TargetFolderDao targetFolderDao;
    @Autowired
    private FolderListDao folderListDao;
    @Autowired
    private KnowledgeFileDao knowledgeFileDao;
    @Autowired
    private DepartmentFileDao departmentFileDao;
    @Autowired
    private PermissionService permissionService;

    public SubFolderItem create(String folderId, String name, String userId) {
        if (folderId == null || folderId.isEmpty()) throw new IllegalArgumentException("folderId must not be empty");
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("name must not be empty");
        FolderList parent = folderListDao.findById(folderId);
        if (parent == null) throw new NotFoundException("parent folder not found: " + folderId);
        if (!permissionService.can(userId, folderId, KnowledgeFileAction.FOLDER_CREATE)) {
            throw new BadRequestException("无权限在此文件夹创建二级文件夹");
        }
        if (!subFolderDao.findByFolderIdAndName(folderId, name.trim()).isEmpty()) {
            throw new BadRequestException("二级文件夹名称已存在");
        }
        // 创建用于文件绑定的标签（与二级文件夹一一对应）
        TargetFolderItem tag = new TargetFolderItem();
        tag.setFolderId(folderId);
        tag.setTargetName(name.trim());
        tag.setCreatorId(userId);
        tag.setCreatTime(Utils.getNowDate());
        tag.setSubFolderTag(true);
        targetFolderDao.save(tag);

        SubFolderItem item = new SubFolderItem();
        item.setFolderId(folderId);
        item.setName(name.trim());
        item.setCreatorId(userId);
        item.setCreateTime(Utils.getNowDate());
        item.setUpdateTime(Utils.getNowDate());
        item.setTagId(tag.getId());
        return subFolderDao.save(item);
    }

    public SubFolderItem update(String id, String newName, String userId) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("id must not be empty");
        if (newName == null || newName.trim().isEmpty()) throw new IllegalArgumentException("newName must not be empty");
        SubFolderItem item = subFolderDao.findById(id);
        if (item == null) throw new NotFoundException("subfolder not found: " + id);
        String folderId = item.getFolderId();
        if (!permissionService.can(userId, folderId, KnowledgeFileAction.FOLDER_UPDATE)) {
            throw new BadRequestException("无权限修改该二级文件夹");
        }
        if (!item.getName().equals(newName.trim()) && !subFolderDao.findByFolderIdAndName(folderId, newName.trim()).isEmpty()) {
            throw new BadRequestException("二级文件夹名称已存在");
        }
        item.setName(newName.trim());
        item.setUpdateTime(Utils.getNowDate());
        // 同步更新标签名（可选）
        TargetFolderItem tag = targetFolderDao.findById(item.getTagId());
        if (tag != null) {
            tag.setTargetName(newName.trim());
            targetFolderDao.save(tag);
        }
        return subFolderDao.save(item);
    }

    public List<SubFolderItem> list(String folderId, String userId) {
        if (folderId == null || folderId.isEmpty()) throw new IllegalArgumentException("folderId must not be empty");
        FolderList parent = folderListDao.findById(folderId);
        if (parent == null) throw new NotFoundException("parent folder not found: " + folderId);
        if (!permissionService.can(userId, folderId, KnowledgeFileAction.READ)) {
            throw new BadRequestException("无权限查看该文件夹的二级文件夹");
        }
        return subFolderDao.findByFolderId(folderId);
    }

    public void delete(String id, String userId) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("id must not be empty");
        SubFolderItem item = subFolderDao.findById(id);
        if (item == null) return;
        String folderId = item.getFolderId();
        FolderList parent = folderListDao.findById(folderId);
        if (parent == null) throw new NotFoundException("parent folder not found: " + folderId);
        if (!permissionService.can(userId, folderId, KnowledgeFileAction.FOLDER_DELETE)) {
            throw new BadRequestException("无权限删除该二级文件夹");
        }
        String tagId = item.getTagId();
        // 解绑文件
        if (parent.isPublic()) {
            List<DepartmentFile> files = departmentFileDao.findByFolderId(folderId);
            for (DepartmentFile f : files) {
                if (f.getTargetItemIds() != null && f.getTargetItemIds().contains(tagId)) {
                    List<String> updated = new ArrayList<>(f.getTargetItemIds());
                    updated.remove(tagId);
                    f.setTargetItemIds(updated);
                    departmentFileDao.save(f);
                }
            }
        } else {
            List<KnowledgeFileInfo> files = knowledgeFileDao.findByFolderId(folderId);
            for (KnowledgeFileInfo f : files) {
                if (f.getTargetItemIds() != null && f.getTargetItemIds().contains(tagId)) {
                    List<String> updated = new ArrayList<>(f.getTargetItemIds());
                    updated.remove(tagId);
                    f.setTargetItemIds(updated);
                    knowledgeFileDao.save(f);
                }
            }
        }
        // 删除标签与二级目录
        if (tagId != null) {
            targetFolderDao.deleteById(tagId);
        }
        subFolderDao.deleteById(id);
    }

    /**
     * 将文件在同一父目录下从一个二级文件夹移动到另一个二级文件夹
     * 本质：在文件 targetItemIds 中移除父目录下的所有二级目录对应 tagId，然后添加目标 subFolder.tagId
     */
    public void moveFileBetweenSubFolders(String userId, String fileId, boolean isDepartment, String toSubFolderId, String fromSubFolderId) {
        if (userId == null || userId.trim().isEmpty()) throw new IllegalArgumentException("userId must not be empty");
        if (fileId == null || fileId.trim().isEmpty()) throw new IllegalArgumentException("fileId must not be empty");
        if (toSubFolderId == null || toSubFolderId.trim().isEmpty()) throw new IllegalArgumentException("toSubFolderId must not be empty");

        SubFolderItem toSub = subFolderDao.findById(toSubFolderId);
        if (toSub == null) throw new NotFoundException("target subfolder not found: " + toSubFolderId);
        String parentFolderId = toSub.getFolderId();
        FolderList parent = folderListDao.findById(parentFolderId);
        if (parent == null) throw new NotFoundException("parent folder not found: " + parentFolderId);

        // 权限：更新父目录（在其下进行子目录移动）
        if (!permissionService.can(userId, parentFolderId, KnowledgeFileAction.FOLDER_UPDATE)) {
            throw new BadRequestException("无权限在该文件夹下移动文件");
        }

        // 收集父目录下所有子目录的 tagId，用于清除同域内的旧归属
        Set<String> siblingTagIds = subFolderDao.findByFolderId(parentFolderId).stream()
                .map(SubFolderItem::getTagId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        String targetTagId = toSub.getTagId();
        if (targetTagId == null || targetTagId.isEmpty()) {
            throw new BadRequestException("目标二级文件夹缺少有效的标签");
        }

        if (isDepartment) {
            DepartmentFile file = departmentFileDao.findById(fileId);
            if (file == null) throw new NotFoundException("department file not found: " + fileId);
            // 校验文件与父目录一致
            if (!parentFolderId.equals(file.getFolderId())) {
                throw new BadRequestException("文件不属于该父目录，无法移动");
            }
            // 同步移除同父目录内的所有子目录标签
            List<String> current = file.getTargetItemIds() == null ? new ArrayList<>() : new ArrayList<>(file.getTargetItemIds());
            current = current.stream().filter(id -> !siblingTagIds.contains(id)).collect(Collectors.toList());
            // 校验来源（可选）
            if (fromSubFolderId != null && !fromSubFolderId.trim().isEmpty()) {
                SubFolderItem from = subFolderDao.findById(fromSubFolderId);
                if (from == null) throw new NotFoundException("source subfolder not found: " + fromSubFolderId);
                if (!parentFolderId.equals(from.getFolderId())) {
                    throw new BadRequestException("来源与目标不在同一父目录");
                }
            }
            // 添加目标标签
            if (!current.contains(targetTagId)) current.add(targetTagId);
            file.setTargetItemIds(current);
            departmentFileDao.save(file);
        } else {
            KnowledgeFileInfo file = knowledgeFileDao.findByFileId(fileId);
            if (file == null) throw new NotFoundException("knowledge file not found: " + fileId);
            if (!parentFolderId.equals(file.getFolderId())) {
                throw new BadRequestException("文件不属于该父目录，无法移动");
            }
            List<String> current = file.getTargetItemIds() == null ? new ArrayList<>() : new ArrayList<>(file.getTargetItemIds());
            current = current.stream().filter(id -> !siblingTagIds.contains(id)).collect(Collectors.toList());
            if (fromSubFolderId != null && !fromSubFolderId.trim().isEmpty()) {
                SubFolderItem from = subFolderDao.findById(fromSubFolderId);
                if (from == null) throw new NotFoundException("source subfolder not found: " + fromSubFolderId);
                if (!parentFolderId.equals(from.getFolderId())) {
                    throw new BadRequestException("来源与目标不在同一父目录");
                }
            }
            if (!current.contains(targetTagId)) current.add(targetTagId);
            file.setTargetItemIds(current);
            knowledgeFileDao.save(file);
        }
    }
}
