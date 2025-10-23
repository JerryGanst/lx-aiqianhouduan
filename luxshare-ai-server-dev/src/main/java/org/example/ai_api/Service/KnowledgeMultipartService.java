package org.example.ai_api.Service;

import org.example.ai_api.Bean.WebRequest.*;
import org.example.ai_api.Service.Common.MultipartUploadCore;
import org.example.ai_api.Strategy.KnowledgeMultipart.KnowledgeMultipartContext;
import org.example.ai_api.Strategy.KnowledgeMultipart.KnowledgeMultipartStrategy;
import org.example.ai_api.Utils.MinioOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeMultipartService {;

    @Autowired
    private KnowledgeMultipartContext multipartContext;
    @Autowired
    private MultipartUploadCore multipartUploadCore;

    public MultipartPrepareResponse prepare(KnowledgeMultipartPrepareRequest request) throws Exception {
        KnowledgeMultipartStrategy strategy = multipartContext.getStrategy(request.getUploadType());
        strategy.validatePrepare(request);
        long partSize = multipartUploadCore.resolvePartSize(request.getPartSize(), request.getSize());
        String objectKey = strategy.buildObjectKey(request);
        return multipartUploadCore.prepareResponse(
                objectKey,
                request.getFilename(),
                request.getContentType(),
                request.getSize(),
                partSize,
                request.getExpireSeconds()
        );
    }

    public Object complete(KnowledgeMultipartCompleteRequest request) throws Exception {
        KnowledgeMultipartStrategy strategy = multipartContext.getStrategy(request.getUploadType());
        strategy.validateComplete(request);
        multipartUploadCore.maybeCompleteMultipart(request.getObjectKey(), request.getUploadId(), request.getParts());
        return strategy.registerComplete(request);
    }

    public String abort(MultipartAbortRequest request) {
        return multipartUploadCore.abort(request);
    }
}

