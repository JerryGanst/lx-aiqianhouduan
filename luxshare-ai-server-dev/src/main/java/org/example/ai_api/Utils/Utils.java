package org.example.ai_api.Utils;


import org.example.ai_api.Bean.Enum.FileHeaderGenerator;
import org.example.ai_api.Bean.Model.DateRange;
import org.example.ai_api.Exception.DataNotComplianceException;
import org.example.ai_api.Exception.BadRequestException;
import org.example.ai_api.Exception.NotFoundException;
import org.example.ai_api.Exception.ThirdServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 自定义通用工具类.
 */
public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
    @Value("${spring.profiles.active}")
    private static String activeProfile;
    private static final String invalidCharsPattern = "[ ()【】《》\"'\\[\\]{}<>!@#$%^&*+=|\\\\/,]";
    // 本地时间格式：yyyy-MM-dd HH:mm:ss（隐含 GMT+8）
    private static final DateTimeFormatter LOCAL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // UTC 时间格式：ISO 8601（如 2025-05-18T08:00:00Z）
    private static final DateTimeFormatter UTC_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /**
     * 本地时间字符串（GMT+8） → UTC 时间字符串（带 Z 的 ISO 格式）
     * @param localTimeStr 格式必须为 yyyy-MM-dd HH:mm:ss
     * @return UTC 时间字符串，如 2025-05-18T08:00:00Z
     */
    public static String localToUTC(String localTimeStr) {
        // 解析为 LocalDateTime（无时区）
        LocalDateTime localDateTime = LocalDateTime.parse(localTimeStr, LOCAL_FORMATTER);
        // 附加 GMT+8 时区信息
        ZonedDateTime beijingTime = localDateTime.atZone(ZoneId.of("GMT+8"));
        // 转换为 UTC 时区
        ZonedDateTime utcTime = beijingTime.withZoneSameInstant(ZoneOffset.UTC);
        // 格式化为 ISO 字符串（带 Z）
        return utcTime.format(UTC_FORMATTER);
    }

    /**
     * UTC 时间字符串（带 Z 的 ISO 格式） → 本地时间字符串（GMT+8）
     * @param utcTimeStr 格式必须为 ISO 8601（如 2025-05-18T08:00:00Z）
     * @return 本地时间字符串，如 2025-05-18 16:00:00
     */
    public static String utcToLocal(String utcTimeStr) {
        // 直接解析为带时区的 ZonedDateTime（自动识别 Z 表示 UTC）
        ZonedDateTime utcTime = ZonedDateTime.parse(utcTimeStr, UTC_FORMATTER);
        // 转换为 GMT+8 时区
        ZonedDateTime beijingTime = utcTime.withZoneSameInstant(ZoneId.of("GMT+8"));
        // 格式化为本地时间字符串
        return beijingTime.format(LOCAL_FORMATTER);
    }
    /**
     * 以字符串返回当前时间.
     */
    public static String getNowDate(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }

    /**
     * 生成带后缀的文件名.
     */
    public static String generateUniqueFileName(String originalFileName) {
        String uuid = UUID.randomUUID().toString();
        if (originalFileName == null) {
            return uuid;
        }
        String baseName = originalFileName;
        String extension = null;
        int lastDotIndex = originalFileName.lastIndexOf('.');
        // 处理基础名和扩展名
        if (lastDotIndex != -1) {
            baseName = originalFileName.substring(0, lastDotIndex);
            extension = originalFileName.substring(lastDotIndex + 1);
        }
        // 构建新文件名
        String newName;
        if (extension != null) {
            newName = baseName + "_" + uuid + "." + extension;
        } else {
            newName = baseName + "_" + uuid;
        }
        // 非法字符校验
        if (newName.matches(".*[\\\\/:*?\"<>|].*")) {
            throw new DataNotComplianceException("文件名包含非法字符");
        }
        return newName;
    }

    /**
     * 获取用户ip.
     */
    public static String getIP(HttpServletRequest request){
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果经过了多层代理，X-Forwarded-For 的值会以逗号分隔多个IP，取第一个非 unknown 的IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0];
        }
        return ip;
    }

    /**
     * 获得文件拓展名.
     */
    public static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == 0) {
            throw new IllegalArgumentException("无效文件名: " + fileName);
        }
        return fileName.substring(dotIndex + 1);
    }

    /**
     * 校验路径合法性.
     */
    public static Path validateFilePath(String inputPath,String uploadBaseDir) throws IOException {
        // 转换为绝对路径并标准化
        Path basePath = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
        Path resolvedPath = basePath.resolve(inputPath).normalize().toAbsolutePath();
        // 校验路径合法性
        if (!resolvedPath.startsWith(basePath)) {
            throw new SecurityException("文件路径越界");
        }
        if (!Files.exists(resolvedPath)) {
            throw new NotFoundException("文件不存在");
        }
        if (!Files.isReadable(resolvedPath)) {
            throw new AccessDeniedException("文件不可读");
        }
        if (Files.isDirectory(resolvedPath)) {
            throw new SecurityException("路径指向目录");
        }
        return resolvedPath;
    }

    /**
     *  判断是否是流式请求.
     */
    public static boolean isStreamRequest(WebRequest request) {
        String acceptHeader = request.getHeader("Accept");
        return acceptHeader != null && acceptHeader.contains("text/event-stream");
    }

    /**
     * 获得一天的起止时间
     */
    public static DateRange getDateRange(String dateStr){
        LocalDate localDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
        LocalDateTime startLocal = localDate.atStartOfDay(); // GMT+8 00:00:00
        LocalDateTime endLocal = startLocal.plusDays(1);    // GMT+8 次日 00:00:00

        Instant startUtc = startLocal.atZone(ZoneId.of("GMT+8")).toInstant();
        Instant endUtc = endLocal.atZone(ZoneId.of("GMT+8")).toInstant();

        DateRange dateRange = new DateRange();
        dateRange.setStart(startUtc);
        dateRange.setEnd(endUtc);
        return dateRange;
    }

    /**
    * 判断是否是windows系统
    */
    public static boolean isWindows(){
        String os = System.getProperty("os.name");
        return os.toLowerCase().startsWith("win");
    }

    /**
     * 将文件重命名为ubuntu可读的文件名
     */
    public static String renameFileToUbuntu(String fileName){
        return fileName.replaceAll(invalidCharsPattern, "_").replaceAll("_+", "_");
    }

    public static String changeFileExtension(String fileName){
        // 分割文件名和扩展名
        int lastDotIndex = fileName.lastIndexOf('.');
        String mainPart = (lastDotIndex == -1) ? fileName : fileName.substring(0, lastDotIndex);
        String extension = (lastDotIndex == -1) ? "" : fileName.substring(lastDotIndex + 1);
        // 根据扩展名设置新后缀
        String newExtension;
        if (extension.equalsIgnoreCase("ppt")) {
            // .ppt 转为 .pptx
            newExtension = "pptx";
        } else {
            // 其他情况转为 .pdf
            newExtension = "pdf";
        }
        // 组合新文件名
        return mainPart + "." + newExtension;
    }

    public static byte[] LibreOfficeFileConverter(MultiValueMap<String, Object> body, RestTemplate restTemplate, String readyUrl, String convertUrl, Logger logger) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA); // 会自动生成 boundary
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Void> readyResponse = restTemplate.getForEntity(readyUrl, Void.class);
            if (readyResponse.getStatusCode() != HttpStatus.OK){
                throw new ThirdServiceException("LibreOffice工具不可用");
            }
            // 接收响应后保存到指定路径将文件返回
            return restTemplate.postForEntity(convertUrl, request, byte[].class).getBody();
        }catch (Exception e){
            logger.error("LibreOffice工具不可用", e);
            throw e;
        }
    }

    /**
     * 读取文件hash
     * @param file 文件
     * @return 文件hash
     * @throws IOException 读取过程中的IO异常
     */
    public static String getHash(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        return DigestUtils.sha256Hex(bytes);
    }

    /**
     * 校验上传文件扩展名是否在白名单内
     * @param files 待校验文件列表
     * @param allowedExtensions 支持的扩展名集合（不区分大小写）
     */
    public static void validateUploadExtensions(List<MultipartFile> files, Collection<String> allowedExtensions) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("文件不能为空");
        }
        if (allowedExtensions == null || allowedExtensions.isEmpty()) {
            throw new BadRequestException("未配置允许的文件格式");
        }
        // 统一转小写，便于对比
        Set<String> allowLower = new HashSet<>();
        for (String ext : allowedExtensions) {
            if (ext != null) allowLower.add(ext.toLowerCase());
        }
        for (MultipartFile file : files) {
            String originalName = file.getOriginalFilename();
            if (originalName == null || !originalName.contains(".")) {
                throw new BadRequestException("文件缺少扩展名，允许格式：" + String.join(",", allowLower));
            }
            String ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
            if (!allowLower.contains(ext)) {
                throw new BadRequestException("不支持的文件格式：" + ext + "，允许格式：" + String.join(",", allowLower));
            }
        }
    }

    /**
     * 将文件流转换为 Resource 对象方便提交前端预览
     * @param fileStream 文件流
     * @param originalName 文件名
     * @return 转换的Resource对象
     * @throws Exception 任何异常
     */
    public static ResponseEntity<Resource> exchangeInputStreamToResource(InputStream fileStream,String originalName)throws Exception{
        Resource resource = new InputStreamResource(fileStream);
        String name = URLEncoder.encode(originalName, StandardCharsets.UTF_8.name())
                .replaceAll("\\+", "%20");
        MediaType mediaType = MediaTypeFactory.getMediaType(originalName)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .contentType(mediaType)
                .body(resource);
    }

    /**
     * 检查字符串列表中是否有重复元素
     * @param Strings 字符串列表
     * @return 是否有重复
     */
    public static boolean checkDuplicateStringInList(List<String> Strings){
        if (Strings == null) {
            throw new IllegalArgumentException("列表不能为null");
        }
        // 空列表或单元素列表直接返回无重复
        if (Strings.size() <= 1) {
            return false;
        }
        Set<String> seen = new HashSet<>();
        for (String s : Strings) {
            // 如果添加失败（已存在），说明有重复
            if (!seen.add(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 移除文件扩展名
     * @param fileName 文件名
     * @return 移除扩展名后的文件名
     */
    public static String removeFileExtension(String fileName){
        int dotIndex = fileName.lastIndexOf('.');
        // 检查点是否存在，并且不在首尾边界位置
        if (dotIndex > 0) {  // 仅处理常规情况：点不在开头
            return fileName.substring(0, dotIndex);
        }
        // 无后缀或非常规情况时返回原文件名
        return fileName;
    }

    /**
     * 手动分页实现
     * @param page 页码
     * @param size 每页数量
     * @param allFiles 所有文件
     * @return Page<KnowledgeFileInfo>
     */
    public static <T>Page<T> getFilesPage(int page, int size, List<T> allFiles) {
        int total = allFiles.size();
        int start = Math.min(page * size, total);
        int end = Math.min(start + size, total);

        List<T> pageFiles = allFiles.subList(start, end);
        return new PageImpl<>(
                pageFiles,
                PageRequest.of(page, size),
                total
        );
    }

    /**
     * 构建用于 MinIO 预签名 URL 的响应头参数（安全处理文件名，避免 400/签名错误）。
     * 不对 filename 进行手工 URL 编码，交由 SDK 统一处理查询参数编码。
     */
    public static Map<String, String> buildFileHeaders(String fileName, FileHeaderGenerator mode) {
        Map<String, String> reqParams = new HashMap<>();
        String safeFileName = sanitizeForContentDisposition(fileName);
        ContentDisposition disposition = ContentDisposition
                .builder(mode.getDispositionType())
                .filename(safeFileName, StandardCharsets.UTF_8)
                .build();
        reqParams.put("response-content-disposition", disposition.toString());
        reqParams.put("response-content-type", ContentTypeDetector.getContentType(fileName));
        return reqParams;
    }

    /**
     * 供 Content-Disposition 使用的文件名清洗，避免注入和异常字符。
     */
    private static String sanitizeForContentDisposition(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "file";
        }
        String cleaned = fileName.replaceAll("[\r\n]", " ").replace('"', '\'');
        int maxLen = 180; // 预留协议及编码空间
        if (cleaned.length() > maxLen) {
            int dot = cleaned.lastIndexOf('.');
            if (dot > 0 && dot < cleaned.length() - 1) {
                String base = cleaned.substring(0, dot);
                String ext = cleaned.substring(dot);
                if (base.length() > (maxLen - ext.length())) {
                    base = base.substring(0, Math.max(1, maxLen - ext.length()));
                }
                cleaned = base + ext;
            } else {
                cleaned = cleaned.substring(0, maxLen);
            }
        }
        return cleaned;
    }

    /**
     * 获得预览或下载响应头
     * @param fileName 文件名
     * @return 预览或下载响应头
     * @throws Exception 异常
     */
    public static Map<String, String> getFileHeaders(String fileName, FileHeaderGenerator mode) throws Exception {
        Map<String, String> reqParams = new HashMap<>();
        // 从枚举获取处置类型字符串
        reqParams.put("response-content-disposition",
                mode.getDispositionType() + "; filename=\"" + URLEncoder.encode(fileName, "UTF-8") + "\"");
        // 获取并设置内容类型
        reqParams.put("response-content-type", ContentTypeDetector.getContentType(fileName));
        return reqParams;
    }

    /**
     * 将文件链接转换为minio代理链接
     * @param url 原始链接
     * @param local minio本地链接
     * @param minioProxy minio代理链接
     * @return 转换后的代理链接
     */
    public static String exchangeFileUrl(String url,String local,String minioProxy){
        if ("prod".equals(activeProfile)) {
            logger.info("Current profile is prod, returning original url: {}", url);
            return url;
        }
        return url.replace(local, minioProxy);
    }

    /**
     * 从文件路径中提取文件名（包括扩展名）
     * @param filePath 文件完整路径
     * @return 文件名（例如 "example.txt"）
     */
    public static String getFileNameFromPath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        int lastSlashIndex = filePath.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            return filePath; // 没有斜杠，整个路径就是文件名
        }
        return filePath.substring(lastSlashIndex + 1);
    }

    /**
     * 将Map或List<Map>中的 _id 字段转为字符串类型的自定义字段，并可选择移除原始 _id 字段
     * @param data 查询结果（单个Map或List<Map>）
     * @param idFieldName 转换后的字段名（如 "id"、"chatId" 等）
     * @param removeOriginId 是否移除原始 _id 字段
     */
    public static void convertMongoIdToStringId(Object data, String idFieldName, boolean removeOriginId) {
        if (data == null) return;
        
        if (data instanceof List) {
            // 处理List<Map>
            List<Map> list = (List<Map>) data;
            for (Map<String, Object> map : list) {
                convertMongoIdToStringId(map, idFieldName, removeOriginId);
            }
        } else if (data instanceof Map) {
            // 处理单个Map
            Map<String, Object> map = (Map<String, Object>) data;
            Object _id = map.get("_id");
            if (_id instanceof org.bson.types.ObjectId) {
                map.put(idFieldName, _id.toString());
            } else if (_id != null) {
                map.put(idFieldName, _id.toString());
            }
            if (removeOriginId) {
                map.remove("_id");
            }
        }
    }
}
