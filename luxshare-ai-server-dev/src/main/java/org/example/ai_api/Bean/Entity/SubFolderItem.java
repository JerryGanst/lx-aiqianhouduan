package org.example.ai_api.Bean.Entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "SubFolderItem")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class SubFolderItem {
    @Id
    private String id;
    // 父级文件夹ID（个人或部门文件夹）
    private String folderId;
    // 二级文件夹名称
    private String name;
    // 创建人
    private String creatorId;
    // 创建时间
    private String createTime;
    // 更新时间
    private String updateTime;
    // 绑定到文件用的标签ID（TargetFolderItem.id）
    private String tagId;
}

