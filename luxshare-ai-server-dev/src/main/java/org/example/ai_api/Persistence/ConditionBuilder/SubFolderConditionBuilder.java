package org.example.ai_api.Persistence.ConditionBuilder;

public class SubFolderConditionBuilder extends BaseConditionBuilder<SubFolderConditionBuilder> {
    public SubFolderConditionBuilder byId(String id) {
        return addCondition("id", id);
    }

    public SubFolderConditionBuilder byFolderId(String folderId) {
        return addCondition("folderId", folderId);
    }

    public SubFolderConditionBuilder byName(String name) {
        return addCondition("name", name);
    }
}

