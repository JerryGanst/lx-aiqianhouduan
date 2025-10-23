package org.example.ai_api.Bean.WebRequest;

import lombok.*;
import org.example.ai_api.Bean.Model.FileId;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class AgentKnowledgeBase {
    private String agentId;
    private List<FileId> files;
}
