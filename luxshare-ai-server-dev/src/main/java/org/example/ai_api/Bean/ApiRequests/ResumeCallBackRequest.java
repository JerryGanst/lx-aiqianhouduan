package org.example.ai_api.Bean.ApiRequests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class ResumeCallBackRequest {
    @JsonProperty("task_id")
    private String taskId;
    @JsonProperty("task_type")
    private String taskType;
}
