package org.example.ai_api.Persistence.Repository;

import org.example.ai_api.Bean.Entity.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoryRepository extends MongoRepository<Message,String> {

}
