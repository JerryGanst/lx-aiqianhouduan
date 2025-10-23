package org.example.ai_api.Persistence.Dao;

import org.example.ai_api.Bean.Entity.Agent;
import org.example.ai_api.Persistence.ConditionBuilder.AgentConditionBuilder;
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
public class AgentDao extends BaseMongoDao<Agent>{
    @Override
    public Class<Agent> getEntityClass() {
        return Agent.class;
    }

    public List<Agent> findAgentByUserId(String userId){
        return find(
                new AgentConditionBuilder()
                        .byUserId(userId)
        );
    }

    // 支持字段投影的查询方法
    public List<Map> findAgentByUserIdWithFields(String userId, String keyword) {
        // 1. 构建条件
        AgentConditionBuilder builder = new AgentConditionBuilder()
                .addOrConditions(
                        Criteria.where("userId").is(userId),
                        Criteria.where("isSystem").is(true)
                )
                .byPersonaNameKeyword(keyword);
        Criteria criteria = builder.buildCriteria();
        MatchOperation match = Aggregation.match(criteria);

        // 2. 字段及别名映射
        Map<String, String> fieldAliasMap = new LinkedHashMap<>();
        fieldAliasMap.put("_id", "_id");
        fieldAliasMap.put("agentPic", "agentPic");
        fieldAliasMap.put("agentPicUrl", "agentPicUrl");
        fieldAliasMap.put("persona.name", "agentName");
        fieldAliasMap.put("lastOperationTime", "lastOperationTime");
        fieldAliasMap.put("persona.introduction", "agentIntroduction");

        // 3. 构建投影
        ProjectionOperation project = builder.buildProjectionWithDefaultValue(fieldAliasMap,"");

        Aggregation agg = Aggregation.newAggregation(
                match,
                project,
                Aggregation.sort(Sort.Direction.DESC, "lastOperationTime")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(agg, "Agent", Map.class);
        return results.getMappedResults();
    }

    // 支持排除字段的查询方法
    public List<Agent> findAgentByUserIdExcludeFields(String userId, String... excludeFields) {
        return find(
                new AgentConditionBuilder()
                        .byUserId(userId)
                        .excludeFields(excludeFields)
        );
    }

    // 查询单个Agent信息并设置字段别名
    public Map findAgentByIdWithFields(String agentId) {
        // 1. 构建条件
        AgentConditionBuilder builder = new AgentConditionBuilder()
                .byId(agentId);
        Criteria criteria = builder.buildCriteria();
        MatchOperation match = Aggregation.match(criteria);

        // 2. 字段及别名映射
        Map<String, String> fieldAliasMap = new LinkedHashMap<>();
        fieldAliasMap.put("_id", "_id");
        fieldAliasMap.put("userId", "userId");
        fieldAliasMap.put("agentPic", "agentPic");
        fieldAliasMap.put("agentPicUrl", "agentPicUrl");
        fieldAliasMap.put("persona.name", "agentName");
        fieldAliasMap.put("persona.description", "agentDescription");
        fieldAliasMap.put("persona.introduction", "agentIntroduction");
        fieldAliasMap.put("persona.files","agentFiles");
        fieldAliasMap.put("createTime", "createTime");
        fieldAliasMap.put("updateTime", "updateTime");
        fieldAliasMap.put("lastOperationTime", "lastOperationTime");

        // 3. 构建投影，使用通用方法处理不存在字段时返回空字符串
        ProjectionOperation project = builder.buildProjectionWithDefaultValue(fieldAliasMap,null);

        Aggregation agg = Aggregation.newAggregation(
                match,
                project
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(agg, "Agent", Map.class);
        List<Map> resultList = results.getMappedResults();
        return resultList.isEmpty() ? null : resultList.get(0);
    }
}
