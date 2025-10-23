package org.example.ai_api.Bean.WebRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ResumeCallback {
    @JsonProperty("batch_id")
    private String batchId;
    @JsonProperty("status")
    private String status;
}
