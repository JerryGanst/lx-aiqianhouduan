package org.example.ai_api.Bean.Entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
@Document(collection = "Session")
public class SessionInfo {
    /**
     * 会话id
     */
    @Id
    private String id;
    /**
     * 用户id
     */
    @JsonProperty("userId")
    private String userId;
    /**
     * 用户名
     */
    private String userName;
    /**
     * 用户部门
     */
    private String department;
    /**
     * 会话开始时间
     */
    @JsonProperty("startTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;
    /**
     * 会话结束时间
     */
    @JsonProperty("endTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;
    /**
     * 会话时长(毫秒)
     */
    private Long duration;
    /**
     * 此次会话调用对话接口的次数
     */
    private Long count = 0L;
}
