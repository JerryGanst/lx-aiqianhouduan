package org.example.ai_api.Bean.Entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "DepartmentFile")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class DepartmentFile {
    /**
     * 文件ID
     */
    @Id
    private String id;
    /**
     * 文件原名
     */
    private String originalFileName;
    /**
     * 文件名
     */
    private String fileName;
    /**
     * 文件存储路径
     */
    private String storagePath;
    /**
     * 文件转换路径
     */
    private String convertPath;
    /**
     * 文件类型
     */
    private String fileType;
    /**
     *  文件标签
     */
    private List<String> targetItemIds;
    /**
     *  文件标签列表
     */
    private List<TargetFolderItem> targetItems;
    /**
     * 文件大小
     */
    private long fileSize;
    /**
     * 上传者ID
     */
    private String uploaderId;
    /**
     * 部门ID
     */
    private String departmentId;
    /**
     * 上传时间
     */
    private LocalDateTime createTime;
    /**
     *  更新时间
     */
    private LocalDateTime updateTime;
    /**
     * 文件哈希值
     */
    private String hashCode;
    /**
     * 文件夹ID
     */
    private String folderId;
    /**
     * ai平台的文件id
     */
    private String aiFileId;
    /**
     * 文件摘要
     */
    private String fileAbstract;
}
