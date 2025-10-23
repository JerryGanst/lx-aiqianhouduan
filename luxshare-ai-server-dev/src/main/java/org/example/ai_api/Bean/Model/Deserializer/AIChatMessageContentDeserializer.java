package org.example.ai_api.Bean.Model.Deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ai_api.Bean.Model.ContentItem.BaseContentItem;
import org.example.ai_api.Bean.Model.ContentItem.TextContentItem;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 自定义反序列化器，用于兼容 content 字段既可以是字符串也可以是数组的情况。
 */
public class AIChatMessageContentDeserializer extends JsonDeserializer<List<BaseContentItem>> {

    @Override
    public List<BaseContentItem> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken currentToken = p.currentToken();
        if (currentToken == JsonToken.VALUE_STRING) {
            return Collections.singletonList(new TextContentItem(p.getValueAsString()));
        }

        if (currentToken == JsonToken.VALUE_NULL) {
            return null;
        }

        if (currentToken == JsonToken.START_ARRAY) {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            return mapper.readValue(p, mapper.getTypeFactory().constructCollectionType(List.class, BaseContentItem.class));
        }

        if (currentToken == JsonToken.START_OBJECT) {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            BaseContentItem item = mapper.readValue(p, BaseContentItem.class);
            return Collections.singletonList(item);
        }

        throw JsonMappingException.from(p, "Unsupported token for content field: " + currentToken);
    }

    @Override
    public List<BaseContentItem> getNullValue(DeserializationContext ctxt) {
        return null;
    }
}
