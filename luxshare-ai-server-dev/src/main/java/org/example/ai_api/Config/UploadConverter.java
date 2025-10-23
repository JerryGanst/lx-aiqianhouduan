package org.example.ai_api.Config;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.example.ai_api.Bean.Enum.KnowledgeFileUpload;

@Component
public class UploadConverter implements Converter<String, KnowledgeFileUpload> {
    @Override
    public KnowledgeFileUpload convert(@NotNull String source) {
        return KnowledgeFileUpload.from(source);
    }
}
