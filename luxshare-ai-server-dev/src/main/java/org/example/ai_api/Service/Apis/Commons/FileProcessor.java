package org.example.ai_api.Service.Apis.Commons;

import org.example.ai_api.Bean.ApiRepeat.QueryRepeat;
import org.example.ai_api.Bean.ApiRepeat.UnifiedChatRepeat;
import org.example.ai_api.Bean.ApiRequests.AIChatRequest;
import org.example.ai_api.Bean.ApiRequests.ResumeRequest;
import org.example.ai_api.Bean.Entity.FileUpload;
import org.example.ai_api.Bean.Model.AIChatMessage;
import org.example.ai_api.Bean.Enum.FileHeaderGenerator;
import org.example.ai_api.Bean.Model.ChatMessage;
import org.example.ai_api.Bean.Model.ContentItem.BaseContentItem;
import org.example.ai_api.Bean.Model.ContentItem.TextContentItem;
import org.example.ai_api.Bean.Model.FileId;
import org.example.ai_api.Bean.Model.ResumeItem;
import org.example.ai_api.Exception.BadRequestException;
import org.example.ai_api.Exception.NotFoundException;
import org.example.ai_api.Service.FileService;
import org.example.ai_api.Service.FileUploadInfoService;
import org.example.ai_api.Service.MessageService;
import org.example.ai_api.Utils.MinioOperations;
import org.example.ai_api.Utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.*;
import java.util.function.Consumer;

/**
 * 文件处理助手：
 * - 提供历史消息中上传文件的内容拼接
 * - 统一读取临时/知识库文件内容
 * - 为 Excel/通用场景整理文件列表或文本内容
 * @author 10353965
 */
@Component
public class FileProcessor {

    @Autowired
    private FileService fileService;
    @Autowired
    private FileUploadInfoService fileUploadInfoService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private MinioOperations minioOperations;

    /**
     * 将excelId转换为excel文件路径
     * @param excelChatRequest excel对话请求
     * @param excelFiles excel文件id
     */
    public void addExcelFilesToRequest(AIChatRequest excelChatRequest, List<String> excelFiles) {
        if (excelChatRequest == null) {
            return;
        }
        if (excelFiles == null || excelFiles.isEmpty()) {
            excelChatRequest.setMetaData(new ArrayList<>());
            return;
        }
        List<String> excelFileNames = new ArrayList<>();
        for (String excelFile : excelFiles) {
            excelFileNames.add(getFileObjectNameById(excelFile));
        }
        if (excelFileNames.isEmpty()) {
            excelChatRequest.setMetaData(new ArrayList<>());
            return;
        }

        Object filePathsValue = excelFileNames.size() == 1 ? excelFileNames.get(0) : excelFileNames;
        Map<String, Object> metadataEntry = new HashMap<>();
        metadataEntry.put("excel_file_paths", filePathsValue);

        List<Map<String, Object>> metadataList = new ArrayList<>();
        metadataList.add(metadataEntry);
        excelChatRequest.setMetaData(metadataList);
    }

    /**
     * 根据文件id获取文件minio路径
     * @param fileId 文件id
     * @return 文件minio路径
     */
    public String getFileObjectNameById(String fileId) {
        try {
            return fileUploadInfoService.getFileObjectName(fileId);
        }catch (NotFoundException e){
            return fileService.getFileObjectName(fileId);
        }
    }

    /**
     * 处理历史记录中的文件（通用）
     */
    public void processHistoricalFiles(List<ChatMessage> messages) throws Exception {
        for (int index = 0; index < messages.size() - 1; index++) {
            ChatMessage message = messages.get(index);
            if (isUserMessageWithFiles(message)) {
                appendFilesToContent(message);
            }
        }
    }

    /**
     * 处理新上传的文件
     * @param fileIds 文件id列表
     * @param setFileConsumer 文件内容设置器
     * @throws Exception 处理过程中的异常
     */
    public void processNewFiles(List<FileId> fileIds, Consumer<List<String>> setFileConsumer) throws Exception {
        if (fileIds == null || fileIds.isEmpty()) {
            setFileConsumer.accept(null);
            return;
        }
        List<String> fileContents = new ArrayList<>();
        for (FileId fileId : fileIds) {
            if (fileId.isLocal()) {
                fileContents.add(fileUploadInfoService.getContentById(fileId.getFileId()));
            } else {
                fileContents.add(fileService.getContentById(fileId.getFileId()));
            }
        }
        setFileConsumer.accept(fileContents);
    }

    /**
     * 获取文件内容，如果fileId无效则返回默认内容
     */
    public String getFileContentOrDefault(FileId fileId, String defaultContent) throws Exception {
        if (fileId == null || fileId.getFileId() == null || fileId.getFileId().isEmpty()) {
            return defaultContent;
        } else {
            return fileId.isLocal() ?
                    fileUploadInfoService.getContentById(fileId.getFileId()) :
                    fileService.getContentById(fileId.getFileId());
        }
    }

    /**
     * 判断是否为包含文件的用户消息
     */
    private boolean isUserMessageWithFiles(ChatMessage message) {
        return "user".equals(message.getRole()) && message.getUploads() != null;
    }

    /**
     * 将文件内容追加到消息中
     */
    private void appendFilesToContent(ChatMessage message) throws Exception {
        List<FileUpload> files = message.getUploads();
        if (files == null || files.isEmpty()) {
            return;
        }
        String fileSection = getFileSectionFromFileUpload(files);
        String originalContent = message.getContent() != null ? message.getContent() : "";
        message.setContent(originalContent + fileSection);
    }

