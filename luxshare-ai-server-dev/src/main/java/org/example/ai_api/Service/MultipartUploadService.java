package org.example.ai_api.Service;

import org.example.ai_api.Bean.Entity.FileUpload;
import org.example.ai_api.Bean.WebRequest.*;
import org.example.ai_api.Exception.BadRequestException;
import org.example.ai_api.Service.Common.MultipartUploadCore;
import org.example.ai_api.Utils.MinioOperations;
import org.example.ai_api.Utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MultipartUploadService {

    @Autowired
    private MinioOperations minioOperations;
    @Autowired
    private FileUploadInfoService fileUploadInfoService;
    @Autowired
    private MultipartUploadCore multipartUploadCore;

    /**
     * 分片上传前置准备
     * @param request 前端请求
     * @return 完成准备后签发的上传信息
     * @throws Exception 过程中的错误
     */
    public MultipartPrepareResponse prepare(MultipartPrepareRequest request) throws Exception {
        if (request.getFilename() == null || request.getFilename().isEmpty()) {
            throw new BadRequestException("filename 不能为空");
        }
        long partSize = multipartUploadCore.resolvePartSize(request.getPartSize(), request.getSize());
        String uniqueName = Utils.generateUniqueFileName(request.getFilename());
        String objectKey = minioOperations.createTempFileName(uniqueName);
        return multipartUploadCore.prepareResponse(
                objectKey,
                request.getFilename(),
                request.getContentType(),
                request.getSize(),
                partSize,
                request.getExpireSeconds()
        );
    }

    public String abort(MultipartAbortRequest request) {
        return multipartUploadCore.abort(request);
    }

    public FileUpload finalizeComplete(MultipartCompleteRequest request) throws Exception {
        if (request.getObjectKey() == null || request.getObjectKey().isEmpty()) {
            throw new BadRequestException("objectKey 不能为空");
        }
        multipartUploadCore.maybeCompleteMultipart(request.getObjectKey(), request.getUploadId(), request.getParts());
        boolean local = request.getLocal() != null && request.getLocal();
        FileUpload fileUpload = fileUploadInfoService.buildFileUploadFromObject(
                request.getOriginalFilename(),
                request.getObjectKey(),
                request.getContentType(),
                request.getSize() == null ? 0L : request.getSize(),
                local
        );
        fileUploadInfoService.getFileUrlByFileId(fileUpload);
        return fileUploadInfoService.save(fileUpload);
    }
}

