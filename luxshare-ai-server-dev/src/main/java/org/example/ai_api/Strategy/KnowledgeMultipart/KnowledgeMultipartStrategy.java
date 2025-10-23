package org.example.ai_api.Strategy.KnowledgeMultipart;

import org.example.ai_api.Bean.Enum.KnowledgeFileUpload;
import org.example.ai_api.Bean.WebRequest.KnowledgeMultipartCompleteRequest;
import org.example.ai_api.Bean.WebRequest.KnowledgeMultipartPrepareRequest;

public interface KnowledgeMultipartStrategy {
    boolean supports(KnowledgeFileUpload type);

    void validatePrepare(KnowledgeMultipartPrepareRequest req) throws Exception;

    void validateComplete(KnowledgeMultipartCompleteRequest req);

    String buildObjectKey(KnowledgeMultipartPrepareRequest req) throws Exception;

    Object registerComplete(KnowledgeMultipartCompleteRequest req) throws Exception;
}

