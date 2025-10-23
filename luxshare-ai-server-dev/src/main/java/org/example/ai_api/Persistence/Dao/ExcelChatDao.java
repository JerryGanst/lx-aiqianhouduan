package org.example.ai_api.Persistence.Dao;

import org.example.ai_api.Bean.WebRequest.ExcelChat;
import org.example.ai_api.Persistence.ConditionBuilder.ExcelChatConditionBuilder;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ExcelChatDao extends BaseMongoDao<ExcelChat>{
    @Override
    protected Class<ExcelChat> getEntityClass() {
        return ExcelChat.class;
    }

    // 支持字段投影的查询方法
    public List<Map> findExcelChatByAgentIdWithFields(String userId, String keyword) {
        // 1. 构建条件
        ExcelChatConditionBuilder builder = new ExcelChatConditionBuilder()
                .byUserId(userId)
                .byTitleKeyword(keyword);
        Criteria criteria = builder.buildCriteria();
        MatchOperation match = Aggregation.match(criteria);

        // 2. 字段及别名映射
        Map<String, String> fieldAliasMap = new LinkedHashMap<>();
        fieldAliasMap.put("_id", "_id");
        fieldAliasMap.put("title", "title");
        fieldAliasMap.put("lastOperationTime", "lastOperationTime");

        // 3. 构建投影
        ProjectionOperation project = builder.buildProjectionWithDefaultValue(fieldAliasMap,"");

        Aggregation agg = Aggregation.newAggregation(
                match,
                project,
                Aggregation.sort(Sort.Direction.DESC, "lastOperationTime")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(agg, "ExcelChatInfo", Map.class);
        return results.getMappedResults();
    }
}
