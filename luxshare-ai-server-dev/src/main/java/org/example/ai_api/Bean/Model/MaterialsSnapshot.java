package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class MaterialsSnapshot {
    @JsonProperty("candidates")
    private List<Candidate> candidates;
    @JsonProperty("jd_snapshot")
    private JdSnapshot jdSnapshot;
}
