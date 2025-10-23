package org.example.ai_api.Persistence.Repository;

import org.example.ai_api.Bean.Entity.FileConversionTask;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileConversionTaskRepository extends MongoRepository<FileConversionTask, String> {
    FileConversionTask findByFileId(String fileId);
}
