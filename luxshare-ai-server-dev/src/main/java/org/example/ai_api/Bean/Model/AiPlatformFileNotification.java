package org.example.ai_api.Bean.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiPlatformFileNotification {
    private NotificationType type; // 操作类型：ADD 或 DELETE
    private List<Map<String, String>> files; // 文件信息列表

    public enum NotificationType {
        ADD,
        DELETE
    }
}
