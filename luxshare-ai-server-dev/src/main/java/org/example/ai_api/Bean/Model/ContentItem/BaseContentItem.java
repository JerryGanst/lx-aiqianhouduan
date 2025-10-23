package org.example.ai_api.Bean.Model.ContentItem;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Data
@Setter
@Getter
@ToString
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
        visible = true   // 子类里也能看到原始的 type 值（可选）
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextContentItem.class,  name = "text"),
        @JsonSubTypes.Type(value = ImageUrlContentItem.class, name = "image_url")
})
public abstract class BaseContentItem {
}
