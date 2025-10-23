package org.example.ai_api.Persistence.ConditionBuilder;

import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.domain.Sort;

import java.util.*;

public abstract class BaseConditionBuilder<T extends BaseConditionBuilder<T>> {

    protected final Criteria criteria = new Criteria();
    protected final Map<String, Object> updateOperations = new HashMap<>();
    protected boolean hasCriteria = false;
    //支持字段投影
    protected Set<String> includeFields = new HashSet<>();
    protected Set<String> excludeFields = new HashSet<>();
    //支持排序
    protected List<Sort.Order> sortOrders = new ArrayList<>();

    // 添加条件 (字段路径查询)
    @SuppressWarnings("unchecked")
    public T addCondition(String fieldPath, Object value) {
        if (value != null) {
            criteria.and(fieldPath).is(value);
            hasCriteria = true;
        }
        return (T) this;
    }

    // IN 查询条件
    @SuppressWarnings("unchecked")
    public T addInCondition(String fieldPath, Collection<?> values) {
        if (values == null) {
            return (T) this; // 不添加任何条件
        }

        if (values.isEmpty()) {
            // 空集合时强制返回空结果：添加一个永不成立的条件
            this.criteria.and("_id").exists(false);
            this.hasCriteria = true;
            return (T) this;
        }

        // 正常 in 查询
        this.criteria.and(fieldPath).in(values);
        this.hasCriteria = true;
        return (T) this;
    }

    // 范围查询
    @SuppressWarnings("unchecked")
    public T addRange(String fieldPath, Comparable<?> start, Comparable<?> end) {
        if (start != null || end != null) {
            Criteria range = Criteria.where(fieldPath);
            if (start != null) range.gte(start);
            if (end != null) range.lte(end);
            criteria.andOperator(range);
            hasCriteria = true;
        }
        return (T) this;
    }

    // 包含字段（支持多层嵌套）
    @SuppressWarnings("unchecked")
    public T includeFields(String... fields) {
        if (fields != null) {
            includeFields.addAll(Arrays.asList(fields));
        }
        return (T) this;
    }

    // 新增：排除字段（支持多层嵌套）
    @SuppressWarnings("unchecked")
    public T excludeFields(String... fields) {
        if (fields != null) {
            excludeFields.addAll(Arrays.asList(fields));
        }
        return (T) this;
    }