    private String getFileSectionFromFileUpload(List<FileUpload> files) throws Exception {
        List<String> fileContents = new ArrayList<>();
        for (FileUpload file : files) {
            if(file.isLocal()){
                fileContents.add(fileUploadInfoService.getContentById(file.getFileId()));
            }else {
                fileContents.add(fileService.getContentById(file.getFileId()));
            }
        }
        return buildFileSection(fileContents);
    }

    /**
     * 构建文件内容部分
     */
    private String buildFileSection(List<String> files) {
        StringJoiner fileJoiner = new StringJoiner("\n\n");
        for (int i = 0; i < files.size(); i++) {
            fileJoiner.add("文件" + (i + 1) + "：\n" + files.get(i));
        }

        return "\n\n#####用户提供的文件内容开始#####\n\n"
                + fileJoiner
                + "\n\n#####用户提供的文件内容结束#####\n\n";
    }

    /**
     *  将文件url添加到消息中（通用对话）
     * @param data  消息
     */
    public void addFileUrlToSource(UnifiedChatRepeat data){
        fileService.addFileUrlToSource(data);
    }

    /**
     *  将文件url添加到消息中（知识库对话）
     * @param data    消息
     * @param queryType  查询类型
     */
    public void addFileUrlToSource(QueryRepeat data,String queryType){
        messageService.addFileUrlToSource(data,queryType);
    }

    //处理历史对话过程的文件
    public void addFileContentToMessage(List<AIChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return;
        }
        history.forEach(message -> {
            //先判断当前消息有没有文件
            if("user".equals(message.getRole()) && message.getUploads() != null){
                //将文件信息放入历史
                try {
                    List<BaseContentItem> contents = message.getContent();
                    if (contents == null || contents.isEmpty()) {
                        TextContentItem textContentItem = new TextContentItem("");
                        contents = new ArrayList<>();
                        contents.add(textContentItem);
                        message.setContent(contents);
                    }
                    addFilesToContent((TextContentItem) contents.get(0),message.getUploads());
                } catch (Exception e) {
                    throw new ClassCastException("消息内容不是TextContentItem");
                }
            }
        });
    }

    private void addFilesToContent(TextContentItem textContentItem, List<FileUpload> files) throws Exception {
        String fileSection = getFileSectionFromFileUpload(files);
        textContentItem.setText(textContentItem.getText() + fileSection);
    }

    public void appendNewFilesToMessage(List<FileId> fileIds, AIChatMessage message) throws Exception {
        if (fileIds == null || fileIds.isEmpty() || message == null) {
            return;
        }
        List<String> fileContents = new ArrayList<>();
        for (FileId fileId : fileIds) {
            if (fileId == null || fileId.getFileId() == null || fileId.getFileId().isEmpty()) {
                continue;
            }
            if (fileId.isLocal()) {
                fileContents.add(fileUploadInfoService.getContentById(fileId.getFileId()));
            } else {
                fileContents.add(fileService.getContentById(fileId.getFileId()));
            }
        }
        if (fileContents.isEmpty()) {
            return;
        }

        List<BaseContentItem> contents = message.getContent();
        TextContentItem textContentItem;
        if (contents == null || contents.isEmpty()) {
            contents = new ArrayList<>();
            textContentItem = new TextContentItem("");
            contents.add(textContentItem);
            message.setContent(contents);
        } else {
            BaseContentItem firstItem = contents.get(0);
            if (!(firstItem instanceof TextContentItem)) {
                throw new ClassCastException("消息内容不是TextContentItem");
            }
            textContentItem = (TextContentItem) firstItem;
        }

        textContentItem.setText(textContentItem.getText() + buildFileSection(fileContents));
    }

    /**
     * 获取临时文件下载链接
     */
    public String getFileDownloadUrl(String fileId) throws Exception {
        return fileUploadInfoService.getDownloadUrlFromTemp(fileId);
    }

    /**
     * 获取临时文件原始文件名
     * @param fileId  临时文件id
     * @return  原始文件名
     */
    public String getOriginalFileName(String fileId){
        return fileUploadInfoService.getFileUpload(fileId).getOriginalFileName();
    }

    //将jd文件的文本内容添加到resumeRequest中
    public String getJDFileContent(String fileId) throws Exception {
        return fileUploadInfoService.getContentById(fileId);
    }

    //将resumes的简历文本内容添加到resumeRequest中
    public void buildResumesForRequest(ResumeRequest resumeRequest, List<String> resumeIds) {
        if (resumeIds == null || resumeIds.isEmpty()) {
            throw new BadRequestException("未提供简历文件");
        }
        //构造resumeRequest的resumes
        List<ResumeItem> resumes = new ArrayList<>();
        resumeIds.forEach(resumeId -> {
            try {
                String resumeContent = fileUploadInfoService.getContentById(resumeId);
                ResumeItem resumeItem = new ResumeItem(resumeId, resumeContent);
                resumes.add(resumeItem);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        resumeRequest.setResumes(resumes);
    }

    public String getDownloadUrlFormMinioByObjectName(String objectName,String fileName) throws Exception {
        Map<String, String> req = Utils.getFileHeaders(fileName, FileHeaderGenerator.DOWNLOAD);
        return minioOperations.getDownloadUrl(objectName,3600,req);
    }
}
