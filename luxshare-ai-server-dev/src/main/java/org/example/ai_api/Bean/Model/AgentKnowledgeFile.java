package org.example.ai_api.Bean.Model;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class AgentKnowledgeFile {
    /**
     * 文件id
     */
    private String fileId;
    /**
     * 文件名
     */
    private String fileName;
    /**
     * 文件类型
     */
    private String extension;
    /**
     * 文件是否本地
     */
    private Boolean isLocal;
}
