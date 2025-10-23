package org.example.ai_api.Bean.Model.ContentItem;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Data
@Setter
@Getter
@ToString
public class ImageUrl {
    private String url;
    private String detail = "auto";
}
