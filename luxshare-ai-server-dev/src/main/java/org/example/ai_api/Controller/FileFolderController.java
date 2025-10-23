package org.example.ai_api.Controller;

import org.example.ai_api.Bean.Entity.*;
import org.example.ai_api.Bean.Enum.KnowledgeFileAction;
import org.example.ai_api.Bean.Model.ResultData;
import org.example.ai_api.Bean.Model.FolderOverviewResponse;
import org.example.ai_api.Bean.WebRequest.DepartmentFiles;
import org.example.ai_api.Bean.WebRequest.FolderSave;
import org.example.ai_api.Bean.WebRequest.KnowledgeBase;
import org.example.ai_api.Bean.WebRequest.FolderOverviewRequest;
import org.example.ai_api.Exception.BadRequestException;
import org.example.ai_api.Exception.NotAccessedException;
import org.example.ai_api.Service.DepartmentFileService;
import org.example.ai_api.Service.FileFolderService;
import org.example.ai_api.Service.FileService;
import org.example.ai_api.Service.SubFolderService;
import org.example.ai_api.Service.PermissionService;
import org.example.ai_api.Utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/FileFolder")
public class FileFolderController {

    private static final Logger logger = LoggerFactory.getLogger(FileFolderController.class);

    @Autowired
    private FileFolderService fileFolderService;
    @Autowired
    private FileService fileService;
    @Autowired
    private DepartmentFileService departmentFileService;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private SubFolderService subFolderService;

    /**
     * 新建文件夹（个人文件夹，部门文件夹）
     * @param request 文件夹创建请求
     * @return 新建的文件夹信息
     */
    @PostMapping("/createFolder")
    public ResultData<FolderList> createFolder(@RequestBody FolderSave request) {
        if (request == null) {
            throw new BadRequestException("请求体不可为空");
        }
        String folderName = request.getFolderName();
        String userId = request.getUserId();
        String departmentId = request.getDepartmentId();
        if (folderName == null || folderName.isEmpty()) {
            throw new BadRequestException("文件夹名称不可为空");
        }
        if (userId == null || userId.isEmpty()) {
            throw new BadRequestException("用户ID不可为空");
        }
        if (departmentId == null || departmentId.isEmpty()) {
            logger.warn("部门ID为空，将创建个人文件夹，userId:{}", userId);
        }
        FolderList folderList = fileFolderService.saveFolder(request);
        return ResultData.success("创建成功", folderList);
    }

    /**
     * 删除文件夹（个人文件夹，部门文件夹）
     * @param folderId 文件夹ID
     * @param userId 用户ID
     * @return 无内容响应
     */
    @PostMapping("/deleteFolder")
    public ResultData<Void> deleteFolder(@RequestParam("folderId") String folderId, @RequestParam("userId") String userId) {
        if (folderId == null || folderId.isEmpty()) {
            throw new BadRequestException("文件夹ID不可为空");
        }
        if (userId == null || userId.isEmpty()) {
            throw new BadRequestException("用户ID不可为空");
        }
        fileFolderService.deleteFolder(folderId, userId);
        return ResultData.success("删除成功");
    }

    /**
     * 获取文件夹列表
     * @param  id ID
     * @param isDepartment 是否为部门文件夹
     * @return 文件夹列表
     */
    @GetMapping("/getFolderList")
    public ResultData<List<FolderList>> getFolderList(
            @RequestParam("id") String id,
            @RequestParam("isDepartment") boolean isDepartment,
            @RequestParam(value = "keyword",defaultValue = "") String keyword
    ) throws Exception {
        if (id == null || id.isEmpty()) {
            throw new BadRequestException("ID不可为空");
        }
        List<FolderList> folders;
        if (isDepartment) {
            folders = fileFolderService.getDepartmentFolders(id,keyword);
        }else {
            folders = fileFolderService.getPrivateFolders(id,keyword);
        }
        return ResultData.success("获取成功", folders);
    }

    /**
     * 获取部门文件夹内文件列表(支持排序，搜索，分页)
     * @param request  文件请求
     * @return  文件列表
     */
    @PostMapping("/getFilesInDepartmentFolder")
    public ResultData<Page<DepartmentFile>> getFilesInDepartmentFolder(@RequestBody DepartmentFiles request) {
        if (request == null) {
            throw new BadRequestException("请求体不可为空");
        }
        String folderId = request.getFolderId();
        String userId = request.getUserId();
        String departmentId = request.getDepartmentId();
        String keyword = request.getKeywords();
        List<String> tagList = request.getTagList();
        if (folderId == null || folderId.isEmpty()) {
            throw new BadRequestException("文件夹ID不可为空");
        }
        if (userId == null || userId.isEmpty()) {
            throw new BadRequestException("用户ID不可为空");
        }
        if (departmentId == null || departmentId.isEmpty()) {
            throw new BadRequestException("部门ID不可为空");
        }
        // 权限：查看部门文件夹文件列表
        if (!permissionService.can(userId, folderId, KnowledgeFileAction.READ)) {
            throw new NotAccessedException("不能查看当前文件夹的文件");
        }
        List<DepartmentFile> files = fileFolderService.getFilesInDepartmentFolder(folderId,userId,departmentId,tagList,keyword);
        if (request.getSortType() != null) {
            files = departmentFileService.sortDepartmentFiles(files, request.getSortType(), request.isIncrease());
        }
        return  ResultData.success("获取成功", Utils.getFilesPage(request.getPage() - 1, request.getPageSize(), files));
    }

