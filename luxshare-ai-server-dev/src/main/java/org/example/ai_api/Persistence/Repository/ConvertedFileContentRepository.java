package org.example.ai_api.Persistence.Repository;

import org.example.ai_api.Bean.Entity.ConvertedFileContent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConvertedFileContentRepository extends MongoRepository<ConvertedFileContent, String> {
    Optional<ConvertedFileContent> findByOriginalFileId(String originalFileId);
}
