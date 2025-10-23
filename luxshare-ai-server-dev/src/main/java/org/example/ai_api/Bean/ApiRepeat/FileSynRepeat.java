package org.example.ai_api.Bean.ApiRepeat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class FileSynRepeat {
    @JsonProperty("success")
    private Boolean success;
    @JsonProperty("document_id")
    private String documentId;
    @JsonProperty("document_abstract")
    private String documentAbstract;
    @JsonProperty("steps")
    private Object steps;
    @JsonProperty("duplicate_reason")
    private String duplicateReason;
    @JsonProperty("errors")
    private List<String> errors;
    @JsonProperty("execution_time")
    private Object executionTime;
    @JsonProperty("rolled_back")
    private Boolean rolledBack;
    @JsonProperty("rollback_success")
    private Boolean rollbackSuccess;
}
