package org.example.ai_api.Persistence.Repository;

import org.example.ai_api.Bean.Entity.DepartmentKnowledgePermission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentFilePermissionRepository extends MongoRepository<DepartmentKnowledgePermission,String> {
    List<DepartmentKnowledgePermission> findByUserId(String userId);
}
