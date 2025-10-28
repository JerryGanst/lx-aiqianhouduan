import { Injectable, Logger } from "@nestjs/common";
import { getProvider, TextGenerator } from "@sdk/ai";

import { getProviderKeyConfig } from "@/common/utils/helper.util";
import { AiModelService } from "@/modules/console/ai/services/ai-model.service";
import { KeyConfigService } from "@/modules/console/key-manager/services/key-config.service";

import { MailSummaryRequestDto } from "../dto/mail-summary-request.dto";
import {
    FolderMessages,
    MailSummaryResult,
    NormalizedMailItem,
} from "../interfaces/mail-summary.interface";
import { ExchangeGraphService } from "./exchange-graph.service";

@Injectable()
export class MailSummarizerService {
    private readonly logger = new Logger(MailSummarizerService.name);

    constructor(
        private readonly aiModelService: AiModelService,
        private readonly keyConfigService: KeyConfigService,
        private readonly exchangeGraphService: ExchangeGraphService,
    ) {}

    async summarize(dto: MailSummaryRequestDto): Promise<MailSummaryResult> {
        const startTime = Date.now();
        const inlineMode = Boolean(dto.text && dto.text.trim().length > 0);

        const folders =
            !inlineMode && dto.folders && dto.folders.length > 0
                ? dto.folders
                : [{ id: "inbox", name: "Inbox" }];

        const top = dto.top ?? 20;
        const includeRead = dto.includeRead ?? false;

        const folderMessages: FolderMessages[] = [];

        if (inlineMode) {
            folderMessages.push(this.buildInlineFolder(dto));
        } else {
            for (const folder of folders) {
                try {
                    const messages = await this.exchangeGraphService.fetchFolderMessages(
                        dto.accessToken!,
                        {
                            folderId: folder.id,
                            folderName: folder.name,
                            top,
                            includeRead,
                        },
                    );

                    if (messages) {
                        folderMessages.push(messages);
                    }
                } catch (error) {
                    this.logger.error(
                        `Failed to fetch messages for folder ${folder.id}: ${error.message}`,
                    );
                }
            }
        }

        const flattened = folderMessages.flatMap((folder) => folder.items);

        if (flattened.length === 0) {
            const latencyMs = Date.now() - startTime;
            return {
                metadata: {
                    totalMessages: 0,
                    folders: folderMessages.map((folder) => ({
                        id: folder.folderId,
                        name: folder.folderName,
                        messageCount: folder.items.length,
                    })),
                    processedMessageIds: [],
                },
                summary: {
                    globalSummary: "暂无需要处理的邮件。",
                    folderSummaries: [],
                    suggestedActions: [],
                    followUpReminders: [],
                },
                rawResponse: "",
                result: "暂无需要处理的邮件。",
                latencyMs,
                latency_ms: latencyMs,
            };
        }

        const modelId = dto.modelId || process.env.MAIL_ASSISTANT_MODEL_ID;

        if (!modelId) {
            throw new Error("缺少用于邮件摘要的模型ID，请配置 MAIL_ASSISTANT_MODEL_ID 或在请求参数中传入 modelId。");
        }

        const model = await this.aiModelService.findOne({
            where: { id: modelId },
            relations: ["provider"],
        });

        if (!model || !model.provider) {
            throw new Error("未找到可用的AI模型或供应商配置。");
        }

        if (!model.provider.bindKeyConfigId) {
            throw new Error("所选模型未绑定有效的密钥配置，无法调用 Fastbuild AI 提供商。");
        }

        const providerKeyConfig = await this.keyConfigService.getConfigKeyValuePairs(
            model.provider.bindKeyConfigId,
        );

        const provider = getProvider(model.provider.provider, {
            apiKey: getProviderKeyConfig("apiKey", providerKeyConfig),
            baseURL: getProviderKeyConfig("baseUrl", providerKeyConfig),
        });

        const generator = new TextGenerator(provider);

        const promptPayload = this.buildPromptPayload(folderMessages);
        const emphasisLine =
            inlineMode && dto.mode === "actions"
                ? "当前模式: 行动项提取。请优先输出 actionItems 与 urgentMessages，仍需提供概览性总结。"
                : "请同时提供总览摘要、行动项、紧急邮件和跟进提醒。";

        const systemPrompt =
            "You are an enterprise email triage assistant embedded in Fastbuild AI. " +
            "Summaries must help executives identify priorities quickly. " +
            "Do not fabricate details and never retain input data.";

        const userPrompt = [
            "按照以下要求总结邮件：",
            "1. 提供一个总览性摘要，突出紧急和高价值事项；",
            "2. 针对每个文件夹返回重点摘要、待办行动和需要提醒的邮件ID；",
            "3. 给出全局的行动建议与跟进提醒；",
            "4. 仅返回JSON，不要包含多余文本，属性为 globalSummary, folderSummaries, suggestedActions, followUpReminders；",
            "5. folderSummaries 为数组，每项包含 folderId, folderName, summary, actionItems, urgentMessages（邮件ID列表）。",
            emphasisLine,
            "",
            `邮件数据：${JSON.stringify(promptPayload)}`,
        ].join("\n");

        const response = await generator.chat.create({
            model: model.model,
            messages: [
                { role: "system", content: systemPrompt },
                { role: "user", content: userPrompt },
            ],
            temperature: model.modelConfig?.temperature?.value ?? 0.3,
            max_tokens: model.modelConfig?.maxTokens?.value ?? 1024,
        });

        const jsonText = response.choices?.[0]?.message?.content ?? "";

        const parsed = this.safeParseSummary(jsonText);
        const resultText = this.composePlainResult(parsed);
        const latencyMs = Date.now() - startTime;

        return {
            metadata: {
                totalMessages: flattened.length,
                folders: folderMessages.map((folder) => ({
                    id: folder.folderId,
                    name: folder.folderName,
                    messageCount: folder.items.length,
                })),
                processedMessageIds: flattened.map((item) => item.id),
            },
            summary: {
                globalSummary: parsed.globalSummary || "",
                folderSummaries: parsed.folderSummaries || [],
                suggestedActions: parsed.suggestedActions || [],
                followUpReminders: parsed.followUpReminders || [],
            },
            rawResponse: jsonText,
            result: resultText,
            latencyMs,
            latency_ms: latencyMs,
        };
    }

