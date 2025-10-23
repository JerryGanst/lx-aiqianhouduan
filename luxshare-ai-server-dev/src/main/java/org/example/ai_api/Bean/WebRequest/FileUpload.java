package org.example.ai_api.Bean.WebRequest;

import lombok.*;
import org.example.ai_api.Bean.Enum.KnowledgeFileUpload;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@ToString
public class FileUpload {
    private String userId;
    private String target;
    private String folderId;
    private String departmentId;
    private KnowledgeFileUpload type;
    // 可选：上传后直接归属到某个二级文件夹（通过其ID绑定对应标签）
    private String subFolderId;
}
