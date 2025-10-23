package org.example.ai_api.Service;

import org.example.ai_api.Bean.Entity.FileUpload;
import org.example.ai_api.Bean.Enum.FileHeaderGenerator;
import org.example.ai_api.Exception.DataNotComplianceException;
import org.example.ai_api.Exception.NotFoundException;
import org.example.ai_api.Persistence.Repository.FileUploadInfoRepository;
import org.example.ai_api.Utils.FileContentReader;
import org.example.ai_api.Utils.MinioOperations;
import org.example.ai_api.Utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 用户问答过程文件上传相关服务.
 * @author 10353965
 */
@Service
public class FileUploadInfoService {
    private static final Logger logger = LoggerFactory.getLogger(FileUploadInfoService.class);
    /**
     * The File upload info repository.
     */
    @Autowired
    private FileUploadInfoRepository fileUploadInfoRepository;
    /**
     * The Mongo template.
     */
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private FileContentReader fileContentReader;
    @Autowired
    private MinioOperations minioOperations;
    @Autowired
    private FileService fileService;
    @Autowired
    @Qualifier("StreamWebClient")
    private WebClient webClient;
    @Value("${downloadTimeout}")
    private int downloadTimeout;
    @Value("${minio-proxy}")
    private String minioProxy;
    @Value("${local}")
    private String local;
    @Value("${libreoffice_FileUploads}")
    private String libreOfficeFileUploads;


    /**
     * 文件批量上传.
     *
     * @param fileUploads 批量上传的文件
     * @return 上传后的文件信息列表
     */
    public List<FileUpload> saveAll(List<FileUpload> fileUploads) {
        return fileUploadInfoRepository.saveAll(fileUploads);
    }

    /**
     * 保存单个文件信息
     */
    public FileUpload save(FileUpload fileUpload) {
        return fileUploadInfoRepository.save(fileUpload);
    }

    public FileUpload getFileUpload(String fileId){
        return fileUploadInfoRepository.findById(fileId).orElseThrow(() -> new NotFoundException("文件id对应的文件不存在"));
    }

    /**
     * 根据id获得文件文本.
     *
     * @param id 文件id
     * @return 对应id文件内的文本
     */
    public String getContentById(String id) throws Exception {
        logger.info("根据id{}获得文件文本", id);
        FileUpload fileUpload = fileUploadInfoRepository.findById(id).orElseThrow(() -> new NotFoundException("文件不存在"));
        InputStream stream = minioOperations.getFileStream(fileUpload.getFilePath());
        return fileContentReader.readFile(stream, fileUpload.getFileName());
    }

    /**
     * 上传文件并构建文件信息的结构体
     *
     * @param file 文件本体
     * @return 单个文件上传后的信息结构
     */
    public FileUpload processFile(MultipartFile file,boolean local) throws Exception {
        // 1. 检查文件非空
        if (file.isEmpty()) {
            throw new DataNotComplianceException("文件不可为空");
        }

        // 2. 处理文件名编码问题
        String originalFilename = decodeFileName(file.getOriginalFilename());
        
        // 3. 生成并验证文件名
        String fileName = Utils.generateUniqueFileName(originalFilename);

        // 4. 保存文件到minio
        String filePath = saveFileToServer(file, fileName);

        // 5. 构建并返回 FileUpload 对象
        return buildFileUpload(file, originalFilename, fileName, filePath,local);
    }

    /**
     * 解码文件名，处理URL编码问题
     * @param encodedFileName 编码后的文件名
     * @return 解码后的文件名
     */
    private String decodeFileName(String encodedFileName) {
        if (encodedFileName == null) {
            return null;
        }
        
        try {
            // 处理URL编码的文件名
            String decodedName = java.net.URLDecoder.decode(encodedFileName, StandardCharsets.UTF_8.name());
            logger.info("文件名解码: {} -> {}", encodedFileName, decodedName);
            return decodedName;
        } catch (Exception e) {
            logger.warn("文件名解码失败，使用原始文件名: {}", encodedFileName, e);
            return encodedFileName;
        }
    }

    /**
     * 根据id获得二进制文件，用于问答过程中本地上传文件预览.
     *
     * @param id 文件id
     * @return 根据文件信息构造的二进制文件
     */
    public ResponseEntity<Resource> getFile(String id) throws Exception {
        logger.info("getFileById {}", id);
        // 获取文件元数据
        FileUpload fileInfo = fileUploadInfoRepository.findById(id).orElseThrow(() -> new NotFoundException("文件信息不存在"));
        // 从Minio获取文件流
        InputStream fileStream = minioOperations.getFileStream(fileInfo.getFilePath());
        return Utils.exchangeInputStreamToResource(fileStream, fileInfo.getOriginalFileName());
    }

