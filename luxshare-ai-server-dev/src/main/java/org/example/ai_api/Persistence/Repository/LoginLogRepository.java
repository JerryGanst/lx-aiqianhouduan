package org.example.ai_api.Persistence.Repository;

import org.example.ai_api.Bean.Entity.LoginLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginLogRepository extends MongoRepository<LoginLog, String> {
    LoginLog findByUserId(String userId);
}