    private buildPromptPayload(folders: FolderMessages[]) {
        return folders.map((folder) => ({
            folderId: folder.folderId,
            folderName: folder.folderName,
            messages: folder.items.map((item: NormalizedMailItem) => ({
                id: item.id,
                subject: item.subject,
                from: item.from,
                receivedAt: item.receivedAt,
                preview: item.preview,
                importance: item.importance,
                categories: item.categories,
                hasAttachments: item.hasAttachments,
            })),
        }));
    }

    private buildInlineFolder(dto: MailSummaryRequestDto): FolderMessages {
        const subject = (dto.subject || "").trim() || "当前邮件";
        const rawText = (dto.text || "").trim();
        const truncated = rawText.length > 6000 ? `${rawText.slice(0, 6000)}…` : rawText;

        return {
            folderId: "inline",
            folderName: dto.mode === "actions" ? "行动项提取" : "当前邮件",
            items: [
                {
                    id: "inline-message",
                    subject,
                    from: "当前用户",
                    receivedAt: new Date().toISOString(),
                    preview: truncated,
                    importance: "normal",
                    categories: [],
                    hasAttachments: false,
                },
            ],
        };
    }

    private safeParseSummary(raw: string) {
        if (!raw) {
            return {
                globalSummary: "",
                folderSummaries: [],
                suggestedActions: [],
                followUpReminders: [],
            };
        }

        const cleaned = raw
            .trim()
            .replace(/^```json/i, "")
            .replace(/```$/i, "")
            .trim();

        try {
            const parsed = JSON.parse(cleaned);
            return {
                globalSummary: parsed.globalSummary ?? "",
                folderSummaries: Array.isArray(parsed.folderSummaries)
                    ? parsed.folderSummaries
                    : [],
                suggestedActions: Array.isArray(parsed.suggestedActions)
                    ? parsed.suggestedActions
                    : [],
                followUpReminders: Array.isArray(parsed.followUpReminders)
                    ? parsed.followUpReminders
                    : [],
            };
        } catch (error) {
            this.logger.warn(`Failed to parse AI summary response: ${error.message}`);
            return {
                globalSummary: cleaned,
                folderSummaries: [],
                suggestedActions: [],
                followUpReminders: [],
            };
        }
    }

    private composePlainResult(parsed: {
        globalSummary?: string;
        folderSummaries?: Array<{
            folderId?: string;
            folderName?: string;
            summary?: string;
            actionItems?: string[];
            urgentMessages?: string[];
        }>;
        suggestedActions?: string[];
        followUpReminders?: string[];
    }): string {
        const sections: string[] = [];

        if (parsed.globalSummary) {
            sections.push(parsed.globalSummary);
        }

        if (parsed.folderSummaries && parsed.folderSummaries.length > 0) {
            const folderLines = parsed.folderSummaries
                .map((folder) => {
                    const details: string[] = [];
                    if (folder.summary) {
                        details.push(folder.summary);
                    }
                    if (folder.actionItems && folder.actionItems.length > 0) {
                        details.push(`行动项: ${folder.actionItems.join("；")}`);
                    }
                    if (folder.urgentMessages && folder.urgentMessages.length > 0) {
                        details.push(`紧急邮件: ${folder.urgentMessages.join(", ")}`);
                    }
                    if (details.length === 0) {
                        return null;
                    }
                    return `${folder.folderName || folder.folderId || "文件夹"}：${details.join("；")}`;
                })
                .filter((line): line is string => Boolean(line));
            if (folderLines.length > 0) {
                sections.push(folderLines.join("\n"));
            }
        }

        if (parsed.suggestedActions && parsed.suggestedActions.length > 0) {
            sections.push(`建议：${parsed.suggestedActions.join("；")}`);
        }

        if (parsed.followUpReminders && parsed.followUpReminders.length > 0) {
            sections.push(`提醒：${parsed.followUpReminders.join("；")}`);
        }

        return sections.join("\n\n").trim();
    }
}
