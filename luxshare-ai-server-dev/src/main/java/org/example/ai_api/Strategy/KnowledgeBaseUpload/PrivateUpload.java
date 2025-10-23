package org.example.ai_api.Strategy.KnowledgeBaseUpload;

import org.example.ai_api.Bean.Entity.KnowledgeFileInfo;
import org.example.ai_api.Bean.Entity.SubFolderItem;
import org.example.ai_api.Bean.Enum.KnowledgeFileUpload;
import org.example.ai_api.Bean.WebRequest.FileUpload;
import org.example.ai_api.Bean.WebRequest.TagRef;
import org.example.ai_api.Bean.WebRequest.UpdateFileTagsRequest;
import org.example.ai_api.Persistence.Dao.SubFolderDao;
import org.example.ai_api.Service.FileService;
import org.example.ai_api.Service.TargetFolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class PrivateUpload implements KnowledgeUploadStrategy {

    @Autowired
    private FileService fileService;
    @Autowired
    private TargetFolderService targetFolderService;
    @Autowired
    private SubFolderDao subFolderDao;

    @Override
    public boolean type(KnowledgeFileUpload type) {
        return type == KnowledgeFileUpload.PrivateType;
    }

    @Override
    public void upload(List<MultipartFile> files, FileUpload fileUpload) throws Exception {
        String userId = fileUpload.getUserId();
        String target = fileUpload.getTarget();
        String folderId = fileUpload.getFolderId();
        List<KnowledgeFileInfo> knowledgeFileInfoList = fileService.knowledgeFileUpload(files, userId, target, folderId, false);
        List<KnowledgeFileInfo> result = fileService.saveAll(knowledgeFileInfoList);
        fileService.buildFileConversionTask(result);
        fileService.covertPrivateKnowledgeFilesAsync(result);

        // Bind uploaded files to a subfolder via its tag, if provided
        String subFolderId = fileUpload.getSubFolderId();
        if (subFolderId != null && !subFolderId.trim().isEmpty()) {
            SubFolderItem sub = subFolderDao.findById(subFolderId);
            if (sub != null && folderId != null && folderId.equals(sub.getFolderId()) && sub.getTagId() != null) {
                TagRef tagRef = new TagRef();
                tagRef.setId(sub.getTagId());
                for (KnowledgeFileInfo f : result) {
                    UpdateFileTagsRequest req = new UpdateFileTagsRequest();
                    req.setUserId(userId);
                    req.setFileId(f.getId());
                    req.setDepartment(false);
                    req.setTags(Collections.singletonList(tagRef));
                    targetFolderService.updateFileTags(req);
                }
            }
        }
    }
}
