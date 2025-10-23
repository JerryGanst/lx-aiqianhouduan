package org.example.ai_api.Utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;

/**
 * 每小时记录一次QPS.
 */
@Component
public class QpsRecorder {
    @Autowired
    private QpsCounter qpsCounter;
    @Autowired
    private MongoTemplate mongoTemplate;

    // 记录当前小时内每分钟的QPS
    private final int[] minuteQps = new int[60];
    private int currentMinute = 0;

    // 每分钟记录一次
    @Scheduled(cron = "0 * * * * ?")
    public void recordMinuteQps() {
        minuteQps[currentMinute] = qpsCounter.getAndReset();
        currentMinute = (currentMinute + 1) % 60;
    }

    // 每小时整点写入一次统计数据
    @Scheduled(cron = "0 0 * * * ?")
    public void recordHourQps() {
        HourlyStatsUtil.recordHourStats(
            mongoTemplate,
            "qps_hour_record",
            "totalQps",
            "avgQpsPerMinute",
            "maxQpsPerMinute",
            "minQpsPerMinute",
            "minuteQps",
            minuteQps,
            new Date(),
            null
        );
        Arrays.fill(minuteQps, 0);
        currentMinute = 0;
    }
}
