package org.example.ai_api.Bean.WebRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.example.ai_api.Bean.Model.FileId;

import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class Translate{
    @JsonProperty("sessionId")
    private String sessionId;
    @JsonProperty("user_id")
    @NotBlank(message = "user_id不能为空")
    private String userId;
    @JsonProperty("target_language")
    @NotBlank(message = "target_language不能为空")
    private String target_language;
    @JsonProperty("source_text")
    private String source_text;
    @JsonProperty("file")
    private FileId file;
}
