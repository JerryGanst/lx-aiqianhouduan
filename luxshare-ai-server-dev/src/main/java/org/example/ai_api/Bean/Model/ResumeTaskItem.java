package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ResumeTaskItem {
    @JsonProperty("match_task_id")
    private String matchTaskId;
    @JsonProperty("resume_id")
    private String resumeId;
}
