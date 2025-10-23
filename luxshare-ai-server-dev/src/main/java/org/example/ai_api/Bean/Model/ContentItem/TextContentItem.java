package org.example.ai_api.Bean.Model.ContentItem;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@Setter
@Getter
@ToString
public class TextContentItem extends BaseContentItem{
    @JsonIgnore
    private String type = "text";
    @JsonProperty("text")
    private String text;

    public TextContentItem(String text) {
        this.text = text;
    }
}
