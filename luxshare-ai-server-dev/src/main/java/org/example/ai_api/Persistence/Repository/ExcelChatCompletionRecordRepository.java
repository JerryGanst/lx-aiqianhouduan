package org.example.ai_api.Persistence.Repository;

import org.example.ai_api.Bean.Entity.ExcelChatCompletionRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExcelChatCompletionRecordRepository extends MongoRepository<ExcelChatCompletionRecord, String> {
}
