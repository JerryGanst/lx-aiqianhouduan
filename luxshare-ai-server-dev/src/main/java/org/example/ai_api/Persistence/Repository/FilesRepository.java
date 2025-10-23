package org.example.ai_api.Persistence.Repository;

import org.example.ai_api.Bean.Entity.FileInfo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


//文件数据库接口
@Repository
public interface FilesRepository extends MongoRepository<FileInfo, String> {
    //根据文件名获得文件信息
    FileInfo findByFileName(String fileName);

}
