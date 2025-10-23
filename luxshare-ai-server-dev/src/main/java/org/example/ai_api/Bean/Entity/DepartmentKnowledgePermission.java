package org.example.ai_api.Bean.Entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "DepartmentKnowledgePermission")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class DepartmentKnowledgePermission {
    @Id
    private String id;
    private String userId;
    private String departmentId;
    private boolean upload;
    private boolean delete;
}
