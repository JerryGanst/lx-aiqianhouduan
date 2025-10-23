package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class UserMessage extends BaseMessage {
    @JsonIgnore
    private String role = "user";
    
    @JsonProperty("content")
    private List<ImgContent> content;
} 