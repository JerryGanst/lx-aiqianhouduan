package org.example.ai_api.Bean.WebRequest;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class MultipartCompleteRequest {
    private String uploadId;
    private String objectKey;
    private List<MultipartCompletedPart> parts;
    private String originalFilename;
    private String contentType;
    private Long size;
    private Boolean local;

}

