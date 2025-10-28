import { Module } from "@nestjs/common";

import { AiConsoleModule } from "@/modules/console/ai/ai.module";
import { KeyManagerModule } from "@/modules/console/key-manager/key-manager.module";

import { MailAssistantController } from "./mail-assistant.controller";
import { ExchangeGraphService } from "./services/exchange-graph.service";
import { MailSummarizerService } from "./services/mail-summarizer.service";

@Module({
    imports: [AiConsoleModule, KeyManagerModule],
    controllers: [MailAssistantController],
    providers: [ExchangeGraphService, MailSummarizerService],
})
export class MailAssistantModule {}
