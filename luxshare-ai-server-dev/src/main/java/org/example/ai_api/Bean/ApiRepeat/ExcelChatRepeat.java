package org.example.ai_api.Bean.ApiRepeat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.example.ai_api.Bean.Model.ExcelRepeatMetaData;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class ExcelChatRepeat {
    @JsonProperty("type")
    private String type;
    @JsonProperty("content")
    private String content;
    @JsonProperty("metadata")
    private ExcelRepeatMetaData metaData;
}
