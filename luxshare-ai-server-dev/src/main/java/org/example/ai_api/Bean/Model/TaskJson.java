package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class TaskJson {
    @JsonProperty("task_status")
    private String taskStatus;
    @JsonProperty("uuid")
    private String uuid;
    @JsonProperty("create_time")
    private String createTime;
    @JsonProperty("end_time")
    private String endTime;
    @JsonProperty("errors")
    private List<Object> errors;
    @JsonProperty("progress")
    private Progress progress;
    @JsonProperty("jd_parse_task_id")
    private String jdParseTaskId;
    @JsonProperty("items")
    private List<ResumeTaskItem> items;
    @JsonProperty("config")
    private Map<String, Object> config;
    @JsonProperty("ranking_result")
    private RankingResult rankingResult;
    @JsonProperty("ready_to_run")
    private boolean readyToRun;
    @JsonProperty("ready_for_next")
    private boolean readyForNext;
}
