package org.example.ai_api.Bean.WebRequest;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class MultipartAbortRequest {
    private String uploadId;
    private String objectKey;

}

