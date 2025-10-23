package org.example.ai_api.Bean.Entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("FolderList")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class FolderList {
    /**
     * 文件夹id
     */
    @Id
    private String id;
    /**
     * 文件夹名
     */
    private String folderName;
    /**
     * 文件夹创建时间
     */
    private String createTime;
    /**
     * 文件夹更新时间
     */
    private String updateTime;
    /**
     * 文件夹所属用户
     */
    private String userId;
    /**
     *  文件夹所属部门id
     */
    private String departmentId;
    /**
     * 是否是公共文件夹
     */
    private boolean isPublic =  false;
    /**
     * 是否默认文件夹
     */
    private boolean isDefault;
}
