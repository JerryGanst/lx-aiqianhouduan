package org.example.ai_api.Bean.WebRequest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class MultipartPrepareResponse {
    private String uploadId;
    private String objectKey;
    private long partSize;
    private int partCount;
    private List<MultipartPartUrl> parts;

}

