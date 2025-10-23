package org.example.ai_api.Bean.Model;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class MCPFileInfo {
    private String fileName;
    private String folderName;
    private int iteration;
}
