package org.example.ai_api.Bean.Entity;

import lombok.*;
import org.example.ai_api.Bean.Model.ExcelChatMessage;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "ExcelChatInfo")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class ExcelChatInfo {
    @Id
    private String id;
    private String userId;
    private String title;
    private List<ExcelChatMessage> messages;
    private String createTime;
    private String updateTime;
    private String lastOperationTime;
}
