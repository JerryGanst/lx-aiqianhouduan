package org.example.ai_api.Persistence.Dao;

import org.example.ai_api.Bean.Entity.ImgRecognition;
import org.example.ai_api.Persistence.ConditionBuilder.ImageRecognitionConditionBuilder;
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
public class ImageRecognitionDao extends BaseMongoDao<ImgRecognition>{

    @Override
    protected Class<ImgRecognition> getEntityClass() {
        return ImgRecognition.class;
    }

    public List<ImgRecognition> getImgRecognitionByUserId(String userId,String keyword)  {
        return find(
                new ImageRecognitionConditionBuilder()
                        .byUserId(userId)
                        .byTitleWithKeyword(keyword)
        );
    }

    public ImgRecognition getImgRecognitionById(String id)  {
        return findOne(
                new ImageRecognitionConditionBuilder().byImageId(id)
        );
    }

    public void deleteImgRecognitionById(String id)  {
        delete(
                new ImageRecognitionConditionBuilder().byImageId(id)
        );
    }
    
    // 支持字段投影的查询方法，只查询id和title
    public List<Map> findImgRecognitionByUserIdWithFields(String userId, String keyword) {
        // 1. 构建条件
        ImageRecognitionConditionBuilder builder = new ImageRecognitionConditionBuilder()
                .byUserId(userId)
                .byTitleWithKeyword(keyword);
        Criteria criteria = builder.buildCriteria();
        MatchOperation match = Aggregation.match(criteria);

        // 2. 字段及别名映射 - 包含排序字段
        Map<String, String> fieldAliasMap = new LinkedHashMap<>();
        fieldAliasMap.put("_id", "_id");
        fieldAliasMap.put("title", "title");
        fieldAliasMap.put("lastOperationTime", "lastOperationTime");

        // 3. 构建投影
        ProjectionOperation project = builder.buildProjectionWithDefaultValue(fieldAliasMap,"");

        // 4. 先排序，再投影（如果上面的方案不行，可以尝试这个）
        Aggregation agg = Aggregation.newAggregation(
                match,
                Aggregation.sort(Sort.Direction.DESC, "lastOperationTime"),
                project
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(agg, "ImgRecognition", Map.class);
        return results.getMappedResults();
    }
}
