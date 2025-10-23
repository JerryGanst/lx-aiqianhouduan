package org.example.ai_api.Service.Apis.Commons;

import org.example.ai_api.Bean.ApiRequests.UnifiedChatRequest;
import org.example.ai_api.Bean.Entity.DepartmentFile;
import org.example.ai_api.Bean.Entity.KnowledgeFileInfo;
import org.example.ai_api.Bean.Enum.ChatType;
import org.example.ai_api.Exception.BadRequestException;
import org.example.ai_api.Persistence.Dao.DepartmentFileDao;
import org.example.ai_api.Persistence.Dao.KnowledgeFileDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 知识库文件选择
 * @author 10353965
 */
@Component
public class KnowledgeEnricher {

    @Autowired
    private KnowledgeFileDao knowledgeFileDao;
    @Autowired
    private DepartmentFileDao departmentFileDao;

    /**
     * 将个人知识库内容加入请求体
     *
     * @param unifiedChatRequest 统一问答请求
     * @param userId 用户id
     */
    public void addPersonalKnowledgeToRequest(UnifiedChatRequest unifiedChatRequest, String userId, ChatType chatType, String folderId, String fileId,List<String> tagList) {
        unifiedChatRequest.setUsePersonalKnowledge(true);
        List<String> filePaths;
        if (chatType == null){
            throw new BadRequestException("请确定知识库问答类型");
        }
        switch (chatType){
            case ALL_FILES: filePaths = addAllKnowledgeFilesToObjects(userId); break;
            case PARTIAL_FILES: filePaths = addPartKnowledgeFilesToObjects(folderId); break;
            case SINGLE_FILE: filePaths = addSingleKnowledgeFilesToObjects(fileId);break;
            case Tag_Files: filePaths = addTagFilesToObjects(tagList);break;
            default:throw new BadRequestException("知识库问答请求参数错误");
        }
        if (filePaths == null || filePaths.isEmpty()){
            throw new BadRequestException("未找到有效的知识库文件，请上传或选择知识库文件后再提问");
        }
        unifiedChatRequest.setObjects(filePaths);
    }

