package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class Candidate {
    @JsonProperty("resume_id")
    private String resumeId;
    @JsonProperty("overall_score")
    private double overallScore;
    @JsonProperty("category_score")
    private Map<String, Object> categoryScore;
    @JsonProperty("detailed_assessments")
    private Map<String, Object> detailedAssessments;
    @JsonProperty("mandatory_qualifications")
    private boolean mandatoryQualified;
    @JsonProperty("overall_assessment")
    private String overallAssessment;
}
