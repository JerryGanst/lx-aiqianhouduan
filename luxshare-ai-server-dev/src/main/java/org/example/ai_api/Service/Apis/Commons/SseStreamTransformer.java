package org.example.ai_api.Service.Apis.Commons;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ai_api.Bean.ApiRepeat.UnifiedStreamEvent;
import org.example.ai_api.Bean.ApiRepeat.UpstreamSseEvent;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
public class SseStreamTransformer {
    private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();

    public static UnifiedStreamEvent transform(JsonNode event) {
        if (event == null || event.isNull()) {
            return base("thinking", null, null, System.currentTimeMillis(), false);
        }
        String type = text(event, "type");
        String channel = text(event, "channel");
        String lifecycle = text(event, "lifecycle");
        String content = text(event, "content");
        if (content == null) {
            content = text(event, "delta");
        }
        String component = text(event, "component");
        Long ts = longVal(event, "ts");
        if (ts == null) ts = longVal(event, "timestamp");
        return transform(type, channel, lifecycle, content, component, ts);
    }

    public static UnifiedStreamEvent transform(Map<String, Object> event) {
        if (event == null) {
            return base("thinking", null, null, System.currentTimeMillis(), false);
        }
        String type = str(event.get("type"));
        String channel = str(event.get("channel"));
        String lifecycle = str(event.get("lifecycle"));
        String content = str(event.get("content"));
        if (content == null) content = str(event.get("delta"));
        String component = str(event.get("component"));
        Long ts = longVal(event.get("ts"));
        if (ts == null) ts = longVal(event.get("timestamp"));
        return transform(type, channel, lifecycle, content, component, ts);
    }

    public static UnifiedStreamEvent transform(String rawJson) {
        if (rawJson == null || rawJson.isEmpty()) {
            return base("thinking", null, null, System.currentTimeMillis(), false);
        }
        try {
            JsonNode node = DEFAULT_MAPPER.readTree(rawJson);
            return transform(node);
        } catch (Exception e) {
            // 无法解析则回退为思考阶段占位，避免中断前端渲染
            return base("thinking", rawJson, null, System.currentTimeMillis(), false);
        }
    }

    public static UnifiedStreamEvent transform(String type,
                                               String channel,
                                               String lifecycle,
                                               String content,
                                               String component,
                                               Long timestamp) {
        String stage = "thinking";
        boolean done = false;

        if (type == null) type = "";
        if (channel == null) channel = "";
        if (lifecycle == null) lifecycle = "";

        switch (type) {
            case "llm":
                if (Objects.equals(channel, "text")) {
                    stage = "answering";
                } else if (Objects.equals(channel, "reasoning")) {
                    stage = "thinking";
                }
                done = Objects.equals(lifecycle, "end");
                break;
            case "final":
                stage = "answering";
                done = true;
                break;
            case "notice":
                if (content != null && content.contains("已生成")) {
                    stage = "answering";
                    done = true;
                } else {
                    stage = "thinking";
                }
                break;
            case "status":
            case "summarize_start":
            case "summarize_done":
            case "tool":
                stage = "thinking";
                break;
            case "error":
                stage = "error";
                done = true;
                break;
            default:
                stage = "thinking";
                break;
        }

        long ts = (timestamp != null) ? timestamp : System.currentTimeMillis();
        return UnifiedStreamEvent.builder()
                .type("stream")
                .stage(stage)
                .content(content)
                .component(component)
                .timestamp(ts)
                .done(done)
                .build();
    }

    private static UnifiedStreamEvent base(String stage, String content, String component, Long ts, boolean done) {
        return UnifiedStreamEvent.builder()
                .type("stream")
                .stage(stage)
                .content(content)
                .component(component)
                .timestamp(ts != null ? ts : System.currentTimeMillis())
                .done(done)
                .build();
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) return null;
        JsonNode v = node.get(field);
        if (v.isTextual()) return v.asText();
        if (v.isNumber()) return v.asText();
        return v.toString();
    }

    private static Long longVal(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) return null;
        JsonNode v = node.get(field);
        if (v.isNumber()) return v.asLong();
        if (v.isTextual()) {
            try { return Long.parseLong(v.asText()); } catch (Exception ignored) {}
        }
        return null;
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Long longVal(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception ignored) {}
        return null;
    }

    /**
     * 基于上游事件实体的转换。
     */
    public static UnifiedStreamEvent transform(UpstreamSseEvent evt) {
        if (evt == null) {
            return base("thinking", null, null, System.currentTimeMillis(), false);
        }
        String type = evt.getType();
        String channel = evt.getChannel();
        String lifecycle = evt.getLifecycle();
        String content = (evt.getContent() != null && !evt.getContent().isEmpty()) ? evt.getContent() : evt.getDelta();
        String component = evt.getComponent();
        Long ts = evt.getTs() != null ? evt.getTs() : evt.getTimestamp();
        return transform(type, channel, lifecycle, content, component, ts);
    }
}
