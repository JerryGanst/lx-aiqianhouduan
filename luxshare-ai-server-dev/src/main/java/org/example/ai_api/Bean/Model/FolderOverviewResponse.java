package org.example.ai_api.Bean.Model;

import lombok.*;
import org.example.ai_api.Bean.Entity.SubFolderItem;
import org.example.ai_api.Bean.Entity.KnowledgeFileInfo;
import org.example.ai_api.Bean.Entity.DepartmentFile;

import java.util.Collections;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class FolderOverviewResponse {
    private List<SubFolderItem> subFolders = Collections.emptyList();
    private List<KnowledgeFileInfo> personalFiles = Collections.emptyList();
    private List<DepartmentFile> departmentFiles = Collections.emptyList();
}

