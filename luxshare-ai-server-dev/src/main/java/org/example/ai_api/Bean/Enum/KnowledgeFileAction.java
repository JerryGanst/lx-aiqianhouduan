package org.example.ai_api.Bean.Enum;

/**
 * 权限动作枚举
 *
 * 兼容已有的泛化动作（READ/UPLOAD/DELETE），并补充更细粒度的文件/文件夹级动作。
 * 说明：
 * - FILE_* 与 FOLDER_* 为更细粒度动作；
 * - 现有逻辑若只判断 UPLOAD/DELETE/READ，不会受新增枚举影响；
 *   如需精细控制，可在权限表中使用 FILE_* / FOLDER_*。
 */
public enum KnowledgeFileAction {
    // 通用
    READ,           // 查看/下载
    UPLOAD,         //（泛化）上传
    DELETE,         //（泛化）删除

    // 文件级动作
    FILE_UPLOAD,    // 文件上传
    FILE_DELETE,    // 文件删除

    // 文件夹级动作
    FOLDER_CREATE,  // 新建文件夹
    FOLDER_UPDATE,  // 修改/重命名文件夹
    FOLDER_DELETE   // 删除文件夹
}
