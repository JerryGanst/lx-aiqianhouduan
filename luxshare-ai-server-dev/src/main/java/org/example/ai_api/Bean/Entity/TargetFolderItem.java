package org.example.ai_api.Bean.Entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "TargetFolderItem")
@Data
public class TargetFolderItem {
    @Id
    private String id;
    // 可选：用于按父级文件夹作用域区分的字段
    private String folderId;
    private String targetName;
    private String creatorId;
    private String creatTime;
    // 区分是否为“二级文件夹”的标签（用于前端展示过滤）；默认 false
    private boolean subFolderTag;
}
