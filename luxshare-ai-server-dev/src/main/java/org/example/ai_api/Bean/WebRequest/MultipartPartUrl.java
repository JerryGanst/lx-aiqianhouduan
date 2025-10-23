package org.example.ai_api.Bean.WebRequest;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class MultipartPartUrl {
    private int partNumber;
    private String url;
}

