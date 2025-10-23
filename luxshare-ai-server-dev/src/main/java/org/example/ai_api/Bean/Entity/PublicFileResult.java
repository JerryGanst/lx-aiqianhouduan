package org.example.ai_api.Bean.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document("PublicFileResult")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PublicFileResult {
    /**
     * 数据库id
     */
    @Id
    private String id;
    /**
     * 对应文件id
     */
    @Indexed (unique = true)
    private String fileId;
    /**
     * 文件路径
     */
    private String filePath;
    /**
     * 文件所属分类
     */
    private String fileTarget;
    /**
     * 文件类型
     */
    private String contentType;
    /**
     * 被转换时间
     */
    private LocalDateTime conversionTime;
    /**
     * 是否被处理
     */
    private boolean isWrittenByAiPlatform;
    /**
     * 被处理时间
     */
    private LocalDateTime processedTime;
}