    /**
     * 文件下载链接(3分钟有效)
     *
     * @param fileId 文件id
     * @return 下载的url
     */
    public String getDownloadUrlFromTemp(String fileId) throws Exception{
        try {
            FileUpload file = fileUploadInfoRepository.findById(fileId).orElseThrow(() -> new NotFoundException("文件不存在"));
            String name = file.getFilePath();
            // 添加强制下载的响应头
            Map<String, String> reqParams = Utils.buildFileHeaders(file.getFileName(), FileHeaderGenerator.DOWNLOAD);
            logger.info(file.getOriginalFileName());
            String url = minioOperations.getDownloadUrl(name,downloadTimeout, reqParams);
            return Utils.exchangeFileUrl(url,local,minioProxy);
        }catch (NotFoundException e){
            return fileService.getDownloadUrl(fileId);
        }

    }

    /**
     * 在上传文件的返回值中，添加文件下载链接
     * @param fileUpload 文件信息
     * @throws Exception 文件不存在时抛出异常
     */
    public void getFileUrlByFileId(FileUpload fileUpload) throws Exception{
        String url = minioOperations.getDownloadUrl(fileUpload.getFilePath(),3600,null);
        fileUpload.setFileUrl(Utils.exchangeFileUrl(url,local,minioProxy));
    }

    /**
     * 根据id获取上传文件的minio路径
     * @param id 文件id
     * @return minio路径
     */
    public String getFileObjectName(String id) {
        FileUpload fileUpload = fileUploadInfoRepository.findById(id).orElseThrow(() -> new NotFoundException("文件不存在"));
        return fileUpload.getFilePath();
    }

    /**
     * 保存文件到服务器.
     *
     * @param file     需要保存的文件
     * @param fileName 文件名
     * @return 文件在minio的路径
     */
    private String saveFileToServer(MultipartFile file, String fileName) throws Exception {
        String name = minioOperations.createTempFileName(fileName);
        String contentType = file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType();
        try {
            minioOperations.uploadFile(name, file.getInputStream(), file.getSize(), contentType);
        } catch (Exception e) {
            logger.error("上传文件失败{}", name);
            //上传失败,文件删除回滚
            minioOperations.deleteFile(name);
            logger.info("文件上传失败，已删除回滚{}", name);
            throw new RuntimeException("上传文件失败");
        }
        logger.info("文件上传成功,文件名{},文件类型{},文件路径{}", fileName, file.getContentType(), name);
        return name;
    }

    /**
     * 构建上传后的文件信息结构.
     *
     * @param file     二进制文件
     * @param fileName 文件名
     * @param filePath 上传后的路径
     * @return 文件信息结构
     */
    private FileUpload buildFileUpload(MultipartFile file, String originalFileName, String fileName, String filePath,boolean local) {
        logger.info("构建文件信息结构,文件名{},文件类型{},文件路径{}", fileName, file.getContentType(), filePath.replace("\\", "/"));
        FileUpload fileUpload = new FileUpload();
        fileUpload.setOriginalFileName(originalFileName);
        fileUpload.setFileName(fileName);
        fileUpload.setFileType(file.getContentType());
        fileUpload.setFilePath(filePath.replace("\\", "/"));
        fileUpload.setUploadTime(Utils.getNowDate());
        fileUpload.setLocal(local);
        return fileUpload;
    }

    /**
     * 根据已存在的对象路径构建文件信息结构
     */
    public FileUpload buildFileUploadFromObject(String originalFileName, String objectKey, String contentType, long size, boolean local) {
        String normalizedPath = objectKey.replace("\\", "/");
        String fileName = normalizedPath.contains("/") ? normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1) : normalizedPath;
        logger.info("构建文件信息结构(已存在对象),文件名{},文件类型{},文件路径{}", fileName, contentType, normalizedPath);
        FileUpload fileUpload = new FileUpload();
        fileUpload.setOriginalFileName(originalFileName);
        fileUpload.setFileName(fileName);
        fileUpload.setFileType(contentType);
        fileUpload.setFilePath(normalizedPath);
        fileUpload.setUploadTime(Utils.getNowDate());
        fileUpload.setLocal(local);
        return fileUpload;
    }

    /**
     * 根据文件列表，将文件转换为pdf或pptx格式
     *
     * @param tasks 需要转换的文件列表
     */
    @Async
    public void covertUploadFiles(List<FileUpload> tasks){
        webClient.post()
                .uri(libreOfficeFileUploads)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(tasks)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

}
