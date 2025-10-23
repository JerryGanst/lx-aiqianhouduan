package org.example.ai_api.Bean.ApiRequests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.example.ai_api.Bean.Model.ResumeItem;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class ResumeRequest {
    @JsonProperty("jd_basic")
    private String jdBasic;
    @JsonProperty("resumes")
    private List<ResumeItem> resumes;
    @JsonProperty("callback_url")
    private String callbackUrl;
}
