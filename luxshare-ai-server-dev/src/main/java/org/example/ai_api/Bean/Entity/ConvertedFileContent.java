package org.example.ai_api.Bean.Entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "ConvertedFile")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class ConvertedFileContent {
    @Id
    private String id; // 数据库唯一id
    @Indexed(unique = true)
    private String originalFileId; // 对应的原文件在数据库的id
    private String convertedStoragePath; // 转换后的文件在Minio中的完整对象路径
    private String contentType; // 转换后内容的MIME类型 (例如 "text/plain")
    private LocalDateTime conversionTime; // 转换完成的时间
    private boolean isWrittenByAiPlatform; // 是否被AI平台成功写入/获取
}
