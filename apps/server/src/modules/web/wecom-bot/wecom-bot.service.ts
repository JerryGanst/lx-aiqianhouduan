import { BadRequestException, ForbiddenException, Injectable, Logger } from "@nestjs/common";
import { ConfigService } from "@nestjs/config";
import { createCipheriv, createDecipheriv, createHash, randomBytes } from "crypto";

interface DecryptedPayload {
    message: string;
    receiveId: string;
}

@Injectable()
export class WecomBotService {
    private readonly logger = new Logger(WecomBotService.name);
    private readonly token: string;
    private readonly aesKey: Buffer;
    private readonly receiveId: string;
    private readonly iv: Buffer;

    constructor(private readonly configService: ConfigService) {
        this.token = this.getRequiredConfig("WECOM_BOT_TOKEN");
        const encodingAesKey = this.getRequiredConfig("WECOM_BOT_ENCODING_AES_KEY");
        this.receiveId =
            this.configService.get<string>("WECOM_BOT_RECEIVE_ID")?.trim() ||
            this.configService.get<string>("WECOM_BOT_CORP_ID")?.trim() ||
            "";

        this.aesKey = Buffer.from(`${encodingAesKey}=`, "base64");
        if (this.aesKey.length !== 32) {
            throw new Error("Invalid EncodingAESKey: expecting 43-char Base64 string");
        }

        this.iv = this.aesKey.subarray(0, 16);
    }

    verifyUrl(signature: string, timestamp: string, nonce: string, echoStr: string): string {
        this.assertSignature(signature, timestamp, nonce, echoStr);
        const { message } = this.decrypt(echoStr);
        this.logger.debug(`URL verification succeeded. receiveId='${this.receiveId}'`);
        return message;
    }

    decryptEvent(signature: string, timestamp: string, nonce: string, encrypted: string): DecryptedPayload {
        this.assertSignature(signature, timestamp, nonce, encrypted);
        return this.decrypt(encrypted);
    }

    encryptResponse(message: string, timestamp?: string, nonce?: string) {
        const useTimestamp = timestamp ?? Math.floor(Date.now() / 1000).toString();
        const useNonce = nonce ?? randomBytes(8).toString("hex");
        const encrypted = this.encrypt(message);
        const msgSignature = this.calculateSignature(useTimestamp, useNonce, encrypted);
        return {
            msgSignature,
            timeStamp: useTimestamp,
            nonce: useNonce,
            encrypt: encrypted,
        };
    }

    private decrypt(encrypted: string): DecryptedPayload {
        try {
            const decipher = createDecipheriv("aes-256-cbc", this.aesKey, this.iv);
            decipher.setAutoPadding(false);
            const decrypted = Buffer.concat([decipher.update(encrypted, "base64"), decipher.final()]);

            const unpadded = this.pkcs7Decode(decrypted);
            const msgLength = unpadded.readUInt32BE(16);
            const message = unpadded.subarray(20, 20 + msgLength).toString();
            const receiveId = unpadded.subarray(20 + msgLength).toString();

            if (this.receiveId && receiveId && receiveId !== this.receiveId) {
                throw new ForbiddenException("receiveId mismatch");
            }

            return {
                message,
                receiveId,
            };
        } catch (error) {
            throw new BadRequestException(`Failed to decrypt payload: ${error.message}`);
        }
    }

    private encrypt(message: string): string {
        const random = randomBytes(16);
        const msgBuffer = Buffer.from(message);
        const receiveIdBuffer = Buffer.from(this.receiveId ?? "");
        const msgLengthBuffer = Buffer.alloc(4);
        msgLengthBuffer.writeUInt32BE(msgBuffer.length, 0);

        const raw = Buffer.concat([random, msgLengthBuffer, msgBuffer, receiveIdBuffer]);
        const padded = this.pkcs7Encode(raw);

        const cipher = createCipheriv("aes-256-cbc", this.aesKey, this.iv);
        cipher.setAutoPadding(false);
        const encrypted = Buffer.concat([cipher.update(padded), cipher.final()]);
        return encrypted.toString("base64");
    }

    private pkcs7Decode(buffer: Buffer): Buffer {
        const pad = buffer[buffer.length - 1];
        if (pad < 1 || pad > 32) {
            return buffer;
        }
        return buffer.subarray(0, buffer.length - pad);
    }

    private pkcs7Encode(buffer: Buffer): Buffer {
        const blockSize = 32;
        const remainder = buffer.length % blockSize;
        const pad = remainder === 0 ? blockSize : blockSize - remainder;
        const padBuffer = Buffer.alloc(pad, pad);
        return Buffer.concat([buffer, padBuffer]);
    }

    private calculateSignature(timestamp: string, nonce: string, encrypted: string): string {
        return createHash("sha1").update([this.token, timestamp, nonce, encrypted].sort().join("")).digest("hex");
    }

    private assertSignature(signature: string, timestamp: string, nonce: string, encrypted: string) {
        const expected = this.calculateSignature(timestamp, nonce, encrypted);
        if (expected !== signature) {
            this.logger.warn(
                `Signature mismatch. expected=${expected}, provided=${signature}, timestamp=${timestamp}, nonce=${nonce}`,
            );
            throw new ForbiddenException("Signature verification failed");
        }
    }

    private getRequiredConfig(key: string): string {
        const value = this.configService.get<string>(key);
        if (!value) {
            throw new Error(`Missing required environment variable: ${key}`);
        }
        return value;
    }
}
