package org.example.ai_api.Service;

import org.apache.commons.lang3.StringUtils;
import org.example.ai_api.Bean.ApiRepeat.AIFileDeleteResponse;
import org.example.ai_api.Bean.ApiRepeat.FileSynRepeat;
import org.example.ai_api.Bean.ApiRequests.FileSynRequest;
import org.example.ai_api.Bean.Entity.DepartmentFile;
import org.example.ai_api.Bean.Entity.KnowledgeFileInfo;
import org.example.ai_api.Bean.Enum.Domain;
import org.example.ai_api.Bean.Enum.KnowledgeBaseType;
import org.example.ai_api.Exception.BadRequestException;
import org.example.ai_api.Persistence.Dao.DepartmentFileDao;
import org.example.ai_api.Persistence.Dao.KnowledgeFileDao;
import org.example.ai_api.Utils.MinioOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AIPlatformSyncService {

    private static final Logger logger = LoggerFactory.getLogger(AIPlatformSyncService.class);

    @Autowired
    private DepartmentFileDao departmentFileDao;
    @Autowired
    private KnowledgeFileDao knowledgeFileDao;
    @Autowired
    private MinioOperations minioOperations;
    @Autowired
    @Qualifier("StreamWebClient")
    private WebClient webClient;
    @Value("${minio-proxy}")
    private String minioProxy;
    @Value("${local}")
    private String local;
    @Value("${downloadTimeout}")
    private int time;
    private static final String aiPlatformSyncEndpoint = "http://10.180.116.172:7000/data_process/file/by_path";
    private static final String aiPlatformDeleteEndpoint = "http://10.180.116.172:7000/data_process/document";
    private static final Map<String,Domain> domainMap = new HashMap<>();
    private static final Map<String,List<String>> categoryMap = new HashMap<>();
    private static final Map<String, String> CONVERSION_RULES = new java.util.HashMap<>();

    static {
        domainMap.put("HR",Domain.HR);
        domainMap.put("IT",Domain.IT);
        categoryMap.put("Doc", Collections.singletonList("document"));
        categoryMap.put("QA", Collections.singletonList("QA"));
        CONVERSION_RULES.put("ppt", "pptx");
        CONVERSION_RULES.put("pptx", "pptx");
        CONVERSION_RULES.put("xls", "pdf");
        CONVERSION_RULES.put("xlsx", "pdf");
        CONVERSION_RULES.put("doc", "pdf");
        CONVERSION_RULES.put("docx", "pdf");
    }

    /**
     * 同步部门文件
     */
    public Mono<Void> syncDepartmentFile(List<String> taskIds){
        List<DepartmentFile> departmentFiles = departmentFileDao.getDepartmentFilesByIds(taskIds);
        return Flux.fromIterable(departmentFiles)
                .flatMap(file -> {
                    FileSynRequest request = buildFileSynRequest(file);
                    return syncFile(request,file.getId());
                },3)
                .then()
                .doOnSuccess(v -> logger.info("部门文件同步"))
                .doOnError(e -> logger.error("文件同步过程中出现错误: {}", e.getMessage(), e));
    }

    /**
     * 同步个人文件
     */
    public Mono<Void> syncPersonalFile(List<String> taskIds){
        List<KnowledgeFileInfo> personalFiles = knowledgeFileDao.findByIds(taskIds);
        return Flux.fromIterable(personalFiles)
                .flatMap(file -> {
                    FileSynRequest request = buildFileSynRequest(file);
                    return syncFile(request,file.getId());
                },3)
                .then()
                .doOnSuccess(v -> logger.info("个人文件同步"))
                .doOnError(e -> logger.error("文件同步过程中出现错误: {}", e.getMessage(), e));
    }

    /**
     * 同步企业文件
     */
    public Mono<Void> syncEnterpriseFile(List<String> taskIds){
        List<KnowledgeFileInfo> enterpriseFiles = knowledgeFileDao.findByIds(taskIds);
        Domain domain = domainMap.get(enterpriseFiles.get(0).getFileTarget());
        return Flux.fromIterable(enterpriseFiles)
                .flatMap(file -> {
                    FileSynRequest request = buildFileSynRequest(file,domain);
                    logger.info("同步企业文件: {}", request);
                    return  syncFile(request,file.getId());
                },3)
                .then()
                .doOnSuccess(v -> logger.info("企业文件同步 {}",domain))
                .doOnError(e -> logger.error("文件同步过程中出现错误: {}", e.getMessage(), e));
    }

    /**
     * 删除ai平台文件
     * @param documentId  ai平台的文件id
     * @return  删除结果
     */
    public Mono<Boolean> deleteFileFromAIPlatform(String documentId) {
        if (StringUtils.isBlank(documentId)) {
            logger.warn("documentId为空，不进行删除");
            return  Mono.just(true);
        }
        return webClient.delete()
                .uri(aiPlatformDeleteEndpoint + "/{document_id}", documentId)
                .retrieve()
                .bodyToMono(AIFileDeleteResponse.class)
                .map(AIFileDeleteResponse::isDeleted)
                .doOnSubscribe(s -> logger.info("开始删除AI平台文档: {}", documentId))
                .doOnSuccess(success -> {
                    if (success) {
                        logger.info("AI平台文档删除成功: {}", documentId);
                    } else {
                        logger.warn("AI平台文档删除返回deleted=false: {}", documentId);
                    }
                })
                .doOnError(e -> logger.error("AI平台删除失败: {}", e.getMessage(), e))
                .onErrorReturn(false); // 容错，删除失败不阻断主流程
    }


    /**
     * 基于部门文件构建文件同步的请求体
     * @param departmentFile 完成转换的部门文件
     * @return ai平台知识库同步请求体
     */
    private FileSynRequest buildFileSynRequest(DepartmentFile departmentFile) {
        FileSynRequest fileSynRequest;
        try {
            fileSynRequest = new FileSynRequest();
            fileSynRequest.setFilePath(minioOperations.getDownloadUrlByConvertPath(departmentFile.getConvertPath(),convertFileName(departmentFile.getFileName()),time,local,minioProxy));
            fileSynRequest.setKnowledgeBaseType(KnowledgeBaseType.Department.getType());
            fileSynRequest.setDomain(Domain.OTHER.getDomain());
            fileSynRequest.setOwnerId(departmentFile.getDepartmentId());
            fileSynRequest.setFolderId(departmentFile.getFolderId());
            fileSynRequest.setTitle(departmentFile.getFileName());
            fileSynRequest.setCategories(categoryMap.get("Doc"));
        } catch (Exception e) {
            logger.info("构建文件同步请求失败: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return fileSynRequest;
    }

    /**
     * 基于个人文件构建文件同步的请求体
     * @param personalFile 完成转换的个人文件
     * @return ai平台知识库同步请求体
     */
    private FileSynRequest buildFileSynRequest(KnowledgeFileInfo personalFile) {
        FileSynRequest fileSynRequest = new FileSynRequest();
        try {
            fileSynRequest.setFilePath(minioOperations.getDownloadUrlByConvertPath(personalFile.getConvertPath(),convertFileName(personalFile.getFileName()),time,local,minioProxy));
            fileSynRequest.setKnowledgeBaseType(KnowledgeBaseType.Personal.getType());
            fileSynRequest.setDomain(Domain.OTHER.getDomain());
            fileSynRequest.setOwnerId(personalFile.getUploaderId());
            fileSynRequest.setFolderId(personalFile.getFolderId());
            fileSynRequest.setTitle(personalFile.getFileName());
            fileSynRequest.setCategories(categoryMap.get("Doc"));
        } catch (Exception e) {
            logger.info("构建文件同步请求失败: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return fileSynRequest;
    }

    /**
     *  基于企业文件构建文件同步的请求体
     * @param enterpriseFile  完成转换的企业文件
     * @param domain   企业文件的领域
     * @return   ai平台知识库同步请求体
     */
    private  FileSynRequest buildFileSynRequest(KnowledgeFileInfo enterpriseFile,Domain domain) {
        FileSynRequest fileSynRequest = new FileSynRequest();
        try {
            fileSynRequest.setFilePath(minioOperations.getDownloadUrlByConvertPath(enterpriseFile.getConvertPath(),convertFileName(enterpriseFile.getFileName()),time,local,minioProxy));
            fileSynRequest.setKnowledgeBaseType(KnowledgeBaseType.Enterprise.getType());
            fileSynRequest.setDomain(domain.getDomain());
            fileSynRequest.setOwnerId(enterpriseFile.getUploaderId());
            fileSynRequest.setFolderId(enterpriseFile.getFolderId());
            fileSynRequest.setTitle(enterpriseFile.getFileName());
            fileSynRequest.setCategories(categoryMap.get("Doc"));
        } catch (Exception e) {
            logger.info("构建文件同步请求失败: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return fileSynRequest;
    }

    /**
     *  同步文件至ai平台，并将结果落库
     * @param fileSynRequest  文件同步请求体
     * @return  同步结果
     */
    private Mono<Void> syncFile(FileSynRequest fileSynRequest,String fileId){
        return webClient.post()
                .uri(aiPlatformSyncEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(fileSynRequest)
                .retrieve()
                .bodyToMono(FileSynRepeat.class)
                .doOnSubscribe(s -> logger.info("开始同步文件至AI平台: {}", fileSynRequest.getFilePath()))
                .flatMap(repeat -> addFileSynRepeatToDataBase(fileSynRequest,repeat,fileId))
                .doOnSuccess(v -> logger.info("文件同步成功: {}", fileSynRequest.getFilePath()))
                .doOnError(e -> logger.error("文件同步失败: {} -> {}", fileSynRequest.getFilePath(), e.getMessage()));
    }

    //结果落库
    private Mono<Void> addFileSynRepeatToDataBase(FileSynRequest request,FileSynRepeat repeat,String fileId){
        return Mono.fromRunnable(() -> {
            KnowledgeBaseType type = KnowledgeBaseType.from(request.getKnowledgeBaseType());
            switch (type){
                case Department: addDepartment(repeat,fileId);break;
                case Personal:
                case Enterprise:addKnowledge(repeat,fileId);break;
                default:throw new BadRequestException("不支持的类型");
            }
        });
    }

    //部门知识库文件落库
    private void addDepartment(FileSynRepeat repeat,String fileId){
        DepartmentFile departmentFile = departmentFileDao.findById(fileId);
        if(repeat.getSuccess()){
            departmentFile.setAiFileId(repeat.getDocumentId());
            departmentFile.setFileAbstract(repeat.getDocumentAbstract());
        }else{
            logger.info("同步部门文件失败: {}", repeat);
        }
        departmentFileDao.save(departmentFile);
    }

    //个人知识库与企业知识库文件落库
    private void addKnowledge(FileSynRepeat repeat,String fileId){
        KnowledgeFileInfo knowledgeFileInfo = knowledgeFileDao.findByFileId(fileId);
        if(repeat.getSuccess()){
            knowledgeFileInfo.setAiFileId(repeat.getDocumentId());
            knowledgeFileInfo.setFileAbstract(repeat.getDocumentAbstract());
        }else {
            logger.info("同步个人文件失败: {}", repeat);
        }
        knowledgeFileDao.save(knowledgeFileInfo);
    }

    /**
     * 按预设规则转换文件名后缀；未匹配则原样返回。
     */
    public String convertFileName(String fileName) {
        return convertFileName(fileName, CONVERSION_RULES);
    }

    /**
     * 按给定规则转换文件名后缀；未匹配则原样返回。
     * 规则示例：{"xls":"pdf","ppt":"pptx"}
     */
    public String convertFileName(String fileName, Map<String, String> rules) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("fileName 不能为空");
        }
        if (rules == null || rules.isEmpty()) {
            return fileName;
        }

        // 分离目录与基础名，尽量保留原始路径（支持 / 与 \）
        int slash = fileName.lastIndexOf('/');
        int backslash = fileName.lastIndexOf('\\');
        int lastSep = Math.max(slash, backslash);

        String dir = lastSep >= 0 ? fileName.substring(0, lastSep + 1) : "";
        String base = lastSep >= 0 ? fileName.substring(lastSep + 1) : fileName;

        if (base.isEmpty()) return fileName;

        int lastDot = base.lastIndexOf('.');
        // 无后缀，或隐藏文件（.gitignore 这类），或以点结尾，直接返回
        if (lastDot <= 0 || lastDot == base.length() - 1) {
            return fileName;
        }

        String namePart = base.substring(0, lastDot);
        String ext = base.substring(lastDot + 1);
        String mapped = rules.get(ext.toLowerCase(Locale.ROOT));

        if (mapped == null || mapped.isEmpty()) {
            return fileName; // 未命中规则
        }

        return dir + namePart + "." + mapped;
    }

}
