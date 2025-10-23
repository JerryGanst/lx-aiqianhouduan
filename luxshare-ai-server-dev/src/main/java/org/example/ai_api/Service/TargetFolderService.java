package org.example.ai_api.Service;

import org.example.ai_api.Bean.Entity.DepartmentFile;
import org.example.ai_api.Bean.Entity.KnowledgeFileInfo;
import org.example.ai_api.Bean.Entity.TargetFolderItem;
import org.example.ai_api.Bean.WebRequest.TagRef;
import org.example.ai_api.Bean.WebRequest.UpdateFileTagsRequest;
import org.example.ai_api.Exception.NotFoundException;
import org.example.ai_api.Persistence.Dao.DepartmentFileDao;
import org.example.ai_api.Persistence.Dao.KnowledgeFileDao;
import org.example.ai_api.Persistence.Dao.TargetFolderDao;
import org.example.ai_api.Utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TargetFolderService {

    @Autowired
    private TargetFolderDao targetFolderDao;
    @Autowired
    private KnowledgeFileDao knowledgeFileDao;
    @Autowired
    private DepartmentFileDao departmentFileDao;
    

    public List<TargetFolderItem> getTagsByIds(List<String> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return Collections.emptyList();
        }
        return targetFolderDao.findAllById(tagIds);
    }

    public void deleteTagsByIds(List<String> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            return;
        }
        for (String targetId : targetIds) {
            targetFolderDao.deleteById(targetId);
        }
    }

    public List<TargetFolderItem> getTagsByUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            return Collections.emptyList();
        }
        return targetFolderDao.findByCreatorId(userId).stream()
                .filter(item -> !item.isSubFolderTag())
                .collect(Collectors.toList());
    }

    // Update file tags to match desired list; if fileId is empty, only create/resolve tags and return them
    public List<TargetFolderItem> updateFileTags(UpdateFileTagsRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body must not be null");
        }

        List<TagRef> desired = request.getTags() != null ? request.getTags() : new ArrayList<>();

        // 当 fileId 为空时，仅创建/解析标签
        if (request.getFileId() == null || request.getFileId().trim().isEmpty()) {
            List<String> desiredIds = resolveDesiredTagIds(desired, request.getUserId());
            return desiredIds.isEmpty() ? Collections.emptyList() : targetFolderDao.findAllById(desiredIds);
        }

        // 使用泛型方法统一逻辑
        if (request.isDepartment()) {
            return updateTagsGeneric(
                    request,
                    id -> departmentFileDao.findById(id),
                    DepartmentFile::getTargetItemIds,
                    DepartmentFile::setTargetItemIds,
                    departmentFileDao::save
            );
        } else {
            return updateTagsGeneric(
                    request,
                    id -> knowledgeFileDao.findByFileId(id),
                    KnowledgeFileInfo::getTargetItemIds,
                    KnowledgeFileInfo::setTargetItemIds,
                    knowledgeFileDao::save
            );
        }
    }

    /**
     * 通用标签更新逻辑，支持不同文件类型（部门文件、知识文件等）
     *
     * @param request      更新请求
     * @param finder       根据 fileId 获取文件对象的函数
     * @param getter       获取当前 tagId 列表的函数
     * @param setter       设置更新后 tagId 列表的函数
     * @param saver        保存实体的函数
     * @param <T>          文件实体类型（DepartmentFile 或 KnowledgeFileInfo）
     */
    private <T> List<TargetFolderItem> updateTagsGeneric(
            UpdateFileTagsRequest request,
            Function<String, T> finder,
            Function<T, List<String>> getter,
            BiConsumer<T, List<String>> setter,
            Consumer<T> saver
    ) {
        T file = finder.apply(request.getFileId());
        if (file == null) {
            throw new NotFoundException("file not found for id: " + request.getFileId());
        }

        List<String> currentIds = getter.apply(file) == null ? new ArrayList<>() : new ArrayList<>(getter.apply(file));
        List<String> desiredIds = resolveDesiredTagIds(request.getTags(), request.getUserId());
        List<String> updatedIds = applyTagsDiff(currentIds, desiredIds);
        setter.accept(file, updatedIds);
        saver.accept(file);
        List<String> removedIds = new ArrayList<>(currentIds);
        removedIds.removeAll(updatedIds);
        cleanupUnusedTags(removedIds);

        return targetFolderDao.findAllById(updatedIds);
    }

    private List<String> resolveDesiredTagIds(List<TagRef> desired, String userId) {
        List<String> desiredIds = new ArrayList<>();
        if (desired == null || desired.isEmpty()) return desiredIds;

        for (TagRef ref : desired) {
            if (ref == null) continue;
            if (ref.getId() != null && !ref.getId().trim().isEmpty()) {
                desiredIds.add(ref.getId());
            } else if (ref.getTargetName() != null && !ref.getTargetName().trim().isEmpty()) {
                if (userId == null || userId.trim().isEmpty()) {
                    throw new IllegalArgumentException("userId required to create new tag");
                }
                TargetFolderItem existing = targetFolderDao.findOneByCreatorIdAndTargetNameExcludingSubFolders(userId, ref.getTargetName());
                if (existing != null) {
                    desiredIds.add(existing.getId());
                } else {
                    TargetFolderItem tag = new TargetFolderItem();
                    tag.setTargetName(ref.getTargetName());
                    tag.setCreatorId(userId);
                    tag.setCreatTime(Utils.getNowDate());
                    tag.setSubFolderTag(false);
                    targetFolderDao.save(tag);
                    desiredIds.add(tag.getId());
                }
            }
        }
        return desiredIds;
    }

    // Apply desiredIds to currentIds and return the updated list (bind/unbind by diff)
    private List<String> applyTagsDiff(List<String> currentIds, List<String> desiredIds) {
        List<String> safeCurrent = currentIds == null ? new ArrayList<>() : new ArrayList<>(currentIds);
        List<String> safeDesired = desiredIds == null ? new ArrayList<>() : new ArrayList<>(desiredIds);

        // add missing
        for (String id : safeDesired) {
            if (!safeCurrent.contains(id)) {
                safeCurrent.add(id);
            }
        }
        // remove extra
        if (!safeCurrent.isEmpty()) {
            safeCurrent = safeCurrent.stream()
                    .filter(safeDesired::contains)
                    .collect(Collectors.toList());
        }
        return safeCurrent;
    }

    private void cleanupUnusedTags(List<String> removedTagIds) {
        if (removedTagIds == null || removedTagIds.isEmpty()) return;

        for (String tagId : removedTagIds) {
            boolean inKnowledgeFiles = knowledgeFileDao.existsByTargetItemIdsContaining(tagId);
            boolean inDepartmentFiles = departmentFileDao.existsByTargetItemIdsContaining(tagId);
            if (!inKnowledgeFiles && !inDepartmentFiles) {
                targetFolderDao.deleteById(tagId);
            }
        }
    }

}
