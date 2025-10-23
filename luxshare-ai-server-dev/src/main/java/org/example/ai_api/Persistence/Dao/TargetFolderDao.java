package org.example.ai_api.Persistence.Dao;

import org.example.ai_api.Bean.Entity.TargetFolderItem;
import org.example.ai_api.Persistence.ConditionBuilder.TargetFolderConditionBuilder;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TargetFolderDao extends BaseMongoDao<TargetFolderItem>{

    @Override
    protected Class<TargetFolderItem> getEntityClass() {
        return TargetFolderItem.class;
    }

    public List<TargetFolderItem> findByFolderIdAndTargetName(String folderId, String targetName) {
        return find(
                new TargetFolderConditionBuilder()
                        .byFolderId(folderId)
                        .byTargetName(targetName)
        );
    }

    public List<TargetFolderItem> findByFolderId(String folderId) {
        return find(
                new TargetFolderConditionBuilder()
                        .byFolderId(folderId)
        );
    }

    public TargetFolderItem findById(String id) {
        return findOne(
                new TargetFolderConditionBuilder()
                        .byId(id)
                );
    }

    public List<TargetFolderItem> findAllById(List<String> tagIds) {
        return find(
                new TargetFolderConditionBuilder()
                        .inIds(tagIds)
        );
    }

    public List<TargetFolderItem> findByCreatorId(String userId) {
        return find(
                new TargetFolderConditionBuilder()
                        .byCreatorId(userId)
        );
    }

    // Exclude tags that are used to represent subfolders
    public List<TargetFolderItem> findByCreatorIdExcludingSubFolders(String userId) {
        return find(
                new TargetFolderConditionBuilder()
                        .byCreatorId(userId)
                        .bySubFolderTag(false)
        );
    }

    public TargetFolderItem findOneByCreatorIdAndTargetName(String userId, String targetName) {
        return findOne(
                new TargetFolderConditionBuilder()
                        .byCreatorId(userId)
                        .byTargetName(targetName)
        );
    }

    public TargetFolderItem findOneByCreatorIdAndTargetNameExcludingSubFolders(String userId, String targetName) {
        return findOne(
                new TargetFolderConditionBuilder()
                        .byCreatorId(userId)
                        .byTargetName(targetName)
                        .bySubFolderTag(false)
        );
    }
}
