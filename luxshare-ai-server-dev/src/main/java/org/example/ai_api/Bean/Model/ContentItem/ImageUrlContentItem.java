package org.example.ai_api.Bean.Model.ContentItem;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@Setter
@Getter
@ToString
public class ImageUrlContentItem extends BaseContentItem{
    @JsonIgnore
    private String type = "image_url";
    @JsonProperty("image_url")
    private ImageUrl imageUrl;
}
