import { DynamicModule, Module } from "@nestjs/common";

import { AiModule } from "./ai/ai.module";
import { AuthModule } from "./auth/auth.module";
import { ConfigModule } from "./config/config.module";
import { DecorateModule } from "./decorate/decorate.module";
import { MailAssistantModule } from "./mail-assistant/mail-assistant.module";
import { PayModule } from "./pay/pay.module";
import { RechargeModule } from "./recharge/recharge.modeule";
import { UploadModule } from "./upload/upload.module";
import { UserModule } from "./user/user.module";
import { WecomBotModule } from "./wecom-bot/wecom-bot.module";

@Module({})
export class WebModule {
    static register(): DynamicModule {
        const baseImports = [
            AuthModule,
            UploadModule,
            AiModule,
            DecorateModule,
            UserModule,
            ConfigModule,
            RechargeModule,
            PayModule,
            MailAssistantModule,
        ];

        const includeWecom =
            process.env.WECOM_BOT_ENABLED === "true" ||
            (!!process.env.WECOM_BOT_TOKEN && !!process.env.WECOM_BOT_ENCODING_AES_KEY);

        const imports = includeWecom ? [...baseImports, WecomBotModule] : baseImports;

        const exports = includeWecom
            ? [...baseImports, WecomBotModule]
            : [...baseImports];

        return {
            module: WebModule,
            imports,
            exports,
        };
    }
}