    private List<String> addTagFilesToObjects(List<String> tagList) {
        return knowledgeFileDao.findByTagList(tagList).stream()
                .map(KnowledgeFileInfo::getConvertPath)
                .filter(path -> path != null && !path.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 将所有知识库文件加入请求体
     * @param userId 用户id
     * @return 知识库文件列表
     */
    private List<String> addAllKnowledgeFilesToObjects(String userId){
        return knowledgeFileDao.findPrivateFilesByUploaderId(userId).stream()
                .map(KnowledgeFileInfo::getConvertPath)
                .filter(path -> path != null && !path.isEmpty())
                //.filter(path -> !path.endsWith(".xlsx")&&!path.endsWith(".xls"))
                .collect(Collectors.toList());

    }

    /**
     * 将指定文件夹中的知识库文件加入请求体
     * @param folderId 文件夹id
     * @return 知识库文件列表
     */
    private List<String> addPartKnowledgeFilesToObjects(String folderId){
        if(folderId == null || folderId.isEmpty()){
            throw new BadRequestException("请选择有效的文件夹提问");
        }
        return knowledgeFileDao.findByFolderId(folderId).stream()
                .map(KnowledgeFileInfo::getConvertPath)
                .filter(path -> path != null && !path.isEmpty())
                //.filter(path -> !path.endsWith(".xlsx")&&!path.endsWith(".xls"))
                .collect(Collectors.toList());
    }

    /**
     * 将指定文件加入请求体
     * @param fileId 文件id
     * @return 知识库文件列表
     */
    private List<String> addSingleKnowledgeFilesToObjects(String fileId){
        String filePath = knowledgeFileDao.findByFileId(fileId).getConvertPath();
        if(filePath != null && !filePath.isEmpty()){
            List<String> list = new ArrayList<>();
            list.add(filePath);
            return list;
        }else{
            throw new BadRequestException("请选择确定的知识库文件提问");
        }
    }

    public void addDepartmentKnowledgeToRequest(UnifiedChatRequest unifiedChatRequest,String folderId,String fileId,List<String> tagList,ChatType chatType){
        unifiedChatRequest.setUsePersonalKnowledge(true);
        List<String> filePaths;
        if (chatType == null){
            throw new BadRequestException("请确定知识库问答类型");
        }
        switch (chatType){
            case Department_Partial: filePaths = addDepartmentPartKnowledgeFilesToObjects(folderId); break;
            case Department_Single: filePaths = addDepartmentSingleKnowledgeFilesToObjects(fileId);break;
            case Tag_Files: filePaths = addDepartmentTagFilesToObjects(tagList);break;
            default:throw new BadRequestException("知识库问答请求参数错误");
        }
        if (filePaths == null || filePaths.isEmpty()){
            throw new BadRequestException("未找到有效的知识库文件，请上传或选择知识库文件后再提问");
        }
        unifiedChatRequest.setObjects(filePaths);
    }

    private List<String> addDepartmentTagFilesToObjects(List<String> tagList) {
        return departmentFileDao.findByTargetItemIds(tagList).stream()
                .map(DepartmentFile::getConvertPath)
                .filter(path -> path != null && !path.isEmpty())
                .collect(Collectors.toList());
    }

    private List<String> addDepartmentPartKnowledgeFilesToObjects(String folderId){
        return departmentFileDao.findByFolderId(folderId).stream()
                .map(DepartmentFile::getConvertPath)
                .filter(path -> path != null && !path.isEmpty())
                //.filter(path -> !path.endsWith(".xlsx")&&!path.endsWith(".xls"))
                .collect(Collectors.toList());
    }

    private List<String> addDepartmentSingleKnowledgeFilesToObjects(String fileId){
        String filePath = departmentFileDao.findById(fileId).getConvertPath();
        if(filePath != null && !filePath.isEmpty()){
            List<String> list = new ArrayList<>();
            list.add(filePath);
            return list;
        }
        throw new BadRequestException("请选择确定的知识库文件提问");
    }

    /**
     * 根据调用方提供的 action / 用户信息 / metadata，构造注入到 RAG 检索工具（如 rag_search）的参数。
     * 仅作为工具方法提供，不影响现有逻辑。
     * <p>
     * 规则（参考调用约定）：
     * - tenant_id：默认 "luxshare-tech"，允许在 metadata 中覆盖
     * - kb_type：由 action 推断（it/hr=enterprise，personal=personal，department=department）；action=auto 时不强制设置
     * - owner_user_id（唯一覆盖键）：
     *   - 当 kb_type=personal 时，默认等于 userId
     *   - 当 kb_type=department 时，默认等于 userDepartment
     *   - 仅接受 metadata 中的 owner_user_id 作为覆盖；忽略 owner_id/department_id/user_department 等别名
     * - folder_id：默认 null（表示检索全部）
     * - doc_type / document_ids：仅在 metadata 提供时传入；不注入默认值
     * - 其余自定义过滤（如 tag_filter）原样透传
     *
     * @param action          调用 action（auto 或 rag_* 子图名）
     * @param userId         当前用户 id（用于个人库默认归属）
     * @param userDepartment 当前用户所属部门（用于部门库默认归属）
     * @param metadataList   元数据数组（后者覆盖前者）；可为 null/空
     * @return 构造后的用于 RAG 工具调用的参数 Map（不可为 null）
     */
    public static Map<String, Object> buildRagToolParams(String action,
                                                         String userId,
                                                         String userDepartment,
                                                         List<Map<String, Object>> metadataList) {
        // 1) 合并 metadata（后面的覆盖前面的），保持插入顺序
        Map<String, Object> merged = new LinkedHashMap<>();
        if (metadataList != null) {
            for (Map<String, Object> m : metadataList) {
                if (m != null) merged.putAll(m);
            }
        }

        // 2) 设置 tenant_id 默认值（允许被 metadata 覆盖）
        merged.putIfAbsent("tenant_id", "luxshare-tech");

        // 3) 根据 action 推断 kb_type（auto 不强制）
        String kbType = inferKbTypeFromAction(action);
        if (kbType != null && !merged.containsKey("kb_type")) {
            merged.put("kb_type", kbType);
        }

        // 4) 规范 owner_user_id（唯一覆盖键），忽略别名键
        //    仅当未显式提供 owner_user_id 时，按 kb_type 注入默认值
        if (!merged.containsKey("owner_user_id")) {
            Object finalKbType = merged.get("kb_type");
            if (Objects.equals(finalKbType, "personal")) {
                if (userId != null && !userId.isEmpty()) {
                    merged.put("owner_user_id", userId);
                }
            } else if (Objects.equals(finalKbType, "department")) {
                if (userDepartment != null && !userDepartment.isEmpty()) {
                    merged.put("owner_user_id", userDepartment);
                }
            }
        }

        // 5) folder_id：默认 null（若未提供）。doc_type/document_ids 不做默认注入，仅保留来参。
        if (!merged.containsKey("folder_id")) {
            merged.put("folder_id", null);
        }

        // 6) 忽略别名覆盖键（不删除，避免破坏上层逻辑；但不会向下游作为标准键使用）
        //    标准键仅为：tenant_id, kb_type, owner_user_id, folder_id, doc_type, document_ids 及其他透传过滤条件

        return merged;
    }

    /**
     * 重载：当调用方已将 metadata 合并为单个 Map 时可使用。
     */
    public static Map<String, Object> buildRagToolParams(String action,
                                                         String userId,
                                                         String userDepartment,
                                                         Map<String, Object> metadata) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (metadata != null) list.add(metadata);
        return buildRagToolParams(action, userId, userDepartment, list);
    }

    /**
     * 根据 action 推断 kb_type。
     * rag_it_agent / rag_hr_agent -> enterprise
     * rag_personal_agent -> personal
     * rag_department_agent -> department
     * 其他（含 auto）返回 null
     */
    private static String inferKbTypeFromAction(String action) {
        if (action == null) return null;
        switch (action) {
            case "rag_it_agent":
            case "rag_hr_agent":
                return "enterprise";
            case "rag_personal_agent":
                return "personal";
            case "rag_department_agent":
                return "department";
            default:
                return null; // auto 或未知 action，不做推断
        }
    }
}
