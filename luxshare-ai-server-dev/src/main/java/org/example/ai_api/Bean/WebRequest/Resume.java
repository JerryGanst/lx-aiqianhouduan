package org.example.ai_api.Bean.WebRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class Resume {
    @JsonProperty("userId")
    private String userId;
    @JsonProperty("jdText")
    private String jdText;
    @JsonProperty("jdFile")
    private String jdFile;
    @JsonProperty("resumes")
    private List<String> resumeFileIds;
}
