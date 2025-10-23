package org.example.ai_api.Persistence.Dao;

import org.example.ai_api.Bean.Entity.SubFolderItem;
import org.example.ai_api.Persistence.ConditionBuilder.SubFolderConditionBuilder;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SubFolderDao extends BaseMongoDao<SubFolderItem> {
    @Override
    protected Class<SubFolderItem> getEntityClass() {
        return SubFolderItem.class;
    }

    public SubFolderItem findById(String id) {
        return findOne(new SubFolderConditionBuilder().byId(id));
    }

    public List<SubFolderItem> findByFolderId(String folderId) {
        return find(new SubFolderConditionBuilder().byFolderId(folderId));
    }

    public List<SubFolderItem> findByFolderIdAndName(String folderId, String name) {
        return find(new SubFolderConditionBuilder().byFolderId(folderId).byName(name));
    }
}

