package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class RankingResult {
    @JsonProperty("ranking")
    private List<RankingItem> ranking;
    @JsonProperty("executive_summary")
    private String executiveSummary;
    @JsonProperty("panel_summary")
    private String panelSummary;
    @JsonProperty("materials_snapshot")
    private MaterialsSnapshot materialsSnapshot;
}
