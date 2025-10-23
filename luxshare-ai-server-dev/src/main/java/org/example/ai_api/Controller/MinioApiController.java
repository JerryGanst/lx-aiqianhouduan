package org.example.ai_api.Controller;

import org.example.ai_api.Bean.Entity.SessionFile;
import org.example.ai_api.Bean.Model.FileDownloadResponse;
import org.example.ai_api.Bean.Model.MCPFileInfo;
import org.example.ai_api.Bean.Model.ResultData;
import org.example.ai_api.Bean.WebRequest.DownloadSessionFile;
import org.example.ai_api.Exception.BadRequestException;
import org.example.ai_api.Service.MinioApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * 暴露给mcp服务器的minio操作接口
 * @author 10353965
 */
@RestController
@RequestMapping("/minio")
public class MinioApiController {
    @Autowired
    private MinioApiService minioApiService;

    /**
     * 列出知识库文件的所有文件
     * @param userId 用户id
     * @param fileTypes 文件类型
     * @return 文件列表
     */
    @GetMapping("/listUserFiles")
    public ResultData<List<MCPFileInfo>> list(@RequestParam("userId") String userId, @RequestParam(value = "fileTypes",required = false) List<String> fileTypes) {
        if (userId == null || userId.isEmpty()) {
            throw new BadRequestException("userId不能为空");
        }
        return ResultData.success(minioApiService.listFiles(userId, fileTypes));
    }

    /**
     * 列出指定会话的所有文件
     * @param userId 用户id
     * @param sessionId 会话id
     * @return 文件列表
     */
    @GetMapping("/listSessionFiles")
    public ResultData<List<MCPFileInfo>> listSessionFiles(@RequestParam("userId") String userId, @RequestParam("sessionId") String sessionId) {
        if (userId == null || userId.isEmpty()) {
            throw new BadRequestException("userId不能为空");
        }
        if (sessionId == null || sessionId.isEmpty()) {
            throw new BadRequestException("sessionId不能为空");
        }
        List<MCPFileInfo> sessionFiles = minioApiService.listFilesBySessionId(userId, sessionId);
        return ResultData.success(sessionFiles);
    }

    /**
     * 根据文件名下载文件
     * @param downloadSessionFile 下载请求
     * @return 文件下载链接
     * @throws Exception 异常
     */
    @PostMapping("/downloadFile")
    public ResultData<FileDownloadResponse> downloadFile(@RequestBody DownloadSessionFile downloadSessionFile) throws Exception {
        if (downloadSessionFile == null) {
            throw new BadRequestException("请求体不能为空");
        }
        FileDownloadResponse response = minioApiService
                .downloadFile(
                        downloadSessionFile.getUserId(),
                        downloadSessionFile.getSessionId(),
                        downloadSessionFile.getFileName(),
                        downloadSessionFile.getIteration(),
                        downloadSessionFile.getFolderName()
                );
        return ResultData.success("获取成功",response);
    }

    /**
     * 上传文件
     * @param userId 用户id
     * @param sessionId 会话id
     * @param iteration 对话轮数
     * @param fileName 文件名
     * @param file 文件
     * @return 完成上传的文件信息
     * @throws Exception 异常
     */
    @PostMapping("/uploadFile")
    public ResultData<SessionFile> uploadFile(
            @RequestParam("userId") String userId,
            @RequestParam("sessionId") String sessionId,
            @RequestParam("iteration") int iteration,
            @RequestParam("folderName") String folderName,
            @RequestParam("fileName") String fileName,
            @RequestPart("file") MultipartFile file
    ) throws Exception {
        if (userId == null || userId.isEmpty()) {
            throw new BadRequestException("userId不能为空");
        }
        if (sessionId == null || sessionId.isEmpty()) {
            throw new BadRequestException("sessionId不能为空");
        }
        if (fileName == null || fileName.isEmpty()) {
            throw new BadRequestException("fileName不能为空");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("文件内容不能为空");
        }
        if (iteration == 0) {
            throw new BadRequestException("iteration不可为0");
        }
        SessionFile result = minioApiService.uploadFile(userId, sessionId, fileName, iteration, folderName ,file);
        return ResultData.success("文件上传成功",result);
    }

    @PostMapping("/uploadExcelTranslate")
    public ResultData<String> uploadExcelTranslate(@RequestParam("sessionId") String sessionId,@RequestPart("file") MultipartFile file) throws Exception {
        String objectName = minioApiService.uploadExcelTranslated(sessionId,file);
        return ResultData.success("上传成功",objectName);
    }
}
