package org.example.ai_api.Persistence.Dao;

import org.example.ai_api.Bean.Entity.AgentChatInfo;
import org.example.ai_api.Persistence.ConditionBuilder.AgentChatConditionBuilder;
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
public class AgentChatDao extends BaseMongoDao<AgentChatInfo>{
    @Override
    protected Class<AgentChatInfo> getEntityClass() {
        return AgentChatInfo.class;
    }

    public List<AgentChatInfo> findAgentChatByAgentIdAndUserId(String agentId, String userId){
        return find(
                new AgentChatConditionBuilder()
                        .byAgentId(agentId)
                        .byUserId(userId)
        );
    }

    public void deleteByAgentId(String agentId){
        delete(
                new AgentChatConditionBuilder()
                        .byAgentId(agentId)
        );
    }

    public List<AgentChatInfo> findAgentChatByAgentId(String agentId){
        return find(
                new AgentChatConditionBuilder()
                        .byAgentId(agentId)
        );
    }

    public AgentChatInfo findAgentChatByChatId(String chatId){
        return findOne(
                new AgentChatConditionBuilder()
                        .byId(chatId)
        );
    }

    // 支持字段投影的查询方法
    public List<Map> findAgentChatByAgentIdWithFields(String agentId, String keyword) {
        // 1. 构建条件
        AgentChatConditionBuilder builder = new AgentChatConditionBuilder()
                .byAgentId(agentId)
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

        AggregationResults<Map> results = mongoTemplate.aggregate(agg, "AgentChat", Map.class);
        return results.getMappedResults();
    }
}
