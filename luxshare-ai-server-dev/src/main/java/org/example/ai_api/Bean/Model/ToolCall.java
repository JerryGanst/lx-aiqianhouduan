package org.example.ai_api.Bean.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class ToolCall {
    /**
     * 工具调用ID
     */
    @JsonProperty("id")
    private String id;
    /**
     * 工具调用类型
     */
    @JsonProperty("type")
    private String type = "function";
    /**
     * 工具调用参数
     */
    @JsonProperty("function")
    private Function function;
}
