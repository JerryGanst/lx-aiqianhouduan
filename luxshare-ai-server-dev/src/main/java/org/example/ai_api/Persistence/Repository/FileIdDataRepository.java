package org.example.ai_api.Persistence.Repository;

import org.example.ai_api.Bean.Entity.FileIdData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileIdDataRepository extends MongoRepository<FileIdData, String> {
    FileIdData findByFileIdInSystem(String fileId);
}
