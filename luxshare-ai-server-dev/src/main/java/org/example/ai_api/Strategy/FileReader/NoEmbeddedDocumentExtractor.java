package org.example.ai_api.Strategy.FileReader;

import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author 10353965
 */
public class NoEmbeddedDocumentExtractor implements EmbeddedDocumentExtractor {
    @Override
    public boolean shouldParseEmbedded(Metadata metadata) {
        return false; // 阻止解析嵌入的文档
    }

    @Override
    public void parseEmbedded(InputStream stream, ContentHandler handler, Metadata metadata, boolean outputHtml)
            throws SAXException, IOException {
        // 不执行任何操作
    }
}
