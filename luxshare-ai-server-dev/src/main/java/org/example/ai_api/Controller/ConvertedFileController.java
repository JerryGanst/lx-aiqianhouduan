package org.example.ai_api.Controller;

import org.example.ai_api.Bean.Entity.PublicFileResult;
import org.example.ai_api.Service.PublicFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author 10353965
 */
@RestController
@RequestMapping("/ConvertedFile")
public class ConvertedFileController {

    private static final Logger logger = LoggerFactory.getLogger(ConvertedFileController.class);

    @Autowired
    private PublicFileService publicFileService;
    /**
     * @description 根据 ID 获取单个转换后的文件结果 (保留原有接口)
     *
     * @param id PublicFileResult 的 ID
     * @return 对应的 PublicFileResult
     */
    @GetMapping("/{id}")
    public ResponseEntity<PublicFileResult> getPublicFileResultById(@PathVariable String id) {
        return publicFileService.getPublicFileResultById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * @description 根据源文件 ID 获取转换后的文件结果列表 (保留原有接口)
     *
     * @param fileId 源文件 ID
     * @return 对应的 PublicFileResult 列表
     */
    @GetMapping("/byFileId/{fileId}")
    public ResponseEntity<List<PublicFileResult>> getPublicFileResultsByFileId(@PathVariable String fileId) {
        List<PublicFileResult> results = publicFileService.getPublicFileResultsByFileId(fileId);
        if (results.isEmpty()) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(results);
        }
    }

    /**
     * @description 第三方 AI 平台调用此接口，确认已成功下载并处理完指定原始文件ID列表的所有转换文件。
     * 调用成功后，会将所有相关的 PublicFileResult 记录的 `isWrittenByAiPlatform` 字段更新为 true。
     *
     * @param originalFileIds 原始文件的 ID 列表 (即 KnowledgeFileInfo 的 ID 列表)
     * @return 操作结果
     */
    @PostMapping("/ai-platform/confirm-processed") // 修改为非路径变量，接收请求体
    public ResponseEntity<String> confirmFileProcessed(@RequestBody List<String> originalFileIds) {
        logger.info("接收到文件列表 {} 的处理确认请求。", originalFileIds);
        if (originalFileIds == null || originalFileIds.isEmpty()) {
            return new ResponseEntity<>("请求体中的文件ID列表不能为空。", HttpStatus.BAD_REQUEST);
        }

        List<PublicFileResult> updatedFiles = publicFileService.updateAiPlatformWrittenStatus(originalFileIds, true);

        if (updatedFiles.isEmpty()) {
            logger.warn("未找到原始文件ID列表 {} 中的任何转换文件进行状态更新。", originalFileIds);
            return new ResponseEntity<>("未找到该原始文件ID列表中的任何转换文件进行状态更新。", HttpStatus.NOT_FOUND);
        }

        logger.info("原始文件ID列表 {} 中，共 {} 个转换文件已确认处理。", originalFileIds, updatedFiles.size());
        return ResponseEntity.ok("文件处理状态更新成功。");
    }

    @PostMapping("/test")
    public ResponseEntity<Void> test() {
        publicFileService.pushUnprocessedDataToAiPlatform();
        return ResponseEntity.ok().build();
    }
}
