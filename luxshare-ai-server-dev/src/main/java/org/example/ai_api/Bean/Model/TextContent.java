package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class TextContent extends ImgContent {
    /**
     * 消息所属类型(text/image_url)
     */
    @JsonIgnore
    private String type = "text";
    @JsonProperty("text")
    private String text;
}
