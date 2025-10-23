package org.example.ai_api.Controller;

import org.example.ai_api.Bean.Entity.TargetFolderItem;
import org.example.ai_api.Bean.Model.ResultData;
import org.example.ai_api.Bean.WebRequest.UpdateFileTagsRequest;
import org.example.ai_api.Service.TargetFolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/TargetFolder")
public class TargetFolderController {

    @Autowired
    private TargetFolderService targetFolderService;

    @PostMapping("/getTargetFolder")
    public ResultData<List<TargetFolderItem>> getTargetFolder(@RequestBody List<String> targetIds) {
        List<TargetFolderItem> tags = targetFolderService.getTagsByIds(targetIds);
        return ResultData.success("获取成功", tags);
    }

    @PostMapping("/getTargetFolderByUserId")
    public ResultData<List<TargetFolderItem>> getTargetFolderByUserId(@RequestParam("userId") String userId) {
        List<TargetFolderItem> tags = targetFolderService.getTagsByUserId(userId);
        return ResultData.success("获取成功", tags);
    }

    @PostMapping("/deleteTargetFolder")
    public ResultData<Void> deleteTargetFolder(@RequestBody List<String> targetIds) {
        targetFolderService.deleteTagsByIds(targetIds);
        return ResultData.success("删除成功");
    }

    // 覆盖文件标签列表；当 fileId 为空时，仅创建/解析标签不绑定
    @PostMapping("/saveTargetFolder")
    public ResultData<List<TargetFolderItem>> updateFileTags(@RequestBody UpdateFileTagsRequest request) {
        List<TargetFolderItem> updated = targetFolderService.updateFileTags(request);
        return ResultData.success("更新成功", updated);
    }

}
