package org.example.ai_api.Bean.ApiRepeat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.example.ai_api.Bean.Model.MatchTask;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class ResumeRepeat {
    @JsonProperty("batch_id")
    private String batchId;
    @JsonProperty("jd_parse_task_id")
    private String jdParseTaskId;
    @JsonProperty("match_tasks")
    private List<MatchTask> matchTasks;
}
