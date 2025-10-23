package org.example.ai_api.Persistence.ConditionBuilder;

import java.util.List;

public class TargetFolderConditionBuilder extends BaseConditionBuilder<TargetFolderConditionBuilder>{
    public TargetFolderConditionBuilder byFolderId(String folderId) {
        return  addCondition("folderId", folderId);
    }

    public TargetFolderConditionBuilder byTargetName(String targetName) {
        return addCondition("targetName", targetName);
    }

    public TargetFolderConditionBuilder byId(String id) {
        return addCondition("id", id);
    }

    public TargetFolderConditionBuilder inIds(List<String> tagIds) {
        return addInCondition("id", tagIds);
    }

    public  TargetFolderConditionBuilder byCreatorId(String userId) {
        return addCondition("creatorId", userId);
    }

    public TargetFolderConditionBuilder bySubFolderTag(boolean isSubFolderTag) {
        return addCondition("subFolderTag", isSubFolderTag);
    }
}
