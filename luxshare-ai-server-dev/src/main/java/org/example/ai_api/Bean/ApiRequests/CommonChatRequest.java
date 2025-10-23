package org.example.ai_api.Bean.ApiRequests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.example.ai_api.Bean.Enum.Action;
import org.example.ai_api.Bean.Model.AgentConfig;

import java.util.List;
import java.util.Map;

@Setter
@Getter
public class CommonChatRequest {
    @JsonProperty("action")
    private Action action;
    @JsonProperty("thinking")
    private boolean thinking;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("session_id")
    private String sessionId;
    @JsonProperty("current_iter")
    private Integer currentIter;
    @JsonProperty("agent_configs")
    private AgentConfig agentConfig;
    @JsonProperty("user_department")
    private String userDepartment;
    @JsonProperty("metadata")
    private List<Map<String, Object>> metaData;
}
