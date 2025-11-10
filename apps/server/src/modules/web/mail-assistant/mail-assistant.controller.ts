import { BaseController } from "@common/base/controllers/base.controller";
import { WebController } from "@common/decorators/controller.decorator";
import { Public } from "@common/decorators/public.decorator";
import { HttpExceptionFactory } from "@common/exceptions/http-exception.factory";
import { Body, Post } from "@nestjs/common";

import { MailSummarizerService } from "./services/mail-summarizer.service";
import { MailSummaryRequestDto } from "./dto/mail-summary-request.dto";

@WebController("mail-assistant")
export class MailAssistantController extends BaseController {
    constructor(private readonly mailSummarizerService: MailSummarizerService) {
        super();
    }

    @Public()
    @Post("summary")
    async summarizeMailbox(@Body() dto: MailSummaryRequestDto) {
        const hasToken = dto.accessToken && dto.accessToken.trim().length > 0;
        const hasText = dto.text && dto.text.trim().length > 0;

        if (!hasToken && !hasText) {
            throw HttpExceptionFactory.badRequest(
                "Either accessToken or text must be provided for summarization.",
            );
        }

        return this.mailSummarizerService.summarize(dto);
    }
}
