package org.example.ai_api.Persistence.Dao;

import org.example.ai_api.Bean.Entity.DepartmentFile;
import org.example.ai_api.Persistence.ConditionBuilder.DepartmentFileConditionBuilder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;

@Repository
public class DepartmentFileDao extends BaseMongoDao<DepartmentFile> {

    @Override
    protected Class<DepartmentFile> getEntityClass() {
        return DepartmentFile.class;
    }

    public List<DepartmentFile> findDepartmentFilesByFolderIdAndDepartmentIdAndFileNames(String departmentId, List<String> fileNames, String folderId) {
        return find(
                new DepartmentFileConditionBuilder()
                        .byDepartmentId(departmentId)
                        .byFolderId(folderId)
                        .inFileNames(fileNames)
        );
    }

    public List<DepartmentFile> findDepartmentFilesByFolderIdAndDepartmentIdAndHashCodes(String departmentId, String folderId, List<String> hashCodes) {
        return find(
                new DepartmentFileConditionBuilder()
                        .byDepartmentId(departmentId)
                        .byFolderId(folderId)
                        .inHashCodes(hashCodes)
        );
    }

    public List<DepartmentFile> saveAll(List<DepartmentFile> departmentFiles) {
        return departmentFiles.stream().map(this::save).collect(Collectors.toList());
    }

    public List<DepartmentFile> findByFolderId(String folderId) {
        return find(
                new DepartmentFileConditionBuilder()
                        .byFolderId(folderId)
        );
    }

    public DepartmentFile findById(String id) {
        return findOne(
                new DepartmentFileConditionBuilder()
                        .byId(id)
        );
    }

    public List<DepartmentFile> findByFolderIdAndUploaderIdAndFileNameRegex(String folderId, String fileNameRegex) {
        return find(
                new DepartmentFileConditionBuilder()
                        .byFolderId(folderId)
                        .byFileNameRegex(fileNameRegex)
        );
    }

    public List<DepartmentFile> findDepartmentFilesByFolderIdAndDepartmentIdAndFileName(String departmentId, String fileName, String folderId) {
        return find(
                new DepartmentFileConditionBuilder()
                        .byDepartmentId(departmentId)
                        .byFolderId(folderId)
                        .byFileName(fileName)
        );
    }

    public List<DepartmentFile> findDepartmentFilesByFolderIdAndDepartmentIdAndHash(String departmentId, String hash, String folderId) {
        return find(
                new DepartmentFileConditionBuilder()
                        .byDepartmentId(departmentId)
                        .byFolderId(folderId)
                        .byHashCode(hash)
        );
    }

    public List<DepartmentFile> findFilesByFolderIdAndFileName(String folderId, String fileName) {
        return find(
                new DepartmentFileConditionBuilder()
                        .byFolderId(folderId)
                        .byFileName(fileName)
        );
    }

    public List<DepartmentFile> findFilesByFolderIdAndHash(String folderId, String hash) {
        return find(
                new DepartmentFileConditionBuilder()
                        .byFolderId(folderId)
                        .byHashCode(hash)
        );
    }

    public List<DepartmentFile> findByTargetFolderIdAndFileNameRegex(String targetId, String keyword) {
        return find(
                new DepartmentFileConditionBuilder()
                        .byTargetFolderId(targetId)
                        .byFileNameRegex(keyword)
        );
    }

    public List<DepartmentFile> findByTargetFolderId(String id) {
        return find(
                new DepartmentFileConditionBuilder()
                        .byTargetFolderId(id)
        );
    }

    public List<DepartmentFile> getDepartmentFilesByIds(List<String> taskIds) {
        return find(
                new DepartmentFileConditionBuilder()
                        .inIds(taskIds)
        );
    }

    public List<DepartmentFile> findByTargetItemIds(List<String> targetList) {
        return find(
                new DepartmentFileConditionBuilder()
                        .inTargetItemIds(targetList)
        );
    }

    // Check if any department file references the given tagId in targetItemIds
    public boolean existsByTargetItemIdsContaining(String tagId) {
        if (tagId == null || tagId.isEmpty()) {
            return false;
        }
        long cnt = count(new DepartmentFileConditionBuilder()
                .inTargetItemIds(Collections.singletonList(tagId)));
        return cnt > 0;
    }

    public List<DepartmentFile> findByDepartmentId(String departmentId) {
        return find(
                new  DepartmentFileConditionBuilder()
                        .byDepartmentId(departmentId)
        );
    }
}

