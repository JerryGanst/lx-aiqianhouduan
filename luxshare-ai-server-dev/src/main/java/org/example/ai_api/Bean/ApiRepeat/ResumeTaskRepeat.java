package org.example.ai_api.Bean.ApiRepeat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.example.ai_api.Bean.Model.TaskJson;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class ResumeTaskRepeat {
    @JsonProperty("task_completed")
    private boolean taskCompleted;
    @JsonProperty("task_json")
    private TaskJson taskJson;
}
