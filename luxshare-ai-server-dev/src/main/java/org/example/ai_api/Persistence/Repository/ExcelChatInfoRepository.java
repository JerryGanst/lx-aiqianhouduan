package org.example.ai_api.Persistence.Repository;

import org.example.ai_api.Bean.Entity.ExcelChatInfo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExcelChatInfoRepository extends MongoRepository<ExcelChatInfo, String> {
    List<ExcelChatInfo> findByUserIdOrderByUpdateTimeDesc(String userId);
} 
