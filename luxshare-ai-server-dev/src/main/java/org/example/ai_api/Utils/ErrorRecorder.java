package org.example.ai_api.Utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;

/**
 * 写入计数器的每小时错误数
 * @author 10353965
 */
@Component
public class ErrorRecorder {
    @Autowired
    private ErrorCounter errorCounter;
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 记录当前小时内每分钟的错误数
     */
    private final int[] minuteErrors = new int[60];
    private int currentMinute = 0;

    /**
     * 每分钟记录一次
     */
    @Scheduled(cron = "0 * * * * ?")
    public void recordMinuteError() {
        minuteErrors[currentMinute] = errorCounter.getAndReset();
        currentMinute = (currentMinute + 1) % 60;
    }

    /**
     * 每小时整点写入一次统计数据
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void recordHourError() {
        HourlyStatsUtil.recordHourStats(
            mongoTemplate,
            "error_hour_record",
            "totalError",
            "avgErrorPerMinute",
            "maxErrorPerMinute",
            "minErrorPerMinute",
            "minuteErrors",
            minuteErrors,
            new Date(),
            null
        );
        Arrays.fill(minuteErrors, 0);
        currentMinute = 0;
    }
}
