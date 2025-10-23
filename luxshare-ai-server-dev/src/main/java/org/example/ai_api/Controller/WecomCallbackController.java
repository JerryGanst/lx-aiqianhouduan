package org.example.ai_api.Controller;

import lombok.extern.slf4j.Slf4j;
import org.example.ai_api.Exception.AesException;
import org.example.ai_api.Service.WecomCryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/wecom/callback")
public class WecomCallbackController {

    private static final Logger logger = LoggerFactory.getLogger(WecomCallbackController.class);

    @Autowired
    private WecomCryptoService wecomCryptoService;

    /**
     * 企业微信 URL 验证（GET）。
     * 要求：
     * 1. 对请求参数先做 URLDecode
     * 2. 使用验证函数校验签名并解密 echostr
     * 3. 在 1 秒内返回明文（纯文本，无引号、无换行、无 BOM）
     */
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verify(
            @RequestParam("msg_signature") String msgSignature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestParam("echostr") String echoStr
    ) {
        logger.info("msgSignature {}",msgSignature);
        logger.info("timestamp {}",timestamp);
        logger.info("nonce {}",nonce);
        logger.info("echoStr {}",echoStr);
        try {
            String dMsgSignature = urlDecode(msgSignature);
            String dTimestamp = urlDecode(timestamp);
            String dNonce = urlDecode(nonce);
            String dEchoStr = urlDecode(echoStr);

            String plainEcho = wecomCryptoService.verifyUrl(dMsgSignature, dTimestamp, dNonce, dEchoStr);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(plainEcho);
        } catch (AesException e) {
            log.warn("WeCom URL verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("invalid");
        } catch (Exception e) {
            log.error("WeCom URL verification error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("error");
        }
    }

    private String urlDecode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8.name());
        } catch (Exception ignore) {
            return s;
        }
    }
}
