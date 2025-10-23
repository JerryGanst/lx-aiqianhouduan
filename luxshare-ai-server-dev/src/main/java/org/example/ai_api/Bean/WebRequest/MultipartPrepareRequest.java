package org.example.ai_api.Bean.WebRequest;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MultipartPrepareRequest {
    private String filename;
    private String contentType;
    private Long size;
    private Long partSize; // optional, bytes
    private Boolean local; // optional
    private Integer expireSeconds; // optional

}

