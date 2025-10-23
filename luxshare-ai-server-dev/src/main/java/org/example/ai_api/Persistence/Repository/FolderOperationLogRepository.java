package org.example.ai_api.Persistence.Repository;

import org.example.ai_api.Bean.Entity.FolderOperationLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FolderOperationLogRepository extends MongoRepository<FolderOperationLog, String> {
}
