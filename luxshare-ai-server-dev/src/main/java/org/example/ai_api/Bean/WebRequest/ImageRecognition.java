package org.example.ai_api.Bean.WebRequest;

import lombok.*;
import org.example.ai_api.Bean.Model.BaseMessage;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 图片识别前端请求体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class ImageRecognition {
    /**
     * 用户id
     */
    @NotBlank(message = "用户id不能为空")
    private String userId;
    /**
     * 此次对话的图片信息(文件id)
     */
    private List<String> images;
    /**
     * 此次对话的文本信息
     */
    private String content;
    /**
     * 历史对话信息
     */
    private List<BaseMessage> messages;
    /**
     * 会话id
     */
    @NotBlank(message = "会话id不能为空")
    private String sessionId;
}
