package org.example.ai_api.Bean.Entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("SessionFile")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class SessionFile {
    /**
     * 文件id
     */
    @Id
    private String id;
    /**
     * 文件名
     */
    private String fileName;
    /**
     * 文件所属对话轮数
     */
    private int iteration;
    /**
     * 文件大小
     */
    private long fileSize;
    /**
     * 文件所属文件夹id
     */
    private String folderId;
    /**
     * 文件路径
     */
    private String objectName;
    /**
     * 文件创建时间
     */
    private String createTime;
    /**
     * 文件可读性
     */
    private boolean readOnly;
}
