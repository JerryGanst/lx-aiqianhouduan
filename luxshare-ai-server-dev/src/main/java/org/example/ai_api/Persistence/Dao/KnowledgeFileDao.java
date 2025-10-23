package org.example.ai_api.Persistence.Dao;

import org.example.ai_api.Bean.Entity.KnowledgeFileInfo;
import org.example.ai_api.Persistence.ConditionBuilder.KnowledgeFileConditionBuilder;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Collections;

@Repository
public class KnowledgeFileDao extends BaseMongoDao<KnowledgeFileInfo> {
    @Override
    protected Class<KnowledgeFileInfo> getEntityClass() {
        return KnowledgeFileInfo.class;
    }

    public List<KnowledgeFileInfo> findPrivateFilesByUploaderId(String uploaderId) {
        return find(
                new KnowledgeFileConditionBuilder()
                        .byUploaderId(uploaderId)
                        .byIsPublic(false)
        );
    }

    public List<KnowledgeFileInfo> findPublicFilesByFileTargetAndFileNames(String fileTarget, List<String> fileNames) {
        return find(
                new KnowledgeFileConditionBuilder()
                        .byFileTarget(fileTarget)
                        .byIsPublic(true)
                        .inFileNames(fileNames)
        );
    }

    public List<KnowledgeFileInfo> findPrivateFilesByUploaderIdAndFileNames(String uploaderId, List<String> fileNames) {
        return find(
                new KnowledgeFileConditionBuilder()
                        .byUploaderId(uploaderId)
                        .byIsPublic(false)
                        .inFileNames(fileNames)
        );
    }

    public List<KnowledgeFileInfo> findPublicFilesByFileTargetAndHashCodes(String fileTarget, List<String> hashCodes) {
        return find(
                new KnowledgeFileConditionBuilder()
                        .byFileTarget(fileTarget)
                        .byIsPublic(true)
                        .inHashCodes(hashCodes)
        );
    }

    public List<KnowledgeFileInfo> findPrivateFilesByUploaderIdAndHashCodes(String uploaderId, List<String> hashCodes) {
        return find(
                new KnowledgeFileConditionBuilder()
                        .byUploaderId(uploaderId)
                        .byIsPublic(false)
                        .inHashCodes(hashCodes)
        );
    }

    // 新增：根据用户ID、文件夹ID和文件名列表查找私有文件
    public List<KnowledgeFileInfo> findPrivateFilesByUploaderIdAndFolderIdAndFileNames(String uploaderId, String folderId, List<String> fileNames) {
        return find(
                new KnowledgeFileConditionBuilder()
                        .byUploaderId(uploaderId)
                        .byIsPublic(false)
                        .byFolderId(folderId) // 新增：按文件夹ID过滤
                        .inFileNames(fileNames)
        );
    }

    // 新增：根据用户ID、文件夹ID和哈希码列表查找私有文件
    public List<KnowledgeFileInfo> findPrivateFilesByUploaderIdAndFolderIdAndHashCodes(String uploaderId, String folderId, List<String> hashCodes) {
        return find(
                new KnowledgeFileConditionBuilder()
                        .byUploaderId(uploaderId)
                        .byIsPublic(false)
                        .byFolderId(folderId) // 新增：按文件夹ID过滤
                        .inHashCodes(hashCodes)
        );
    }

    public List<KnowledgeFileInfo> findPrivateFilesByUploaderIdAndFileTarget(String uploaderId, String fileTarget) {
        return find(
                new KnowledgeFileConditionBuilder()
                        .byUploaderId(uploaderId)
                        .byIsPublic(false)
                        .byFileTarget(fileTarget)
        );
    }

    public KnowledgeFileInfo findFileByFileId(String fileId) {
        return findOne(
                new KnowledgeFileConditionBuilder()
                        .byId(fileId)
        );
    }

    public List<KnowledgeFileInfo> findPrivateFilesByUserIdAndFileType(String userId, List<String> fileTypes) {
        return find(
                new KnowledgeFileConditionBuilder()
                        .byUploaderId(userId)
                        .byIsPublic(false)
                        .inFileTypes(fileTypes)
        );
    }

    public List<KnowledgeFileInfo> findByFolderId(String folderId) {
        return find(
                new KnowledgeFileConditionBuilder()
                        .byFolderId(folderId)
        );
    }

    // 新增：根据文件夹ID、用户ID和文件名模糊匹配查找文件
    public List<KnowledgeFileInfo> findByFolderIdAndUploaderIdAndFileNameRegex(String folderId, String userId, String keyword) {
        return find(
                new KnowledgeFileConditionBuilder()
                        .byFolderId(folderId)
                        .byUploaderId(userId)
                        .byFileNameRegex(keyword)
        );
    }

    public KnowledgeFileInfo findByFileId(String fileId) {
        return findOne(
                new KnowledgeFileConditionBuilder()
                        .byId(fileId)
        );
    }

    public List<KnowledgeFileInfo> findByFolderIdAndFileName(String folderId, String fileName) {
        return find(
                new KnowledgeFileConditionBuilder()
                        .byFolderId(folderId)
                        .byFileName(fileName)
        );
    }

    public List<KnowledgeFileInfo> findByFolderIdAndHashCode(String folderId, String hashCode) {
        return find(
                new KnowledgeFileConditionBuilder()
                        .byFolderId(folderId)
                        .byHashCode(hashCode)
        );
    }

    public List<KnowledgeFileInfo> findByTargetFolderIdAndFileNameRegex(List<String> targetList, String keyword) {
        return find(
                new KnowledgeFileConditionBuilder()
                        .inTargetItemIds(targetList)
                        .byFileNameRegex(keyword)
        );
    }

    public List<KnowledgeFileInfo> findByTargetFolderId(String id) {
        return find(
                new KnowledgeFileConditionBuilder()
                        .byTargetFolderId(id)
        );
    }

    public List<KnowledgeFileInfo> findByIds(List<String> ids) {
        return find(
                new  KnowledgeFileConditionBuilder()
                        .inIds(ids)
        );
    }

    public List<KnowledgeFileInfo> findByTagList(List<String> tagList) {
        return find(
                new KnowledgeFileConditionBuilder()
                        .inTargetItemIds(tagList)
        );
    }

    // Check if any knowledge file references the given tagId in targetItemIds
    public boolean existsByTargetItemIdsContaining(String tagId) {
        if (tagId == null || tagId.isEmpty()) {
            return false;
        }
        long cnt = count(new KnowledgeFileConditionBuilder()
                .inTargetItemIds(Collections.singletonList(tagId)));
        return cnt > 0;
    }
}
