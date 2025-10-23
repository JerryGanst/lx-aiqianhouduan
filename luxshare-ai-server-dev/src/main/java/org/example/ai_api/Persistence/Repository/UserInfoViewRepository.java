package org.example.ai_api.Persistence.Repository;

import org.example.ai_api.Bean.Entity.UserInfo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface UserInfoViewRepository extends Repository<UserInfo, Long> {
    UserInfo findById(String id);

    @Query(
            value = "SELECT ZHR900101 FROM EDH.V_ZZJG_820 WHERE OBJID = :objId AND ROWNUM = 1",
            nativeQuery = true
    )
    String findLevelCode(@Param("objId") String objId);
}
