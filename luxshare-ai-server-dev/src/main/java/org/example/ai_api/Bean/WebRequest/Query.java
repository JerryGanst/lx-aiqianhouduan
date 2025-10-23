package org.example.ai_api.Bean.WebRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.validation.constraints.NotBlank;

//知识库问答请求体
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class Query {
    @JsonProperty("type")
    @NotBlank(message = "type不能为空")
    private String type;
    @JsonProperty("question")
    private String question;
    @JsonProperty("user_id")
    @NotBlank(message = "user_id不能为空")
    private String userId;
    @JsonProperty("model")
    private int model;
}
