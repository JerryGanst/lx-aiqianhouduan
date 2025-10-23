package org.example.ai_api.Bean.WebRequest;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class MultipartCompletedPart {
    private int partNumber;
    private String etag;
}

