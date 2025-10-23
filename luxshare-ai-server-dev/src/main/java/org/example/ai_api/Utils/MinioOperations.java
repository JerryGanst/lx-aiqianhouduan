package org.example.ai_api.Utils;

import io.minio.*;
import io.minio.http.Method;
import org.example.ai_api.Bean.Enum.FileHeaderGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import io.minio.ListObjectsArgs;
import io.minio.Result;
import io.minio.messages.Item;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;

/**
 * minio相关操作封装
 */
@Component
public class MinioOperations {
    private static final Logger logger = LoggerFactory.getLogger(MinioOperations.class);
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private S3Client s3;
    @Autowired
    private S3Presigner presigner;
    @Value("${minio.bucketName}")
    private String bucketName;

    /**上传文件到minio
     *
     * @param name minio文件名
     * @param stream 文件流
     * @param size 文件大小
     * @param contentType 文件类型
     * @throws Exception 抛出错误
     */
    public void uploadFile(String name, InputStream stream, long size, String contentType) throws Exception{
        uploadFile(bucketName, name, stream, size, contentType);
    }

    /**上传文件到minio
     *
     * @param customBucketName 自定义桶名
     * @param name minio文件名
     * @param stream 文件流
     * @param size 文件大小
     * @param contentType 文件类型
     * @throws Exception 抛出错误
     */
    public void uploadFile(String customBucketName, String name, InputStream stream, long size, String contentType) throws Exception{
        String finalBucketName = customBucketName != null && !customBucketName.isEmpty() ? customBucketName : this.bucketName;
        logger.info("上传文件到minio，桶名{},文件名{},文件类型{},文件大小{}", finalBucketName, name, contentType, size);
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(finalBucketName)
                        .object(name)
                        .stream(stream, size, -1)
                        .contentType(contentType)
                        .build()
        );
    }

    /** 从minio删除文件
     *
     * @param name minio文件名
     * @throws Exception 抛出错误
     */
    public void deleteFile(String name) throws Exception{
        deleteFile(bucketName, name);
    }

    /** 从minio删除文件
     *
     * @param customBucketName 自定义桶名
     * @param name minio文件名
     * @throws Exception 抛出错误
     */
    public void deleteFile(String customBucketName, String name) throws Exception{
        String finalBucketName = customBucketName != null && !customBucketName.isEmpty() ? customBucketName : this.bucketName;
        logger.info("从minio删除文件，桶名{},文件名{}", finalBucketName, name);
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(finalBucketName)
                        .object(name)
                        .build()
        );
    }

    /**获得minio文件下载链接
     *
     * @param name minio文件名
     * @param time 链接可用时长(以秒为单位)
     * @param reqParams 定义请求头参数
     * @return 下载链接
     * @throws Exception 抛出错误
     */
    public String getDownloadUrl(String name, int time, Map<String, String> reqParams) throws Exception{
        return getDownloadUrl(bucketName, name, time, reqParams);
    }

    /**
     * 获得minio文件下载链接（自定义桶名）
     *
     * @param customBucket 桶名
     * @param name minio文件名
     * @param time 链接可用时长(以秒为单位)
     * @param reqParams 定义请求头参数
     * @return 下载链接
     * @throws Exception 抛出错误
     */
    public String getDownloadUrl(String customBucket, String name, int time, Map<String, String> reqParams) throws Exception{
        logger.info("获得minio文件下载链接，桶名{},文件名{},链接可用时长{}秒",customBucket, name, time);
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(customBucket)
                        .object(name)
                        .expiry(time, TimeUnit.SECONDS)
                        .extraQueryParams(reqParams)
                        .build()
        );
    }

    /** 获得minio文件流
     *
     * @param name minio文件名
     * @return 文件流
     * @throws Exception 抛出错误
     */
    public InputStream getFileStream(String name) throws Exception{
        return getFileStream(bucketName, name);
    }

    /** 获得minio文件流
     *
     * @param customBucketName 自定义桶名
     * @param name minio文件名
     * @return 文件流
     * @throws Exception 抛出错误
     */
    public InputStream getFileStream(String customBucketName, String name) throws Exception{
        String finalBucketName = customBucketName != null && !customBucketName.isEmpty() ? customBucketName : this.bucketName;
        logger.info("获得minio文件流，桶名{},文件名{}", finalBucketName, name);
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(finalBucketName)
                        .object(name)
                        .build()
        );
    }

    /** 获得minio文件元数据
     *
     * @param name minio文件名
     * @return 文件元数据
     * @throws Exception 抛出错误
     */
    public StatObjectResponse getObjectStat(String name) throws Exception{
        return getObjectStat(bucketName, name);
    }

    /** 获得minio文件元数据
     *
     * @param customBucketName 自定义桶名
     * @param name minio文件名
     * @return 文件元数据
     * @throws Exception 抛出错误
     */
    public StatObjectResponse getObjectStat(String customBucketName, String name) throws Exception{
        String finalBucketName = customBucketName != null && !customBucketName.isEmpty() ? customBucketName : this.bucketName;
        logger.info("获得minio文件元数据，桶名{},文件名{}", finalBucketName, name);
        return minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(finalBucketName)
                        .object(name)
                        .build()
        );
    }

    /**
     * 根据相关信息生成知识库文件在minio的文件名
     *
     * @param fileName 原文件名
     * @param target   目标领域
     * @param userId   上传者id
     * @param folderId 文件夹id (新增)
     * @param isPublic 是否公有
     * @return 知识库文件在minio的文件名
     */
    public String createKnowledgeFileName(String fileName, String target, String userId, String folderId, boolean isPublic){ // 添加 folderId 参数
        logger.info("根据相关信息生成知识库文件在minio的文件名,文件名{},目标领域{},上传者id{},文件夹id{},是否公有{}", fileName, target, userId, folderId, isPublic); // 更新日志
        String prefix = isPublic ? "public/" : "private/";
        String normalizedPath;
        if (isPublic) {
            normalizedPath = target.endsWith("/") ? target : target + "/";
        } else {
            // 私有文件，如果 folderId 不为空，则在路径中包含 folderId
            if (folderId != null && !folderId.isEmpty()) {
                normalizedPath = userId + "/" + folderId + "/"; // 添加 folderId
            } else {
                normalizedPath = userId.endsWith("/") ? userId : userId + "/";
            }
        }
        return prefix + normalizedPath + fileName;
    }

    /**
     *  根据相关信息生成部门文件在minio的文件名
     * @param fileName  文件原名
     * @param departmentId  部门id
     * @return  部门文件在minio的文件名
     */
    public String createDepartmentFileName(String fileName, String departmentId, String folderId){
        logger.info("根据相关信息生成部门文件在minio的文件名,文件名{},部门id{},文件夹id{}", fileName, departmentId, folderId);
        return "department/" + departmentId + "/" + folderId + "/" + fileName;
    }

    /**
     * 创建临时文件在minio的文件名
     * @param fileName 文件原名
     * @return 临时文件在minio的文件名
     */
    public String createTempFileName(String fileName){
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return "tmp" + "/" + dateDir + "/" + fileName;
    }

    /**
     * 根据minio文件名获取base64编码字符串
     * @param name minio文件名
     * @param contentType 文件类型（如 image/jpeg）
     * @return base64字符串，带data:前缀
     */
    public String getFileBase64(String name, String contentType) throws Exception {
        try (InputStream minioStream = getFileStream(name)) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = minioStream.read(buffer)) != -1) {
                stream.write(buffer, 0, len);
            }
            String base64 = Base64.getEncoder().encodeToString(stream.toByteArray());
            return "data:" + contentType + ";base64," + base64;
        }
    }

    /**
     * 检查Minio桶是否存在
     * @param bucketName 桶名
     * @return 如果桶存在返回true，否则返回false
     * @throws Exception 抛出错误
     */
    public boolean bucketExists(String bucketName) throws Exception {
        logger.info("检查Minio桶是否存在，桶名: {}", bucketName);
        return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
    }

    /**
     * 检查默认桶中是否存在文件
     * @param objectName 文件名
     * @return 如果文件存在返回true，否则返回false
     * @throws Exception 抛出错误
     */
    public boolean objectExists(String objectName) throws Exception{
        return objectExists(bucketName, objectName);
    }

    /**
     * 检查某个桶中是否存在文件
     * @param bucketName 桶名
     * @param objectName 文件名
     * @return 如果文件存在返回true，否则返回false
     * @throws Exception 抛出错误
     */
    public boolean objectExists(String bucketName, String objectName) throws Exception {
        logger.info("检查Minio桶{}中文件是否存在，文件名: {}", bucketName, objectName);
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return true; // 文件存在
        } catch (io.minio.errors.ErrorResponseException e) {
            // 如果错误码是NoSuchKey，表示文件不存在
            if (e.errorResponse().code().equals("NoSuchKey")) {
                logger.warn("Minio桶{}中文件{}不存在", bucketName, objectName);
                return false;
            }
            throw e; // 抛出其他异常
        }
    }

    /**
     * 将某个桶中的对象复制到另一个桶中
     * @param sourceBucket 源桶名
     * @param sourceObject 源对象名
     * @param destinationBucket 目标桶名
     * @param destinationObject 目标对象名
     * @throws Exception 抛出错误
     */
    public void copyObject(String sourceBucket, String sourceObject, String destinationBucket, String destinationObject) throws Exception {
        logger.info("将Minio桶{}中的文件{}复制到桶{}，文件名为{}", sourceBucket, sourceObject, destinationBucket, destinationObject);
        minioClient.copyObject(
                CopyObjectArgs.builder()
                        .source(
                                CopySource.builder()
                                        .bucket(sourceBucket)
                                        .object(sourceObject)
                                        .build()
                        )
                        .bucket(destinationBucket)
                        .object(destinationObject)
                        .build()
        );
    }

    /**
     * 根据传入的桶名新建一个桶
     * @param bucketName 桶名
     * @throws Exception 抛出错误
     */
    public void createBucket(String bucketName) throws Exception {
        logger.info("尝试创建Minio桶，桶名: {}", bucketName);
        // 检查桶是否已存在
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            // 如果桶不存在，则创建
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            logger.info("Minio桶 {} 创建成功", bucketName);
        } else {
            logger.warn("Minio桶 {} 已存在，无需重复创建", bucketName);
        }
    }

    /**
     * 列出指定桶和前缀下的所有对象。
     * @param bucketName 桶名
     * @param prefix 对象名前缀（例如：某个sessionID作为目录）
     * @return 匹配的对象列表
     * @throws Exception 抛出错误
     */
    public List<Item> listObjects(String bucketName, String prefix) throws Exception {
        logger.info("列出Minio桶{}中前缀为{}的对象", bucketName, prefix);
        List<Item> objects = new ArrayList<>();
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(prefix)
                        .recursive(true) // 递归列出子目录中的对象
                        .build()
        );
        for (Result<Item> result : results) {
            objects.add(result.get());
        }
        return objects;
    }

    /**
     * 判断 Minio 预签名 URL 是否过期或无效，用于决定是否需要刷新。
     * @param url Minio 预签名 URL
     * @return 如果链接过期或无法提取参数，返回 true（表示需要刷新）；否则返回 false（表示不需要刷新）
     */
    public boolean isMinioUrlExpiredOrInvalid(String url) {
        if (url == null || url.isEmpty()) {
            return true; // URL 为空或无效，需要刷新
        }
        try {
            // 使用 UriComponentsBuilder 解析 URL
            // 从 URL 中解析查询参数
            // `UriComponentsBuilder` 会自动处理 URL 编码和参数的键值对解析
            Map<String, String> queryParams = org.springframework.web.util.UriComponentsBuilder.fromUriString(url)
                    .build()
                    .getQueryParams()
                    .toSingleValueMap();

            String amzDate = queryParams.get("X-Amz-Date");
            String amzExpires = queryParams.get("X-Amz-Expires");

            if (amzDate == null || amzExpires == null) {
                return true; // 缺少日期或过期时间参数，视为无效，需要刷新
            }

            // 解析日期和过期秒数
            // X-Amz-Date 格式为 YYYYMMDDTHHMMSSZ
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
            ZonedDateTime signedTime = ZonedDateTime.parse(amzDate, formatter.withZone(ZoneOffset.UTC));
            long expiresInSeconds = Long.parseLong(amzExpires);

            // 计算过期时间
            Instant expirationTime = signedTime.toInstant().plusSeconds(expiresInSeconds);

            // 判断是否过期
            return Instant.now().isAfter(expirationTime);

        } catch (Exception e) {
            logger.warn("判断 Minio URL 过期状态失败: {}，将视为需要刷新。", e.getMessage());
            return true; // 发生异常，视为需要刷新
        }
    }


    /** 用 SDK 发起 CreateMultipartUpload，并在此处设置对象的 Content-Type（可选 Content-Disposition） */
    public String createMultipartUpload(String objectKey, String contentType, String contentDisposition) {
        if(contentDisposition == null || contentDisposition.isEmpty()){
            contentDisposition = null;
        }
        CreateMultipartUploadResponse resp =
                s3.createMultipartUpload(CreateMultipartUploadRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .contentType((contentType != null && !contentType.isEmpty()) ? contentType : "application/octet-stream")
                        .contentDisposition(contentDisposition)
                        .build());
        return resp.uploadId();
    }

    /** 预签名分片 PUT，注意不要把 Content-Type 放进签名或请求头 */
    public String getPresignedUploadPartUrl(String objectKey, String uploadId, int partNumber, int expirySeconds) {
        PresignedUploadPartRequest p = presigner.presignUploadPart(b -> b
                .signatureDuration(Duration.ofSeconds(expirySeconds))
                .uploadPartRequest(UploadPartRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .build()));
        return p.url().toString();
    }

    /** 可选：完成合并也走 AWS SDK（你已有实现就沿用即可） */
    public void completeMultipart(String objectKey, String uploadId, List<CompletedPart> parts) {
        s3.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder().parts(parts).build())
                .build());
    }

    /**
     * 取消未完成的分片上传（幂等）。
     * 如果 uploadId 已不存在/已被取消，MinIO 可能返回 NoSuchUpload，此处吞掉视为成功。
     */
    public void abortMultipart(String objectKey, String uploadId) {
        if (objectKey == null || objectKey.isEmpty()) {
            throw new IllegalArgumentException("objectKey 不能为空");
        }
        if (uploadId == null || uploadId.isEmpty()) {
            throw new IllegalArgumentException("uploadId 不能为空");
        }
        try {
            s3.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .uploadId(uploadId)
                    .build());
            logger.info("Abort multipart upload success. objectKey={}, uploadId={}", objectKey, uploadId);
        } catch (S3Exception e) {
            // 幂等：已不存在时返回 NoSuchUpload，视为成功
            if ("NoSuchUpload".equalsIgnoreCase(e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : null)) {
                logger.warn("Abort ignored (NoSuchUpload). objectKey={}, uploadId={}", objectKey, uploadId);
                return;
            }
            logger.error("Abort multipart upload failed. objectKey={}, uploadId={}, err={}",
                    objectKey, uploadId, e.getMessage(), e);
            throw e; // 交由上层统一异常处理
        }
    }

    public String getDownloadUrlByConvertPath(String convertPath,String fileName,int downloadTimeout,String local,String minioProxy) throws Exception {
        Map<String,String> reqParam = Utils.buildFileHeaders(fileName, FileHeaderGenerator.DOWNLOAD);
        String url = getDownloadUrl(convertPath,downloadTimeout,reqParam);
        return Utils.exchangeFileUrl(url,local,minioProxy);
    }
}
