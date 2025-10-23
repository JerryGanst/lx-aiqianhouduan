package org.example.ai_api.Bean.Entity;

import lombok.*;
import org.example.ai_api.Bean.Enum.KnowledgeFileAction;
import org.example.ai_api.Bean.Enum.ScopeType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "UserScopePermission")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class UserScopePermission {
    @Id
    private String id;
    private String userId;
    private ScopeType scopeType;    // DEPARTMENT or FOLDER
    private String scopeId;         // departmentId or folderId
    private KnowledgeFileAction action; // READ / UPLOAD / DELETE
    private boolean allowed;        // true allow, false deny
}

