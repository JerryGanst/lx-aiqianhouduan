package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RequirementClassificationWeights {
    @JsonProperty("education")
    private int education;
    @JsonProperty("motivation_and_values")
    private int motivationAndValues;
    @JsonProperty("others")
    private int others;
    @JsonProperty("skills")
    private int skills;
    @JsonProperty("soft_skills")
    private int softSkills;
    @JsonProperty("work_experience")
    private int workExperience;
}
