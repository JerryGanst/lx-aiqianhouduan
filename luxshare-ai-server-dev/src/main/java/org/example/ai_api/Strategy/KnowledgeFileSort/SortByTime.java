package org.example.ai_api.Strategy.KnowledgeFileSort;

import org.example.ai_api.Bean.Entity.DepartmentFile;
import org.example.ai_api.Bean.Entity.KnowledgeFileInfo;
import org.example.ai_api.Bean.Enum.SortType;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 10353965
 */
@Component
public class SortByTime implements FileSortStrategy {

    @Override
    public SortType getType() {
        return SortType.TIME;
    }

    @Override
    public List<KnowledgeFileInfo> sort(List<KnowledgeFileInfo> list) {
        return list.stream()
                .sorted(
                        Comparator.comparing(
                                KnowledgeFileInfo::getCreateTime,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        )
                )
                .collect(Collectors.toList());
    }

    @Override
    public List<DepartmentFile> sortDepartmentFile(List<DepartmentFile> list) {
        return list.stream()
                .sorted(
                        Comparator.comparing(
                                DepartmentFile::getCreateTime,
                                Comparator.nullsLast(Comparator.naturalOrder())
                                )
                        )
                .collect(Collectors.toList());
    }
}
