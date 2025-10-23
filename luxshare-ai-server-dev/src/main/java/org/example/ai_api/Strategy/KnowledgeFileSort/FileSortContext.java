package org.example.ai_api.Strategy.KnowledgeFileSort;

import org.example.ai_api.Bean.Enum.SortType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 知识库文件排序上下文
 * @author 10353965
 */
@Component
public class FileSortContext {
    private final Map<SortType, FileSortStrategy> strategyMap;

    @Autowired
    public FileSortContext(List<FileSortStrategy> strategyList) {
        strategyMap = strategyList.stream()
                .collect(
                        Collectors.toMap(
                                FileSortStrategy::getType,
                                Function.identity()
                        )
                );
    }

    /**
     * 根据枚举类型获取策略
     */
    public FileSortStrategy getStrategy(SortType type) {
        return strategyMap.get(type);
    }

    /**
     * 根据字符串类型获取策略（向后兼容）
     */
    public FileSortStrategy getStrategy(String type) {
        try {
            SortType sortType = SortType.valueOf(type.toUpperCase());
            return strategyMap.get(sortType);
        } catch (IllegalArgumentException e) {
            // 如果字符串不匹配枚举值，尝试通过code匹配
            for (SortType sortType : SortType.values()) {
                if (sortType.getCode().equals(type)) {
                    return strategyMap.get(sortType);
                }
            }
            return null;
        }
    }
}
