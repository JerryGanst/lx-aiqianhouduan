package org.example.ai_api.Service.Common;

import org.example.ai_api.Bean.WebRequest.MultipartAbortRequest;
import org.example.ai_api.Bean.WebRequest.MultipartCompletedPart;
import org.example.ai_api.Bean.WebRequest.MultipartPartUrl;
import org.example.ai_api.Bean.WebRequest.MultipartPrepareResponse;
import org.example.ai_api.Exception.BadRequestException;
import org.example.ai_api.Utils.MinioOperations;
import org.example.ai_api.Utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MultipartUploadCore {

    @Autowired
    private MinioOperations minioOperations;
    @Value("${minio-proxy}")
    private String minioProxy;
    @Value("${local}")
    private String local;

    public long resolvePartSize(Long requestedPartSize, long fileSize) {
        if (fileSize <= 0) {
            throw new BadRequestException("size 非法");
        }
        long maxSize = 100L * 1024 * 1024;
        if (fileSize > maxSize) {
            throw new BadRequestException("单个文件大小不能超过100MB");
        }
        long defaultPartSize = 8L * 1024 * 1024; // 8MB
        long partSize = (requestedPartSize != null && requestedPartSize > 0) ? requestedPartSize : defaultPartSize;
        if (partSize < 5L * 1024 * 1024) {
            partSize = 5L * 1024 * 1024; // S3 最小分片大小（最后一片可小于）
        }
        return partSize;
    }

    public MultipartPrepareResponse prepareResponse(
            String objectKey,
            String filename,
            String contentType,
            long fileSize,
            long partSize,
            Integer expireSeconds
    ) throws Exception {
        int expiry = (expireSeconds != null && expireSeconds > 0) ? expireSeconds : 20 * 60;
        String ct = (contentType != null && !contentType.isEmpty()) ? contentType : "application/octet-stream";
        String uploadId = minioOperations.createMultipartUpload(
                objectKey,
                ct,
                null
        );
        if (uploadId == null || uploadId.isEmpty()) {
            throw new BadRequestException("初始化分片上传失败，请重试");
        }
        int partCount = (int) ((fileSize + partSize - 1) / partSize);
        partCount = Math.max(partCount, 1);
        List<MultipartPartUrl> parts = new ArrayList<>();
        for (int pn = 1; pn <= partCount; pn++) {
            String url = minioOperations.getPresignedUploadPartUrl(objectKey, uploadId, pn, expiry);
            url = Utils.exchangeFileUrl(url,local,minioProxy);
            parts.add(new MultipartPartUrl(pn, url));
        }
        return new MultipartPrepareResponse(uploadId, objectKey, partSize, partCount, parts);
    }

    public void maybeCompleteMultipart(String objectKey, String uploadId, List<MultipartCompletedPart> parts) {
        if (uploadId == null || uploadId.isEmpty() || parts == null || parts.isEmpty()) {
            return;
        }
        List<CompletedPart> completed = parts.stream()
                .sorted(Comparator.comparingInt(MultipartCompletedPart::getPartNumber))
                .map(p -> CompletedPart.builder()
                        .partNumber(p.getPartNumber())
                        .eTag(p.getEtag())
                        .build())
                .collect(Collectors.toList());
        minioOperations.completeMultipart(objectKey, uploadId, completed);
    }

    public String abort(MultipartAbortRequest request) {
        if (request.getUploadId() == null || request.getUploadId().isEmpty()
                || request.getObjectKey() == null || request.getObjectKey().isEmpty()) {
            throw new BadRequestException("uploadId 和 objectKey 不能为空");
        }
        minioOperations.abortMultipart(request.getObjectKey(), request.getUploadId());
        return "已取消";
    }

    public String buildContentDisposition(String filename) throws UnsupportedEncodingException {
        // 1) 回退名：仅 ASCII、去引号与控制字符，避免被中间件改写
        String fallback = asciiFallback(filename);

        // 2) RFC 5987 编码真实文件名（UTF-8 百分号编码；空格用 %20）
        String encoded = encodeRFC5987(filename != null ? filename : "download");

        // 3) 组合（attachment 可换 inline）
        return "attachment; filename=\"" + fallback + "\"; filename*=UTF-8''" + encoded;
    }

    private String asciiFallback(String name) {
        if (name == null || name.isEmpty()) return "download";
        // 先做兼容性分解，去掉音标等，再过滤非 ASCII
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFKD);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            // 过滤引号、反斜杠与控制字符，只保留可打印 ASCII
            if (c >= 0x20 && c <= 0x7E && c != '"' && c != '\\') {
                sb.append(c == ' ' ? '_' : c); // 空格换成下划线更稳
            }
        }
        String s = sb.toString().replaceAll("^\\.+", "").replaceAll("\\.+$", ""); // 去首尾点
        if (s.isEmpty()) s = "download";
        if (s.length() > 60) s = s.substring(0, 60); // 回退名不宜过长
        return s;
    }

    private String encodeRFC5987(String value) throws UnsupportedEncodingException {
        // URLEncoder 会把空格编码为 +，这里改成 %20；其余百分号编码符合要求
        // 可选：URLEncoder 会把 * 也编码为 %2A，RFC5987 允许 *，但编码不影响兼容性
        return URLEncoder.encode(value, String.valueOf(StandardCharsets.UTF_8))
                .replace("+", "%20");
    }
}

