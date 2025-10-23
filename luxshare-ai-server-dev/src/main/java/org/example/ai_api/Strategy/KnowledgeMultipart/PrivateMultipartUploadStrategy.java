package org.example.ai_api.Strategy.KnowledgeMultipart;

import org.example.ai_api.Bean.Entity.FolderList;
import org.example.ai_api.Bean.Entity.KnowledgeFileInfo;
import org.example.ai_api.Bean.Enum.KnowledgeFileUpload;
import org.example.ai_api.Bean.WebRequest.KnowledgeMultipartCompleteRequest;
import org.example.ai_api.Bean.WebRequest.KnowledgeMultipartPrepareRequest;
import org.example.ai_api.Bean.WebRequest.MultipartCompletedPart;
import org.example.ai_api.Exception.BadRequestException;
import org.example.ai_api.Persistence.Dao.FolderListDao;
import org.example.ai_api.Persistence.Dao.KnowledgeFileDao;
import org.example.ai_api.Service.FileService;
import org.example.ai_api.Strategy.KnowledgeMultipart.Commons.FileMetaDataBuilder;
import org.example.ai_api.Strategy.KnowledgeMultipart.Commons.ValidationHelpers;
import org.example.ai_api.Utils.MinioOperations;
import org.example.ai_api.Utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class PrivateMultipartUploadStrategy implements KnowledgeMultipartStrategy {
    @Autowired
    private MinioOperations minioOperations;
    @Autowired
    private FileService fileService;
    @Autowired
    private FolderListDao folderListDao;
    @Autowired
    private KnowledgeFileDao knowledgeFileDao;
    @Autowired
    private FileMetaDataBuilder fileMetaDataBuilder;

    @Override
    public boolean supports(KnowledgeFileUpload type) {
        return type == KnowledgeFileUpload.PrivateType;
    }

    @Override
    public void validatePrepare(KnowledgeMultipartPrepareRequest req) {
        //请求参数判空
        String userId = req.getUserId();
        String folderId = req.getFolderId();
        String fileName = req.getFilename();
        String hash = req.getHash();
        ValidationHelpers.requireNonEmpty(userId,"用户id");
        ValidationHelpers.requireNonEmpty(folderId,"文件夹id");
        ValidationHelpers.requireNonEmpty(fileName,"文件名");
        ValidationHelpers.requireNonEmpty(hash,"文件哈希");
        //检查文件夹是否存在
        FolderList  folderList = folderListDao.findById(folderId);
        if (folderList == null) {
            throw new BadRequestException("文件夹不存在");
        }
        //文件夹是否私有
        if (folderList.isPublic()){
            throw new BadRequestException("所选文件夹不是个人文件夹");
        }
        //文件夹是否属于该用户
        if (!userId.equals(folderList.getUserId())){
            throw new BadRequestException("文件夹不属于该用户");
        }
        //文件是否重名
        String filename = Utils.renameFileToUbuntu(fileName);
        List<KnowledgeFileInfo> nameList = knowledgeFileDao.findByFolderIdAndFileName(folderId, filename);
        if (!nameList.isEmpty()){
            throw new BadRequestException("文件名重复");
        }
        //文件是否重复
        List<KnowledgeFileInfo> hashList = knowledgeFileDao.findByFolderIdAndHashCode(folderId, hash);
        if (!hashList.isEmpty()){
            throw new BadRequestException("文件已存在");
        }
    }

    @Override
    public void validateComplete(KnowledgeMultipartCompleteRequest req) {
        //请求参数判空
        String uploadId = req.getUploadId();
        String objectKey = req.getObjectKey();
        String originalFilename = req.getOriginalFilename();
        String folderId = req.getFolderId();
        String hash = req.getHash();
        List<MultipartCompletedPart> parts = req.getParts();
        ValidationHelpers.requireNonEmpty(uploadId,"上传id");
        ValidationHelpers.requireNonEmpty(objectKey,"对象键");
        ValidationHelpers.requireNonEmpty(originalFilename,"文件原名");
        ValidationHelpers.requireNonEmpty(folderId,"文件夹id");
        ValidationHelpers.requireNonEmpty(hash,"文件哈希");
        ValidationHelpers.requirePartsNotEmpty(parts);
    }

    @Override
    public String buildObjectKey(KnowledgeMultipartPrepareRequest req) {
        String normalizedName = Utils.renameFileToUbuntu(req.getFilename());
        return minioOperations.createKnowledgeFileName(normalizedName, req.getTarget(), req.getUserId(), req.getFolderId(), false);
    }

    @Override
    public Object registerComplete(KnowledgeMultipartCompleteRequest request) throws Exception {
        String objectKey = request.getObjectKey();
        long sizeOnServer = minioOperations.getObjectStat(objectKey).size();

        KnowledgeFileInfo fileInfo = fileMetaDataBuilder.knowledgeFileInfoBuilder(request,sizeOnServer,false);
        List<KnowledgeFileInfo> saved = fileService.saveAll(Collections.singletonList(fileInfo));
        fileService.buildFileConversionTask(saved);
        fileService.covertPrivateKnowledgeFilesAsync(saved);
        return saved.get(0);
    }
}

