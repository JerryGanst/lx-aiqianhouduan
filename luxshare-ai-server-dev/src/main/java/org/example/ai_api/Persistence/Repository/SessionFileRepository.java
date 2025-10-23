package org.example.ai_api.Persistence.Repository;

import org.example.ai_api.Bean.Entity.SessionFile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionFileRepository extends MongoRepository<SessionFile, String> {

}
