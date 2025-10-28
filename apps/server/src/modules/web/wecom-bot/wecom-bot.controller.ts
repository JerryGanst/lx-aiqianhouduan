import {
    BadRequestException,
    Body,
    Controller,
    Get,
    Head,
    Logger,
    Post,
    Query,
    Res,
    SetMetadata,
} from "@nestjs/common";

import { DECORATOR_KEYS } from "@common/constants/decorators-key.constant";
import { Response } from "express";

import { WecomBotSenderService } from "./wecom-bot-sender.service";
import { WecomBotService } from "./wecom-bot.service";

@SetMetadata(DECORATOR_KEYS.IS_PUBLIC_KEY, true)
@Controller("wecom/callback")
export class WecomBotController {
    private readonly logger = new Logger(WecomBotController.name);

    constructor(
        private readonly wecomBotService: WecomBotService,
        private readonly wecomBotSender: WecomBotSenderService,
    ) {}

    @SetMetadata(DECORATOR_KEYS.IS_PUBLIC_KEY, true)
    @Head()
    handleHead(@Res() res: Response): void {
        res.status(200).end();
    }

    @SetMetadata(DECORATOR_KEYS.IS_PUBLIC_KEY, true)
    @Get()
    async verify(
        @Query("msg_signature") signature: string,
        @Query("timestamp") timestamp: string,
        @Query("nonce") nonce: string,
        @Query("echostr") echostr: string,
        @Res() res: Response,
    ): Promise<void> {
        if (!signature || !timestamp || !nonce || !echostr) {
            throw new BadRequestException("Missing required query parameters");
        }

        const plain = this.wecomBotService.verifyUrl(signature, timestamp, nonce, echostr);
        this.logger.log("WeCom URL verification succeeded");
        res.status(200).type("text/plain; charset=utf-8").send(plain);
    }

    @SetMetadata(DECORATOR_KEYS.IS_PUBLIC_KEY, true)
    @Post()
    async receive(
        @Query("msg_signature") signature: string,
        @Query("timestamp") timestamp: string,
        @Query("nonce") nonce: string,
        @Body() body: any,
        @Res() res: Response,
    ): Promise<void> {
        const encrypted =
            body?.xml?.Encrypt ??
            body?.Encrypt ??
            body?.encrypt ??
            body?.Message?.Encrypt ??
            body?.message?.Encrypt;

        if (!signature || !timestamp || !nonce || !encrypted) {
            this.logger.warn(
                `Missing request parameters. signature=${signature}, timestamp=${timestamp}, nonce=${nonce}, body=${JSON.stringify(body)}`,
            );
            throw new BadRequestException("Missing required request parameters");
        }

        const payload = this.wecomBotService.decryptEvent(signature, timestamp, nonce, encrypted);
        this.logger.debug(`Received WeCom payload: ${payload.message}`);

        let replyContent = "已收到消息。";
        let replyMsgId: string | undefined;
        let toUser: string | undefined;
        let aibotId: string | undefined;
        try {
            const parsed = JSON.parse(payload.message);
            const content =
                parsed?.text?.content ??
                parsed?.content ??
                parsed?.msg?.text?.content ??
                payload.message;
            replyContent = `收到：${content}`;
            replyMsgId = parsed?.msgid;
            toUser = parsed?.from?.userid;
            aibotId = parsed?.aibotid;
        } catch {
            replyContent = `收到：${payload.message}`;
        }

        const replyPlain = JSON.stringify({
            msgid: replyMsgId,
            msgtype: "text",
            text: { content: replyContent },
        });

        const encryptedReply = this.wecomBotService.encryptResponse(
            replyPlain,
            timestamp,
            nonce,
        );

        if (aibotId && toUser) {
            this.wecomBotSender
                .sendTextMessage(aibotId, toUser, replyContent)
                .catch((err) =>
                    this.logger.error("WeCom sendTextMessage failed", err),
                );
        } else {
            this.logger.warn(
                `Missing aibotId or toUser in payload, skip proactive send. aibotId=${aibotId}, toUser=${toUser}`,
            );
        }

        res.status(200).json({
            msg_signature: encryptedReply.msgSignature,
            timestamp: encryptedReply.timeStamp,
            nonce: encryptedReply.nonce,
            encrypt: encryptedReply.encrypt,
        });
    }
}
