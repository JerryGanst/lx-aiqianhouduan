package org.example.ai_api.Bean.ApiRequests;

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
public class ExcelChatRequest {
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("excel_file_name")
    private List<String> excelFileList;
    @JsonProperty("messages")
    private List<ExcelChatMessage> excelChatMessages;
    @JsonProperty("model")
    private String model;
    @JsonProperty("stream")
    private Boolean stream;
    @JsonProperty("session_id")
    private String sessionId;
    @JsonProperty("iteration")
    private int iteration;
}
