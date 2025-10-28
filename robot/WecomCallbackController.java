package org.example.ai_api.Controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.ai_api.Service.WecomCryptoService;

/**
 * 企业微信 智能机器人 回调控制器：
 *  - GET /wecom/callback  : URL 验证（返回明文 echostr，text/plain）
 *  - POST /wecom/callback : 接收消息/事件（解密后处理，再加密被动回复）
 */
@RestController
@RequestMapping("/wecom")
public class WecomCallbackController {
    private final WecomCryptoService crypto;

    public WecomCallbackController(WecomCryptoService crypto) {
        this.crypto = crypto;
    }

    /** ① URL 有效性验证（返回 text/plain 明文） */
    @GetMapping(value = "/callback", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verifyUrl(
            @RequestParam("msg_signature") String signature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestParam("echostr") String echostr) {
        try {
            String plain = crypto.verifyUrl(signature, timestamp, nonce, echostr);
            return ResponseEntity.ok(plain); // 必须返回纯文本明文 echostr（不可 JSON/不可加引号）
        } catch (Exception e) {
            return ResponseEntity.status(403).body("signature verify failed");
        }
    }

    /** ② 接收消息/事件 + 被动回复（最小文本示例） */
    @PostMapping(value = "/callback", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> receive(
            @RequestParam("msg_signature") String signature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestBody String body) {
        try {
            String msgPlainJson = crypto.decrypt(signature, timestamp, nonce, body);
            // TODO: 解析 msgPlainJson 的 MsgType/Event 并路由业务。
            String replyPlainJson = "{\"msgtype\":\"text\",\"text\":{\"content\":\"您好，消息已收到。\"}}";
            String encrypted = crypto.encrypt(replyPlainJson, timestamp, nonce);
            return ResponseEntity.ok(encrypted);
        } catch (Exception e) {
            // 失败情况下也返回 200 避免大量重试，具体策略按需调整
            return ResponseEntity.ok("{}");
        }
    }
}
