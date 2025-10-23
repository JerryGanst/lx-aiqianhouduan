package org.example.ai_api.Bean.Entity;

import lombok.*;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
@Document(collection = "folder_operation_log")
public class FolderOperationLog {
    @Id
    private String id;

    private String userId;
    private String userName;
    private String folderId;
    private String folderName;
    private String departmentId;
    private String departmentName;
    private String fileId;      // for department file scenarios
    private String fileName;    // for department file scenarios
    private String targetType; // FOLDER / DEPARTMENT / DEPARTMENT_FILE
    private String action; // KnowledgeFileAction name
    private boolean allowed;
    private String detail;
    private String source; // which method triggered the check
    private LocalDateTime createdAt;
}
