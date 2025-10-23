package org.example.ai_api.Strategy.KnowledgeFileSort;

import org.example.ai_api.Bean.Entity.DepartmentFile;
import org.example.ai_api.Bean.Entity.KnowledgeFileInfo;
import org.example.ai_api.Bean.Enum.SortType;

import java.util.List;

/**
 * 知识库文件排序策略
 */
public interface FileSortStrategy {
    /**
     * 获取策略对应的类型标识
     * @return  SortType
     */
    SortType getType();

    /**
     * 文件排序
     * @param list  List<KnowledgeFileInfo>
     * @return   List<KnowledgeFileInfo>
     */
    List<KnowledgeFileInfo> sort(List<KnowledgeFileInfo> list);

    /**
     *  部门文件排序
     * @param list  List<DepartmentFile>
     * @return    List<DepartmentFile>
     */
    List<DepartmentFile> sortDepartmentFile(List<DepartmentFile> list);
}
