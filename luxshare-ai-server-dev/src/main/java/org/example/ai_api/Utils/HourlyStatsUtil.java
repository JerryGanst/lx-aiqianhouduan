package org.example.ai_api.Utils;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

/**
 * 工具类-写入每小时统计数据到MongoDB
 */
public class HourlyStatsUtil {
    /**
     * 写入每小时统计数据到MongoDB
     * @param mongoTemplate MongoTemplate实例
     * @param collectionName 集合名
     * @param totalField 总数字段名
     * @param avgField 平均值字段名
     * @param maxField 最大值字段名
     * @param minField 最小值字段名
     * @param detailField 明细数组字段名
     * @param data 统计数组（如每分钟QPS）
     * @param timestamp 统计时间（小时整点）
     * @param extraFields 额外字段（如功能名等，以map结构传入，可为null）
     */
    public static void recordHourStats(
            MongoTemplate mongoTemplate,
            String collectionName,
            String totalField,
            String avgField,
            String maxField,
            String minField,
            String detailField,
            int[] data,
            Date timestamp,
            Map<String, Object> extraFields
    ) {
        int sum = Arrays.stream(data).sum();
        int max = Arrays.stream(data).max().orElse(0);
        int min = Arrays.stream(data).min().orElse(0);
        double avg = Arrays.stream(data).average().orElse(0.0);

        Document doc = new Document();
        doc.put("timestamp", timestamp);
        doc.put(totalField, sum);
        doc.put(avgField, avg);
        doc.put(maxField, max);
        doc.put(minField, min);
        doc.put(detailField, Arrays.stream(data).boxed().collect(java.util.stream.Collectors.toList()));
        if (extraFields != null) doc.putAll(extraFields);

        mongoTemplate.getCollection(collectionName).insertOne(doc);
    }
} 