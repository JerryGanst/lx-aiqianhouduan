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
public class Summary {
    @JsonProperty("question")
    private String question;
    @JsonProperty("user_id")
    @NotBlank(message = "user_id不能为空")
    private String userId;
    @JsonProperty("file")
    private FileId file;

}
