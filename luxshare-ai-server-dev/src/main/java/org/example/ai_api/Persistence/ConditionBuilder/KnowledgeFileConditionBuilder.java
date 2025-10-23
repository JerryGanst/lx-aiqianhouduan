package org.example.ai_api.Persistence.ConditionBuilder;

import java.util.List;

public class KnowledgeFileConditionBuilder extends BaseConditionBuilder<KnowledgeFileConditionBuilder>{
    public KnowledgeFileConditionBuilder byUploaderId(String uploaderId) {
        return addCondition("uploaderId", uploaderId);
    }

    public KnowledgeFileConditionBuilder byFileTarget(String fileTarget) {
        return addCondition("fileTarget", fileTarget);
    }

    public KnowledgeFileConditionBuilder byIsPublic(boolean isPublic) {
        return addCondition("isPublic", isPublic);
    }

    public KnowledgeFileConditionBuilder inFileNames(List<String> fileNames) {
        return addInCondition("fileName", fileNames);
    }

    public KnowledgeFileConditionBuilder inHashCodes(List<String> hashCodes) {
        return addInCondition("hashCode", hashCodes);
    }

    public KnowledgeFileConditionBuilder byId(String id) {
        return addCondition("id", id);
    }

    public KnowledgeFileConditionBuilder inFileTypes(List<String> fileTypes) {
        return addInCondition("fileType", fileTypes);
    }

    public KnowledgeFileConditionBuilder byFolderId(String folderId)  {
        return addCondition("folderId", folderId);
    }

    // 新增：根据文件名模糊匹配
    public KnowledgeFileConditionBuilder byFileNameRegex(String keyword) {
        return addKeywordCondition("fileName", keyword); // 调用父类的模糊匹配方法
    }

    public KnowledgeFileConditionBuilder byFileName(String fileName) {
        return addCondition("fileName", fileName);
    }

    public KnowledgeFileConditionBuilder byHashCode(String hashCode) {
        return addCondition("hashCode", hashCode);
    }

    public KnowledgeFileConditionBuilder byTargetFolderId(String targetFolderId) {
        return addCondition("targetFolderId", targetFolderId);
    }

    public KnowledgeFileConditionBuilder inIds(List<String> ids) {
        return  addInCondition("id", ids);
    }

    public KnowledgeFileConditionBuilder inTargetItemIds(List<String> targetList) {
        return addInCondition("targetItemIds", targetList);
    }
}
