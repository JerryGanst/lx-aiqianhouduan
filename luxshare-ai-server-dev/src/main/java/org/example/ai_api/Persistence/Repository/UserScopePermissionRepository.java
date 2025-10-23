package org.example.ai_api.Persistence.Repository;

import org.example.ai_api.Bean.Entity.UserScopePermission;
import org.example.ai_api.Bean.Enum.KnowledgeFileAction;
import org.example.ai_api.Bean.Enum.ScopeType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserScopePermissionRepository extends MongoRepository<UserScopePermission, String> {
    UserScopePermission findFirstByUserIdAndScopeTypeAndScopeIdAndAction(
            String userId,
            ScopeType scopeType,
            String scopeId,
            KnowledgeFileAction action
    );
}