    /**
     * 获取文件夹内文件列表(支持排序，搜索，分页)
     * @param request 文件请求
     * @return 文件列表
     */
    @PostMapping("/getFilesInFolder")
    public ResultData<Page<KnowledgeFileInfo>> getFilesInFolder(@RequestBody KnowledgeBase<Void> request) {
        if (request == null) {
            throw new BadRequestException("请求体不可为空");
        }
        String folderId = request.getFolderId();
        String userId = request.getUserId();
        List<String> tagList = request.getTagList();
        String keyword = request.getKeywords();
        if (folderId == null || folderId.isEmpty()) {
            throw new BadRequestException("文件夹ID不可为空");
        }
        if (userId == null || userId.isEmpty()) {
            throw new BadRequestException("用户ID不可为空");
        }
        List<KnowledgeFileInfo> files = fileFolderService.getFilesInFolder(folderId, userId, keyword, tagList);
        if (request.getSortType() != null) {
            files = fileService.sortFileList(files, request.getSortType(), request.isIncrease());
        }
        return ResultData.success("获取成功", Utils.getFilesPage(request.getPage() - 1, request.getPageSize(), files));
    }

    /**
     * 获取一级文件夹下的“所有文件 + 二级文件夹”汇总（支持关键字与排序）
     * - 不改变现有接口，新增聚合查询
     * - 关键字：匹配文件名与子目录名（子目录端在服务端过滤）
     * - 排序：子目录（name|createTime），文件（fileName|createTime|fileSize），均支持升降序
     */
    @PostMapping("/getFolderOverview")
    public ResultData<FolderOverviewResponse> getFolderOverview(@RequestBody FolderOverviewRequest request) {
        if (request == null) {
            throw new BadRequestException("请求体不可为空");
        }
        String folderId = request.getFolderId();
        String userId = request.getUserId();
        String keyword = request.getKeywords();
        if (folderId == null || folderId.isEmpty()) {
            throw new BadRequestException("文件夹ID不可为空");
        }
        if (userId == null || userId.isEmpty()) {
            throw new BadRequestException("用户ID不可为空");
        }

        FolderOverviewResponse resp = new FolderOverviewResponse();

        // subfolders: 权限在子目录服务内部校验（READ）
        List<SubFolderItem> subFolders = subFolderService.list(folderId, userId);
        if (keyword != null && !keyword.isEmpty()) {
            subFolders = subFolders.stream()
                    .filter(sf -> sf.getName() != null && sf.getName().contains(keyword))
                    .collect(Collectors.toList());
        }
        // sort subfolders
        final boolean fAsc = request.isFolderIncrease();
        if ("createTime".equalsIgnoreCase(request.getFolderSortType())) {
            subFolders = subFolders.stream()
                    .sorted((a,b) -> fAsc ?
                            compareNullable(a.getCreateTime(), b.getCreateTime()) :
                            compareNullable(b.getCreateTime(), a.getCreateTime()))
                    .collect(Collectors.toList());
        } else if ("name".equalsIgnoreCase(request.getFolderSortType())) {
            subFolders = subFolders.stream()
                    .sorted((a,b) -> fAsc ?
                            compareNullable(a.getName(), b.getName()) :
                            compareNullable(b.getName(), a.getName()))
                    .collect(Collectors.toList());
        }
        resp.setSubFolders(subFolders);

        // files: 直接复用已有查询与排序
        if (request.isDepartment()) {
            List<DepartmentFile> files = fileFolderService.getFilesInDepartmentFolder(folderId, userId, null, null, keyword);
            if (request.getFileSortType() != null) {
                files = departmentFileService.sortDepartmentFiles(files, request.getFileSortType(), request.isFileIncrease());
            }
            resp.setDepartmentFiles(files);
        } else {
            List<KnowledgeFileInfo> files = fileFolderService.getFilesInFolder(folderId, userId, keyword, null);
            if (request.getFileSortType() != null) {
                files = fileService.sortFileList(files, request.getFileSortType(), request.isFileIncrease());
            }
            resp.setPersonalFiles(files);
        }

        return ResultData.success("获取成功", resp);
    }

    private static int compareNullable(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return a.compareTo(b);
    }
}
