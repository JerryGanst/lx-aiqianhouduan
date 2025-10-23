package org.example.ai_api.Persistence.Repository;

import org.example.ai_api.Bean.Entity.Target;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TargetRepository extends MongoRepository<Target, String> {
    Target findByTargetName(@NotNull String targetName);
}