    /**
     * 对指定字段添加关键字模糊查询（不区分大小写）
     * @param fieldPath 字段路径（如 "persona.name"）
     * @param keyword 关键字
     * @return this
     */
    @SuppressWarnings("unchecked")
    public T addKeywordCondition(String fieldPath, String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            criteria.and(fieldPath).regex(".*" + keyword + ".*", "i");
            hasCriteria = true;
        }
        return (T) this;
    }

    /**
     * 添加排序规则
     * @param field 排序字段
     * @param direction 排序方向（升序或降序）
     * @return this
     */
    @SuppressWarnings("unchecked")
    public T addSort(String field, Sort.Direction direction) {
        if (field != null && direction != null) {
            sortOrders.add(new Sort.Order(direction, field));
        }
        return (T) this;
    }

    /**
     * 添加升序排序
     * @param field 排序字段
     * @return this
     */
    public T addAscSort(String field) {
        return addSort(field, Sort.Direction.ASC);
    }

    /**
     * 添加降序排序
     * @param field 排序字段
     * @return this
     */
    public T addDescSort(String field) {
        return addSort(field, Sort.Direction.DESC);
    }

    // 构造查询对象
    public Query toQuery() {
        Query query = hasCriteria ? Query.query(criteria) : new Query();
        
        // 设置字段投影
        if (!includeFields.isEmpty()) {
            for (String field : includeFields) {
                query.fields().include(field);
            }
        }
        
        if (!excludeFields.isEmpty()) {
            for (String field : excludeFields) {
                query.fields().exclude(field);
            }
        }
        
        // 应用排序规则
        if (!sortOrders.isEmpty()) {
            query.with(Sort.by(sortOrders));
        }
        
        return query;
    }

    // 构造删除查询
    public Query toDeleteQuery() {
        return toQuery();
    }

    // 构建最终条件对象
    public Criteria buildCriteria() {
        return criteria;
    }

    // === 构造更新操作的辅助方法 ===

    // 设置字段值
    @SuppressWarnings("unchecked")
    public T set(String field, Object value) {
        if (value != null) {
            updateOperations.put(field, value);
        }
        return (T) this;
    }

    // 增加字段值
    @SuppressWarnings("unchecked")
    public T inc(String field, Number amount) {
        if (amount != null) {
            updateOperations.put("$inc." + field, amount);
        }
        return (T) this;
    }

    // 添加到数组
    @SuppressWarnings("unchecked")
    public T push(String field, Object value) {
        if (value != null) {
            updateOperations.put("$push." + field, value);
        }
        return (T) this;
    }

    // 构建更新操作
    public Update toUpdate() {
        Update update = new Update();

        updateOperations.forEach((key, value) -> {
            if (key.startsWith("$inc.")) {
                update.inc(key.substring(5), (Number) value);
            } else if (key.startsWith("$push.")) {
                update.push(key.substring(6), value);
            } else {
                update.set(key, value);
            }
        });

        return update;
    }

    public ProjectionOperation buildProjection(Map<String, String> fieldAliasMap) {
        ProjectionOperation project = Aggregation.project();
        for (Map.Entry<String, String> entry : fieldAliasMap.entrySet()) {
            project = project.and(entry.getKey()).as(entry.getValue());
        }
        return project;
    }

    /**
     * 构建投影操作，当字段不存在时返回指定的默认值
     * @param fieldAliasMap 字段路径到别名的映射
     * @param defaultValues 字段路径到默认值的映射，如果字段不存在则返回对应的默认值
     * @return ProjectionOperation
     */
    public ProjectionOperation buildProjectionWithDefaults(Map<String, String> fieldAliasMap, Map<String, Object> defaultValues) {
        ProjectionOperation project = Aggregation.project();
        for (Map.Entry<String, String> entry : fieldAliasMap.entrySet()) {
            String fieldPath = entry.getKey();
            String alias = entry.getValue();
            Object defaultValue = defaultValues.get(fieldPath);
            
            if ("_id".equals(fieldPath)) {
                // _id 字段不需要 $ifNull 处理
                project = project.and(fieldPath).as(alias);
            } else if (defaultValue != null) {
                // 使用指定的默认值
                project = project.and(ConditionalOperators.ifNull(fieldPath).then(defaultValue)).as(alias);
            } else {
                // 没有指定默认值，使用原字段
                project = project.and(fieldPath).as(alias);
            }
        }
        return project;
    }

    /**
     * 构建投影操作，当字段不存在时返回设定的默认值
     * @param fieldAliasMap 字段路径到别名的映射
     * @param defaultValue  字段不存在时的默认值
     * @return ProjectionOperation
     */
    public ProjectionOperation buildProjectionWithDefaultValue(Map<String, String> fieldAliasMap,Object defaultValue) {
        ProjectionOperation project = Aggregation.project();
        for (Map.Entry<String, String> entry : fieldAliasMap.entrySet()) {
            String fieldPath = entry.getKey();
            String alias = entry.getValue();
            
            if ("_id".equals(fieldPath)) {
                // _id 字段不需要 $ifNull 处理
                project = project.and(fieldPath).as(alias);
            } else {
                // 其他字段不存在时返回默认值
                if(defaultValue == null){
                    project = project.and(fieldPath).as(alias);
                }else {
                    project = project.and(ConditionalOperators.ifNull(fieldPath).then(defaultValue)).as(alias);
                }

            }
        }
        return project;
    }

    /**
     * 向当前条件构造器中添加“或”逻辑的复合条件（orOperator）。
     *
     * @param criterias 需要“或”连接的条件（可变参数，不能为空）
     * @return 当前构造器实例，支持链式调用
     */
    @SuppressWarnings("unchecked")
    public T addOrConditions(Criteria... criterias) {
        if (criterias != null && criterias.length > 0) {
            criteria.orOperator(criterias);
            hasCriteria = true;
        }
        return (T) this;
    }
}
