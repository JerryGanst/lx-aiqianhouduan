package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class MatchTask {
    @JsonProperty("resume_id")
    private String resumeId;
    @JsonProperty("task_id")
    private String taskId;
}
