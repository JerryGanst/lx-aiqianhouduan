package org.example.ai_api.Bean.WebRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.example.ai_api.Bean.Model.ExcelChatMessage;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class ExcelChat {
    @JsonProperty("userId")
    private String userId;
    @JsonProperty("messages")
    private List<ExcelChatMessage> messages;
    @JsonProperty("excelFiles")
    private List<String> files;
    @JsonProperty("model")
    private int model;
    @JsonProperty("sessionId")
    private String sessionId; 
}
