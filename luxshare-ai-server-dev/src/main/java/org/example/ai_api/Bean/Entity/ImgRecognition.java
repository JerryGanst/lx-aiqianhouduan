package org.example.ai_api.Bean.Entity;

import lombok.*;
import org.example.ai_api.Bean.Model.BaseMessage;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * 用户图片对比历史记录
 */
@Document(collection = "ImgRecognition")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class ImgRecognition {
    /**
     * 唯一id
     */
    @Id
    private String id;
    /**
     * 对话用户id
     */
    private String userId;
    /**
     * 对话标题
     */
    private String title;
    /**
     * 对话记录
     */
    private List<BaseMessage> imgMessages;
    /**
     * 创建时间
     */
    private String createTime;
    /**
     * 更新时间
     */
    private String updateTime;
    /**
     * 操作时间
     */
    private String lastOperationTime;
}
