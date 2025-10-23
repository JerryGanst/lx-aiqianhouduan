package org.example.ai_api.Persistence.Repository;

import org.example.ai_api.Bean.Entity.RSAKeyPair;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RSAKeyPairRepository extends MongoRepository<RSAKeyPair, String> {
    RSAKeyPair findByRequestId(String requestId);
}
