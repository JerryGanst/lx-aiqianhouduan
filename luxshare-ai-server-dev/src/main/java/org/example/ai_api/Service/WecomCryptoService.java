package org.example.ai_api.Service;

import lombok.extern.slf4j.Slf4j;
import org.example.ai_api.Exception.AesException;
import org.example.ai_api.WecomCrypto.WXBizJsonMsgCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * 企业微信加解密服务封装
 * 将 {@link WXBizJsonMsgCrypt} 封装为 Spring Service，便于业务直接调用。
 */
@Slf4j
@Service
public class WecomCryptoService {

    @Value("${wecom.token:}")
    private String token;

    @Value("${wecom.encodingAesKey:}")
    private String encodingAesKey;

    /**
     * 接收者标识：企业ID（corpId）或应用的receiveid，具体含义见企业微信文档
     */
    @Value("${wecom.receiveId:}")
    private String receiveId;

    private WXBizJsonMsgCrypt crypt;

    @PostConstruct
    public void init() {
        if (isBlank(token) || isBlank(encodingAesKey) || isBlank(receiveId)) {
            log.warn("WecomCryptoService not fully configured. token/encodingAesKey/receiveId are required");
            return;
        }
        try {
            this.crypt = new WXBizJsonMsgCrypt(token, encodingAesKey, receiveId);
            log.info("WecomCryptoService initialized with receiveId:{}", receiveId);
        } catch (AesException e) {
            // 配置不正确直接失败，避免运行时再抛出
            throw new IllegalStateException("Invalid WeCom crypto configuration: " + e.getMessage(), e);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * 验证企业微信回调 URL（开发者第一次配置回调时使用）。
     *
     * @param msgSignature URL 参数 msg_signature
     * @param timestamp    URL 参数 timestamp
     * @param nonce        URL 参数 nonce
     * @param echoStr      URL 参数 echostr
     * @return 解密后的 echostr（原样返回给企业微信）
     * @throws AesException 加解密异常
     */
    public String verifyUrl(String msgSignature, String timestamp, String nonce, String echoStr) throws AesException {
        ensureReady();
        return crypt.VerifyURL(msgSignature, timestamp, nonce, echoStr);
    }

    /**
     * 解密企业微信推送的消息体。
     *
     * @param msgSignature URL/请求头中的 msg_signature
     * @param timestamp    URL/请求头中的 timestamp
     * @param nonce        URL/请求头中的 nonce
     * @param postData     请求体原始 JSON 字符串
     * @return 解密后的明文 JSON 字符串
     * @throws AesException 加解密异常或签名校验失败
     */
    public String decryptMessage(String msgSignature, String timestamp, String nonce, String postData) throws AesException {
        ensureReady();
        return crypt.DecryptMsg(msgSignature, timestamp, nonce, postData);
    }

    /**
     * 加密回复给企业微信的 JSON 消息。
     *
     * @param replyJson  明文 JSON
     * @param timestamp  可传入当前时间戳（若为空则内部自动生成）
     * @param nonce      可传入随机串
     * @return 封装后的密文 JSON（包含 msg_signature、timestamp、nonce、encrypt）
     * @throws AesException 加密失败
     */
    public String encryptReply(String replyJson, String timestamp, String nonce) throws AesException {
        ensureReady();
        return crypt.EncryptMsg(replyJson, timestamp, nonce);
    }

    private void ensureReady() {
        if (this.crypt == null) {
            throw new IllegalStateException("WecomCryptoService is not configured. Please set wecom.token, wecom.encodingAesKey, wecom.receiveId");
        }
    }
}

