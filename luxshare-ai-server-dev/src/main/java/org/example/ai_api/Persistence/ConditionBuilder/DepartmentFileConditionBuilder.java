package org.example.ai_api.Persistence.ConditionBuilder;

import java.util.List;

public class DepartmentFileConditionBuilder extends BaseConditionBuilder<DepartmentFileConditionBuilder>{

    public DepartmentFileConditionBuilder inFileNames(List<String> fileNames) {
        return addInCondition("fileName", fileNames);
    }

    public DepartmentFileConditionBuilder inHashCodes(List<String> hashCodes) {
        return addInCondition("hashCode", hashCodes);
    }

    public DepartmentFileConditionBuilder byDepartmentId(String departmentId) {
        return addCondition("departmentId", departmentId);
    }

    public DepartmentFileConditionBuilder byFolderId(String folderId) {
        return addCondition("folderId", folderId);
    }

    public DepartmentFileConditionBuilder byId(String id) {
        return addCondition("id", id);
    }

    public DepartmentFileConditionBuilder byFileNameRegex(String keyword) {
        return addKeywordCondition("fileName", keyword);
    }

    public DepartmentFileConditionBuilder byFileName(String fileName) {
        return addCondition("fileName", fileName);
    }

    public DepartmentFileConditionBuilder byHashCode(String hashCode) {
        return addCondition("hashCode", hashCode);
    }

    public DepartmentFileConditionBuilder byTargetFolderId(String targetId) {
        return addCondition("targetFolderId", targetId);
    }

    public DepartmentFileConditionBuilder inIds(List<String> ids) {
        return addInCondition("id", ids);
    }

    public DepartmentFileConditionBuilder inTargetItemIds(List<String> targetList) {
        return addInCondition("targetItemIds", targetList);
    }
}
