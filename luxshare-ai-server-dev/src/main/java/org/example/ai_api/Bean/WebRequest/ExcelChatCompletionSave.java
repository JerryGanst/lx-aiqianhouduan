package org.example.ai_api.Bean.WebRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.example.ai_api.Bean.ApiRepeat.ExcelChatRepeat;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class ExcelChatCompletionSave {
    @JsonProperty("chatId")
    private String chatId;
    @JsonProperty("excelChatRepeat")
    private ExcelChatRepeat excelChatRepeat;
}
