package org.example.ai_api.Bean.WebRequest;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class DownloadSessionFile {
    @NonNull
    private String userId;
    @NonNull
    private String sessionId;
    @NonNull
    private Integer iteration;
    @NonNull
    private String fileName;
    @NonNull
    private String folderName;
}
