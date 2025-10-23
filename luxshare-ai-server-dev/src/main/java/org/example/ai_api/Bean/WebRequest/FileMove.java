package org.example.ai_api.Bean.WebRequest;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@ToString
public class FileMove {
    /**
     * 目标文件夹id
     */
    @NonNull
    private String targetFolderId;
    /**
     * 文件所属用户id
     */
    @NonNull
    private String userId;
    /**
     * 文件id
     */
    @NonNull
    private String fileId;
    /**
     *  部门id
     */
    @NonNull
    private String departmentId;
}
