package org.example.ai_api.Utils;

import org.example.ai_api.Aop.FunctionCountAspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 功能调用计数记录器.
 */
@Component
public class FunctionCountRecorder {

    @Autowired
    private FunctionCountAspect functionCountAspect;
    @Autowired
    private MongoTemplate mongoTemplate;

    // 记录当前小时内每分钟所有功能的计数
    private final Map<String, int[]> functionMinuteCounts = new HashMap<>();
    private int currentMinute = 0;

    // 每分钟记录一次
    @Scheduled(cron = "0 * * * * ?")
    public void recordMinuteFunctionCounts() {
        Map<String, Integer> counts = functionCountAspect.getAndResetAllCounts();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            functionMinuteCounts.computeIfAbsent(entry.getKey(), k -> new int[60]);
            functionMinuteCounts.get(entry.getKey())[currentMinute] = entry.getValue();
        }
        // 对于本小时未被调用的功能，补0
        for (String key : functionMinuteCounts.keySet()) {
            if (!counts.containsKey(key)) {
                functionMinuteCounts.get(key)[currentMinute] = 0;
            }
        }
        currentMinute = (currentMinute + 1) % 60;
    }

    // 每小时整点写入一次统计数据
    @Scheduled(cron = "0 0 * * * ?")
    public void recordHourFunctionCounts() {
        Date now = new Date();
        for (Map.Entry<String, int[]> entry : functionMinuteCounts.entrySet()) {
            String functionName = entry.getKey();
            int[] minuteCounts = entry.getValue();
            Map<String, Object> extra = new HashMap<>();
            extra.put("functionName", functionName);
            HourlyStatsUtil.recordHourStats(
                mongoTemplate,
                "function_hour_record",
                "totalCount",
                "avgCountPerMinute",
                "maxCountPerMinute",
                "minCountPerMinute",
                "minuteCounts",
                minuteCounts,
                now,
                extra
            );
        }
        functionMinuteCounts.clear();
        currentMinute = 0;
    }
}