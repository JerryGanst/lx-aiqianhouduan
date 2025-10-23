package org.example.ai_api.Bean.ApiRequests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.example.ai_api.Bean.Model.AIChatMessage;

import java.util.List;

/**
 * 翻译工作流请求体，对齐 /v1/chat 协议。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class TranslateRequest extends CommonChatRequest {
    @JsonProperty("history")
    private List<AIChatMessage> history;

    @JsonProperty("messages")
    private List<AIChatMessage> messages;
}
