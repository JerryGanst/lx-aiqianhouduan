package org.example.ai_api.Controller;

import org.example.ai_api.Bean.Entity.SubFolderItem;
import org.example.ai_api.Bean.Model.ResultData;
import org.example.ai_api.Bean.WebRequest.SubFolderCreateRequest;
import org.example.ai_api.Bean.WebRequest.SubFolderUpdateRequest;
import org.example.ai_api.Bean.WebRequest.SubFolderMoveRequest;
import org.example.ai_api.Service.SubFolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/SubFolder")
public class SubFolderController {

    @Autowired
    private SubFolderService subFolderService;

    // 1. 新建二级文件夹
    @PostMapping("/create")
    public ResultData<SubFolderItem> create(@RequestBody SubFolderCreateRequest request) {
        SubFolderItem item = subFolderService.create(request.getFolderId(), request.getTargetName(), request.getUserId());
        return ResultData.success("创建成功", item);
    }

    // 2. 更新二级文件夹
    @PostMapping("/update")
    public ResultData<SubFolderItem> update(@RequestBody SubFolderUpdateRequest request) {
        SubFolderItem item = subFolderService.update(request.getId(), request.getTargetName(), request.getUserId());
        return ResultData.success("更新成功", item);
    }

    // 3. 列表
    @GetMapping("/list")
    public ResultData<List<SubFolderItem>> list(@RequestParam("folderId") String folderId,
                                                @RequestParam("userId") String userId) {
        List<SubFolderItem> list = subFolderService.list(folderId, userId);
        return ResultData.success("获取成功", list);
    }

    // 5. 删除二级文件夹
    @PostMapping("/delete")
    public ResultData<Void> delete(@RequestParam("id") String id,
                                   @RequestParam("userId") String userId) {
        subFolderService.delete(id, userId);
        return ResultData.success("删除成功");
    }

    // 6+. 文件在二级文件夹间移动（同一父目录作用域内）
    @PostMapping("/moveFile")
    public ResultData<Void> moveFile(@RequestBody SubFolderMoveRequest request) {
        subFolderService.moveFileBetweenSubFolders(
                request.getUserId(),
                request.getFileId(),
                request.isDepartment(),
                request.getToSubFolderId(),
                request.getFromSubFolderId()
        );
        return ResultData.success("移动成功");
    }
}
