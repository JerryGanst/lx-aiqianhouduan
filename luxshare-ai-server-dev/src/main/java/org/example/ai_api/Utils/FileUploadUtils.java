package org.example.ai_api.Utils;

import org.example.ai_api.Exception.DataNotComplianceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class FileUploadUtils {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadUtils.class.getName());

    @Autowired
    private MinioOperations minioOperations;

    /**
     * 前置检查,返回转换后的在服务器合法的文件名列表
     *
     * @param files  需要上传的文件列表
     * @param userId 上传用户的id
     * @return 上传文件在服务器合法的文件名列表
     */
    public List<String> checkBeforeUpload(List<MultipartFile> files, String userId) {
        logger.info("用户{}上传文件,进行前置检查", userId);
        //文件非空检查
        if (files == null || files.isEmpty()) {
            throw new DataNotComplianceException("文件列表为空");
        }
        files.forEach(file -> {
            if (file.isEmpty()) {
                throw new DataNotComplianceException("文件列表中存在空文件");
            }
        });
        //用户id非空检查
        if (userId == null || userId.isEmpty()) {
            throw new DataNotComplianceException("用户id为空");
        }
        //文件名非空检查
        files.forEach(file -> {
            if (file.getOriginalFilename() == null || file.getOriginalFilename().isEmpty()) {
                throw new DataNotComplianceException("文件列表中存在文件名为空的文件");
            }
        });
        //文件名转换，包含文件名转换和文件扩展名转换
        return files.stream()
                .map(file -> Utils.renameFileToUbuntu(Objects.requireNonNull(file.getOriginalFilename())))
                .collect(Collectors.toList());
    }

    /**
     * 上传报错时，清除已上传文件
     *
     * @param fileUploads 已上传文件名
     */
    public void deleteUploadedFiles(List<String> fileUploads) {
        logger.info("文件上传过程出错，开始回滚所有上传文件");
        for (String fileName : fileUploads) {
            try {
                minioOperations.deleteFile(fileName);
            } catch (Exception e) {
                // 处理删除异常（例如记录日志）
                logger.info("回滚删除文件{}失败: {}", fileName, e.getMessage());
            }
        }
    }
}
