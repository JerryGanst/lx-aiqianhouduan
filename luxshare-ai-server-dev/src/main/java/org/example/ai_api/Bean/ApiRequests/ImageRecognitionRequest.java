package org.example.ai_api.Bean.ApiRequests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.example.ai_api.Bean.Model.AIChatMessage;

import java.util.List;

/**
 * 图片识别模型侧请求体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class ImageRecognitionRequest extends CommonChatRequest {
    @JsonProperty("history")
    private List<AIChatMessage> history;

    @JsonProperty("messages")
    private List<AIChatMessage> messages;
}
