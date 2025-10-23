package org.example.ai_api.Bean.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.example.ai_api.Bean.ApiRepeat.ResumeRepeat;
import org.example.ai_api.Bean.ApiRepeat.ResumeTaskRepeat;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document("ResumeTask")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class ResumeTask {
    @Id
    @JsonProperty("id")
    private String id;
    @JsonProperty("userId")
    private String userId;
    @JsonProperty("title")
    private String title;
    @JsonProperty("text")
    private String text;
    @JsonProperty("JDFile")
    private FileUpload JDFile;
    @JsonProperty("resumeFiles")
    private List<FileUpload> resumeFiles;
    @JsonProperty("resumeRepeat")
    private ResumeRepeat resumeRepeat;
    @JsonProperty("resumeTaskRepeat")
    private ResumeTaskRepeat resumeTaskRepeat;
    @JsonProperty("complete")
    private boolean complete;
    @JsonProperty("createTime")
    private String createTime;
    @JsonProperty("lastUpdateTime")
    private String lastUpdateTime;
}
