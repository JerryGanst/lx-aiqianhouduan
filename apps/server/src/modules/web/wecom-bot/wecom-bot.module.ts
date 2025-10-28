import { HttpModule } from "@nestjs/axios";
import { Module } from "@nestjs/common";

import { WecomBotController } from "./wecom-bot.controller";
import { WecomBotSenderService } from "./wecom-bot-sender.service";
import { WecomBotService } from "./wecom-bot.service";

@Module({
    imports: [HttpModule],
    controllers: [WecomBotController],
    providers: [WecomBotService, WecomBotSenderService],
    exports: [WecomBotService, WecomBotSenderService],
})
export class WecomBotModule {}
