package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class FileDownloadResponse {
    @JsonProperty("download_url")
    private String downloadUrl;
    @JsonProperty("file_name")
    private String fileName;
    @JsonProperty("bucket_name")
    private String bucketName;
    @JsonProperty("object_name")
    private String objectName;
    @JsonProperty("iteration")
    private Integer iteration;
    @JsonProperty("read_only")
    private boolean readOnly;
    @JsonProperty("file_size")
    private long fileSize;
}
