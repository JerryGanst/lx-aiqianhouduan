package org.example.ai_api.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.example.ai_api.WecomCrypto.WXBizJsonMsgCrypt;

/**
 * WeCom (企业微信) 加解密与签名验证服务。
 * 依赖项目中的 WecomCrypto 包：WXBizJsonMsgCrypt、SHA1、PKCS7Encoder、ByteGroup、JsonParse、AesException。
 */
@Service
public class WecomCryptoService {
    @Value("{wecom.token}") private String token;
    @Value("{wecom.encoding-aes-key}") private String aesKey;
    @Value("{wecom.corp-id}") private String corpId;

    private WXBizJsonMsgCrypt newCrypt() {
        return new WXBizJsonMsgCrypt(token, aesKey, corpId);
    }

    /** URL 有效性验证：解密 echostr 并返回明文 */
    public String verifyUrl(String signature, String timestamp, String nonce, String echostr) {
        return newCrypt().VerifyURL(signature, timestamp, nonce, echostr);
    }

    /** 收到消息/事件：解密 Encrypt 字段，返回明文 JSON */
    public String decrypt(String signature, String timestamp, String nonce, String body) {
        return newCrypt().DecryptMsg(signature, timestamp, nonce, body);
    }

    /** 被动回复：把明文 JSON 加密返回给企业微信 */
    public String encrypt(String replyJson, String timestamp, String nonce) {
        return newCrypt().EncryptMsg(replyJson, timestamp, nonce);
    }
}
