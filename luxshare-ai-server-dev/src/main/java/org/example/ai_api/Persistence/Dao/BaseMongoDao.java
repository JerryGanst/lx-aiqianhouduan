package org.example.ai_api.Persistence.Dao;

import org.example.ai_api.Persistence.ConditionBuilder.BaseConditionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public abstract class BaseMongoDao<T> {

    @Autowired
    protected MongoTemplate mongoTemplate;

    protected abstract Class<T> getEntityClass();

    // ===== 查询操作 =====
    public List<T> find(BaseConditionBuilder<?> builder) {
        return mongoTemplate.find(builder.toQuery(), getEntityClass());
    }

    public T findOne(BaseConditionBuilder<?> builder) {
        return mongoTemplate.findOne(builder.toQuery(), getEntityClass());
    }

    public List<T> findPaged(BaseConditionBuilder<?> builder, Pageable pageable) {
        Query query = builder.toQuery().with(pageable);
        return mongoTemplate.find(query, getEntityClass());
    }

    public long count(BaseConditionBuilder<?> builder) {
        return mongoTemplate.count(builder.toQuery(), getEntityClass());
    }

    // ===== 更新操作 =====
    public long update(BaseConditionBuilder<?> condition, Update update) {
        return mongoTemplate.updateMulti(
                condition.toQuery(),
                update,
                getEntityClass()
        ).getModifiedCount();
    }

    public long update(BaseConditionBuilder<?> condition, BaseConditionBuilder<?> updateBuilder) {
        return update(condition, updateBuilder.toUpdate());
    }

    // ===== 删除操作 =====
    public void delete(BaseConditionBuilder<?> condition) {
        mongoTemplate.remove(
                condition.toDeleteQuery(),
                getEntityClass()
        ).getDeletedCount();
    }

    // ===== 便捷方法 =====
    public T save(T entity) {
        return mongoTemplate.save(entity);
    }

    public void insert(T entity) {
        mongoTemplate.insert(entity);
    }

    public void deleteById(String id) {
        mongoTemplate.remove(Query.query(Criteria.where("id").is(id)), getEntityClass());
    }
}
