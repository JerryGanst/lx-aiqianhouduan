package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class JdSnapshot {
    @JsonProperty("jd_task_id")
    private String jdTaskId;
    @JsonProperty("requirement_classification_weights")
    private RequirementClassificationWeights requirementClassificationWeights;
}
