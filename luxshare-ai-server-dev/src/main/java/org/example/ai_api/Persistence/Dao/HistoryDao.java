package org.example.ai_api.Persistence.Dao;

import org.example.ai_api.Bean.Entity.Message;
import org.example.ai_api.Persistence.ConditionBuilder.HistoryConditionBuilder;
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
public class HistoryDao extends BaseMongoDao<Message>{
    @Override
    protected Class<Message> getEntityClass() {
        return Message.class;
    }

    public List<Message> findHistoryByUserId(String userId) {
        return find(
                new HistoryConditionBuilder()
                        .byUserId(userId)
        );
    }

    public List<Message> findHistoryByUserIdAndDateAfter(String userId, String date) {
        return find(
                new HistoryConditionBuilder()
                        .byUserId(userId)
                        .byDateAfter(date)
        );
    }

    public List<Message> findHistoryByUserIdAndDateBefore(String userId, String date) {
        return find(
                new HistoryConditionBuilder()
                        .byUserId(userId)
                        .byDateBefore(date)
        );
    }

    public List<Message> findHistoryByUserIdAndDateBetween(String userId, String startDate, String endDate) {
        return find(
                new HistoryConditionBuilder()
                        .byUserId(userId)
                        .byDateRange(startDate, endDate)
        );
    }

    // 支持字段投影的查询方法
    public List<Map> findMessagesByUserIdWithFields(String userId, String type,String keyword) {
        // 1. 构建条件
        HistoryConditionBuilder builder = new HistoryConditionBuilder()
                .byUserId(userId)
                .byType(type)
                .byTitleKeyword(keyword);
        Criteria criteria = builder.buildCriteria();
        MatchOperation match = Aggregation.match(criteria);

        // 2. 字段及别名映射
        Map<String, String> fieldAliasMap = new LinkedHashMap<>();
        fieldAliasMap.put("_id", "_id");
        fieldAliasMap.put("title", "title");
        fieldAliasMap.put("type", "type");
        fieldAliasMap.put("date", "date");

        // 3. 构建投影
        ProjectionOperation project = builder.buildProjectionWithDefaultValue(fieldAliasMap,"");

        Aggregation agg = Aggregation.newAggregation(
                match,
                project,
                Aggregation.sort(Sort.Direction.DESC, "date")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(agg, "QAHistory", Map.class);
        return results.getMappedResults();
    }

}
