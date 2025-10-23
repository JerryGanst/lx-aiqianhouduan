package org.example.ai_api.Bean.WebRequest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@ToString
public class FolderSave {
    private String id;
    private String folderName;
    private String userId;
    private String departmentId;
}
