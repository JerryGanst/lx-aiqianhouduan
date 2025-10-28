import { HttpService } from "@nestjs/axios";
import { Injectable, Logger } from "@nestjs/common";
import { ConfigService } from "@nestjs/config";
import { catchError, firstValueFrom, map, throwError, timeout } from "rxjs";

interface AccessTokenResponse {
    errcode: number;
    errmsg: string;
    access_token?: string;
    expires_in?: number;
}

interface SendResponse {
    errcode: number;
    errmsg: string;
    msgid?: string;
}

@Injectable()
export class WecomBotSenderService {
    private readonly logger = new Logger(WecomBotSenderService.name);
    private cachedToken?: { token: string; expiresAt: number };

    private readonly corpId: string | undefined;
    private readonly corpSecret: string | undefined;
    private readonly baseUrl: string;

    constructor(private readonly configService: ConfigService, private readonly http: HttpService) {
        this.corpId = this.configService.get<string>("WECOM_BOT_CORP_ID");
        this.corpSecret = this.configService.get<string>("WECOM_BOT_CORP_SECRET");
        this.baseUrl =
            this.configService.get<string>("WECOM_BOT_API_BASE_URL") ||
            "https://qyapi.weixin.qq.com";
    }

    private isSenderConfigured(): boolean {
        return Boolean(this.corpId && this.corpSecret);
    }

    private async getAccessToken(): Promise<string | null> {
        if (!this.isSenderConfigured()) {
            this.logger.warn("WeCom sender credentials not configured, skip replying");
            return null;
        }

        const now = Date.now();
        if (this.cachedToken && this.cachedToken.expiresAt > now + 60 * 1000) {
            return this.cachedToken.token;
        }

        const url = `${this.baseUrl}/cgi-bin/gettoken`;
        const observable = this.http
            .get<AccessTokenResponse>(url, {
                params: {
                    corpid: this.corpId,
                    corpsecret: this.corpSecret,
                },
            })
            .pipe(
                timeout(5000),
                map((res) => res.data),
                catchError((err) => {
                    this.logger.error("Failed to fetch access token", err);
                    return throwError(() => err);
                }),
            );

        const data = await firstValueFrom(observable);
        if (data.errcode !== 0 || !data.access_token) {
            this.logger.error(`WeCom access token error: ${data.errmsg} (${data.errcode})`);
            return null;
        }

        const expiresInMs = (data.expires_in ?? 7200) * 1000;
        this.cachedToken = {
            token: data.access_token,
            expiresAt: now + expiresInMs,
        };

        return data.access_token;
    }

    async sendTextMessage(aibotId: string, toUser: string, content: string): Promise<void> {
        const token = await this.getAccessToken();
        if (!token) {
            return;
        }

        const url = `${this.baseUrl}/cgi-bin/aibot/send?access_token=${token}`;
        const payload = {
            aibotid: aibotId,
            touser: toUser,
            msgtype: "text",
            text: {
                content,
            },
        };

        try {
            const data = await firstValueFrom(
                this.http.post<SendResponse>(url, payload).pipe(
                    timeout(5000),
                    map((res) => res.data),
                ),
            );

            if (data.errcode !== 0) {
                this.logger.warn(
                    `WeCom sendTextMessage failed: ${data.errmsg} (${data.errcode})`,
                );
            } else {
                this.logger.debug(`WeCom message sent, msgid=${data.msgid ?? "unknown"}`);
            }
        } catch (error) {
            this.logger.error("WeCom sendTextMessage error", error);
        }
    }
}

