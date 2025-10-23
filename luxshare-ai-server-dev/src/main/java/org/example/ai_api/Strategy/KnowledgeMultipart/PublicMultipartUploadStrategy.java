package org.example.ai_api.Strategy.KnowledgeMultipart;

import org.example.ai_api.Bean.Entity.KnowledgeFileInfo;
import org.example.ai_api.Bean.Entity.FileIdData;
import org.example.ai_api.Bean.Enum.KnowledgeFileUpload;
import org.example.ai_api.Bean.Model.FileInfoFormSystem;
import org.example.ai_api.Bean.WebRequest.KnowledgeMultipartCompleteRequest;
import org.example.ai_api.Bean.WebRequest.KnowledgeMultipartPrepareRequest;
import org.example.ai_api.Bean.WebRequest.MultipartCompletedPart;
import org.example.ai_api.Exception.BadRequestException;
import org.example.ai_api.Exception.NotAccessedException;
import org.example.ai_api.Service.FileService;
import org.example.ai_api.Service.PublicFileService;
import org.example.ai_api.Service.SystemFileService;
import org.example.ai_api.Service.UserPermissionService;
import org.example.ai_api.Persistence.Repository.FileIdDataRepository;
import org.example.ai_api.Strategy.KnowledgeMultipart.Commons.FileMetaDataBuilder;
import org.example.ai_api.Strategy.KnowledgeMultipart.Commons.ValidationHelpers;
import org.example.ai_api.Utils.MinioOperations;
import org.example.ai_api.Utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PublicMultipartUploadStrategy implements KnowledgeMultipartStrategy {
    @Autowired
    private MinioOperations minioOperations;
    @Autowired
    private FileService fileService;
    @Autowired
    private PublicFileService publicFileService;
    @Autowired
    private UserPermissionService userPermissionService;
    @Autowired
    private SystemFileService systemFileService;
    @Autowired
    private FileIdDataRepository fileIdDataRepository;
    @Autowired
    private FileMetaDataBuilder fileMetaDataBuilder;

    @Override
    public boolean supports(KnowledgeFileUpload type) {
        return type == KnowledgeFileUpload.PublicType;
    }

    @Override
    public void validatePrepare(KnowledgeMultipartPrepareRequest req) throws Exception {
        //请求参数判空
        String userId = req.getUserId();
        String target = req.getTarget();
        String fileName = req.getFilename();
        ValidationHelpers.requireNonEmpty(userId,"用户id");
        ValidationHelpers.requireNonEmpty(target,"目标领域");
        ValidationHelpers.requireNonEmpty(fileName,"文件名");
        //检查用户是否有权限上传该领域
        if (!userPermissionService.checkUserPermission(userId, target).isUpload()) {
            throw new NotAccessedException("无权限访问该领域");
        }
        //文件重名检查
        List<String> fileInSystem = fileService.getFileByTarget(target).stream()
                .map(FileInfoFormSystem::getCategory)
                .collect(Collectors.toList());
        if (fileInSystem.contains(fileName)) {
            throw new BadRequestException("文件名重复");
        }
    }

    @Override
    public void validateComplete(KnowledgeMultipartCompleteRequest req) {
        //请求参数判空
        String uploadId = req.getUploadId();
        String userId = req.getUserId();
        String target = req.getTarget();
        String objectKey = req.getObjectKey();
        String originalFilename = req.getOriginalFilename();
        List<MultipartCompletedPart> parts = req.getParts();
        ValidationHelpers.requireNonEmpty(uploadId,"上传id");
        ValidationHelpers.requireNonEmpty(userId,"用户id");
        ValidationHelpers.requireNonEmpty(target,"目标领域");
        ValidationHelpers.requireNonEmpty(objectKey,"minio对象键");
        ValidationHelpers.requireNonEmpty(originalFilename,"文件名");
        ValidationHelpers.requirePartsNotEmpty(parts);
    }

    @Override
    public String buildObjectKey(KnowledgeMultipartPrepareRequest req) {
        String normalizedName = Utils.renameFileToUbuntu(req.getFilename());
        return minioOperations.createKnowledgeFileName(normalizedName, req.getTarget(), req.getUserId(), "", true);
    }

    @Override
    public Object registerComplete(KnowledgeMultipartCompleteRequest request) throws Exception {
        String objectKey = request.getObjectKey();

        // 1) 获取 MinIO 中对象大小，确认对象存在
        long sizeOnServer = minioOperations.getObjectStat(objectKey).size();

        // 2) 从 MinIO 读取对象字节，准备上传到第三方系统
        byte[] fileBytes;
        try (InputStream in = minioOperations.getFileStream(objectKey)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                bos.write(buf, 0, len);
            }
            fileBytes = bos.toByteArray();
        }

        String thirdPartyFileId;
        try {
            // 3) 先上传到第三方系统（方案A）。若失败，删除 MinIO 对象并向上抛错
            thirdPartyFileId = systemFileService.publicUploadFromBytes(
                    fileBytes,
                    request.getOriginalFilename(),
                    request.getTarget()
            );
            if (thirdPartyFileId == null || thirdPartyFileId.isEmpty()) {
                throw new RuntimeException("第三方文件系统未返回文件ID");
            }

            // 4) 仅在第三方上传成功后，登记本地 DB（KnowledgeFileInfo）
            KnowledgeFileInfo fileInfo = fileMetaDataBuilder.knowledgeFileInfoBuilder(request,sizeOnServer,true);
            List<KnowledgeFileInfo> saved = fileService.saveAll(Collections.singletonList(fileInfo));

            // 5) 建立第三方ID与本地文件ID映射
            KnowledgeFileInfo savedInfo = saved.get(0);
            FileIdData idMapping = new FileIdData();
            idMapping.setFileId(savedInfo.getId());
            idMapping.setFileIdInSystem(thirdPartyFileId);
            fileIdDataRepository.save(idMapping);

            // 6) 调用转换并保存结果（改为异步，不阻塞上传完成响应）
            publicFileService.asyncConvertAndSave(saved, request.getTarget());
            return savedInfo;
        } catch (Exception ex) {
            // 任意第三方上传或后续登记失败，删除 MinIO 中对象并抛错（回滚）
            try {
                minioOperations.deleteFile(objectKey);
            } catch (Exception delEx) {
                // 删除 MinIO 失败，仅记录，不覆盖原始异常
            }
            throw ex;
        }
    }
}
