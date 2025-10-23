package org.example.ai_api.Persistence.Repository;

import org.example.ai_api.Bean.Entity.UserPermission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPermissionRepository extends MongoRepository<UserPermission, String> {
    List<UserPermission> findUserPermissionByUserId(String userId);
    void removeUserPermissionByUserIdAndTarget(String userId, String target);
    UserPermission findUserPermissionByUserIdAndTarget(String userId, String target);
}
