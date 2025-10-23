package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class RankingItem {
    @JsonProperty("rank")
    private int rank;
    @JsonProperty("name")
    private String name;
    @JsonProperty("resume_id")
    private String resumeId;
    @JsonProperty("final_score")
    private double finalScore;
    @JsonProperty("highlights")
    private List<String> highlights;
    @JsonProperty("concerns")
    private List<String> concerns;
    @JsonProperty("reasons")
    private String reasons;
}
