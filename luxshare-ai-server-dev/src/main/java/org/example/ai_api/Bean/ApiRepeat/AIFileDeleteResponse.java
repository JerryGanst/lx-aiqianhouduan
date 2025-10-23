package org.example.ai_api.Bean.ApiRepeat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AIFileDeleteResponse {
    @JsonProperty("document_id")
    private String documentId;
    @JsonProperty("deleted")
    private boolean deleted;
}
