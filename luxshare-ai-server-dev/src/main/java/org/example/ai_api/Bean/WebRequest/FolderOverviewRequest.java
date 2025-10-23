package org.example.ai_api.Bean.WebRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class FolderOverviewRequest {
    @JsonProperty("folderId")
    private String folderId;
    @JsonProperty("userId")
    private String userId;
    @JsonProperty("isDepartment")
    private boolean isDepartment;
    @JsonProperty("keywords")
    private String keywords;

    // subfolder sorting: name | createTime
    @JsonProperty("folderSortType")
    private String folderSortType;
    @JsonProperty("folderIncrease")
    private boolean folderIncrease = true;

    // file sorting: fileName | createTime | fileSize
    @JsonProperty("fileSortType")
    private String fileSortType;
    @JsonProperty("fileIncrease")
    private boolean fileIncrease = true;
}

