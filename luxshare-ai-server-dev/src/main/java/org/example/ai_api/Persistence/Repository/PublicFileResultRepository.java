package org.example.ai_api.Persistence.Repository;

import org.example.ai_api.Bean.Entity.PublicFileResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PublicFileResultRepository extends MongoRepository<PublicFileResult, String> {
    List<PublicFileResult> findByFileId(String fileId);
    List<PublicFileResult> findByIsWrittenByAiPlatformFalse();
    List<PublicFileResult> findByFileIdIn(Collection<String> fileIds);
}
