package org.example.ai_api.Persistence.ConditionBuilder;

public class FolderListConditionBuilder extends BaseConditionBuilder<FolderListConditionBuilder>{

    public FolderListConditionBuilder ById(String folderId){
        return addCondition("id",folderId);
    }

    public FolderListConditionBuilder ByFolderName(String folderName){
        return addCondition("folderName",folderName);
    }

    public FolderListConditionBuilder ByUserId(String userId){
        return addCondition("userId",userId);
    }

    public FolderListConditionBuilder ByDepartmentId(String departmentId){
        return addCondition("departmentId",departmentId);
    }

    public FolderListConditionBuilder isDepartmentFolder(boolean isDepartmentFolder){
        return addCondition("public",isDepartmentFolder);
    }
}
