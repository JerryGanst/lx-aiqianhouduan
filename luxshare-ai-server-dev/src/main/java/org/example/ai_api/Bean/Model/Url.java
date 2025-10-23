package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.example.ai_api.Bean.Entity.FileUpload;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class Url {
    /**
     * 图片url(以base64编码)
     */
    @JsonProperty("url")
    private String url;
    
    /**
     * 图片信息，用于展示用户选择的图片
     */
    @JsonProperty("image")
    private FileUpload image;
}
