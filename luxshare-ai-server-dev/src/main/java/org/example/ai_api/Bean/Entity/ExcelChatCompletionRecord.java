package org.example.ai_api.Bean.Entity;

import lombok.*;
import org.example.ai_api.Bean.ApiRepeat.ExcelChatRepeat;
import org.example.ai_api.Bean.Model.ExcelRepeatMetaData;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "ExcelChatCompletion")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class ExcelChatCompletionRecord {
    /**
     * 数据库id
     */
    @Id
    private String id;
    /**
     * 相关联的会话id
     */
    private String chatId;
    /**
     * 最后记录的信息
     */
    private ExcelChatRepeat excelChatRepeat;
    /**
     * 创建时间
     */
    private String createTime;
}
