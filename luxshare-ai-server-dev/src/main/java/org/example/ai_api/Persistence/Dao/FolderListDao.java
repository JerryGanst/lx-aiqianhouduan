package org.example.ai_api.Persistence.Dao;

import org.example.ai_api.Bean.Entity.FolderList;
import org.example.ai_api.Persistence.ConditionBuilder.FolderListConditionBuilder;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class FolderListDao extends BaseMongoDao<FolderList>{
    @Override
    protected Class<FolderList> getEntityClass() {
        return FolderList.class;
    }

    public FolderList findById(String folderId){
        return findOne(
          new FolderListConditionBuilder()
                  .ById(folderId)
        );
    }

    public List<FolderList> findByDepartmentId(String departmentId,String keyword) {
        return find(
                new FolderListConditionBuilder()
                        .ByDepartmentId(departmentId)
                        .addKeywordCondition("folderName", keyword)
                        .addDescSort("updateTime")
        );
    }

    public List<FolderList> findByFolderNameAndUserId(String folderName, String userId){
        return find(
                new FolderListConditionBuilder()
                        .ByFolderName(folderName)
                        .ByUserId(userId)
        );
    }

    public List<FolderList> findByUserId(String userId){
        return find(
                new FolderListConditionBuilder()
                        .ByUserId(userId)
                        .addDescSort("updateTime")
        );
    }

    public List<FolderList> findByFolderNameAndDepartmentId(String folderName, String departmentId){
        return  find(
                new FolderListConditionBuilder()
                        .ByFolderName(folderName)
                        .ByDepartmentId(departmentId)
        );
    }
}
