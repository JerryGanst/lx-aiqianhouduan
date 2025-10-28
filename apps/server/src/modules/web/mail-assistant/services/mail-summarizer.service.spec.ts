import { MailSummarizerService } from "./mail-summarizer.service";

const mockChatCreate = jest.fn();

jest.mock("@sdk/ai", () => ({
    getProvider: jest.fn(() => ({})),
    TextGenerator: jest.fn().mockImplementation(() => ({
        chat: {
            create: mockChatCreate,
        },
    })),
}));

jest.mock("@/modules/console/ai/services/ai-model.service", () => ({
    AiModelService: jest.fn(),
}));

jest.mock("@/modules/console/key-manager/services/key-config.service", () => ({
    KeyConfigService: jest.fn(),
}));

jest.mock("@/common/utils/helper.util", () => ({
    getProviderKeyConfig: (key: string, config: Record<string, { value: string }>) =>
        config[key]?.value ?? "",
}));

describe("MailSummarizerService", () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockChatCreate.mockReset();
        process.env.MAIL_ASSISTANT_MODEL_ID = "default-model";
    });

    afterAll(() => {
        delete process.env.MAIL_ASSISTANT_MODEL_ID;
    });

    const buildService = () => {
        const aiModelService = {
            findOne: jest.fn().mockResolvedValue({
                id: "default-model",
                model: "fastbuild-chat",
                modelConfig: {
                    temperature: { value: 0.2, enable: true },
                    maxTokens: { value: 800, enable: true },
                },
                provider: {
                    provider: "fastbuild",
                    bindKeyConfigId: "config-1",
                },
            }),
        };

        const keyConfigService = {
            getConfigKeyValuePairs: jest.fn().mockResolvedValue({
                apiKey: { value: "api-key", required: true },
                baseUrl: { value: "https://fastbuild.ai/api", required: false },
            }),
        };

        const exchangeGraphService = {
            fetchFolderMessages: jest.fn(),
        };

        return {
            service: new MailSummarizerService(
                aiModelService as any,
                keyConfigService as any,
                exchangeGraphService as any,
            ),
            aiModelService,
            keyConfigService,
            exchangeGraphService,
        };
    };

    it("returns structured summary when AI response is valid JSON", async () => {
        const { service, exchangeGraphService } = buildService();

        exchangeGraphService.fetchFolderMessages.mockResolvedValue({
            folderId: "inbox",
            folderName: "Inbox",
            items: [
                {
                    id: "mail-1",
                    subject: "Quarterly Report",
                    from: "Finance <finance@example.com>",
                    receivedAt: "2024-04-01T08:00:00Z",
                    preview: "Please review the attached quarterly numbers...",
                    importance: "high",
                    categories: ["Finance"],
                    hasAttachments: true,
                },
            ],
        });

        mockChatCreate.mockResolvedValue({
            choices: [
                {
                    message: {
                        content: JSON.stringify({
                            globalSummary: "关键事项：季度财报待审核。",
                            folderSummaries: [
                                {
                                    folderId: "inbox",
                                    folderName: "Inbox",
                                    summary: "财务部发送季度财报，需要尽快确认。",
                                    actionItems: ["安排财务会议"],
                                    urgentMessages: ["mail-1"],
                                },
                            ],
                            suggestedActions: ["与财务总监确认时间表"],
                            followUpReminders: ["mail-1"],
                        }),
                    },
                },
            ],
        });

        const result = await service.summarize({
            accessToken: "token",
        });

        expect(result.metadata.totalMessages).toBe(1);
        expect(result.metadata.folders[0]).toMatchObject({
            id: "inbox",
            messageCount: 1,
        });

        expect(result.summary.globalSummary).toContain("季度财报");
        expect(result.summary.folderSummaries[0].urgentMessages).toEqual(["mail-1"]);
        expect(result.rawResponse).toContain("季度财报待审核");
        expect(result.result).toContain("季度财报");
        expect(result.latencyMs).toBeGreaterThanOrEqual(0);
        expect(result.latency_ms).toBe(result.latencyMs);
    });

    it("falls back to plain text when AI response is not JSON", async () => {
        const { service, exchangeGraphService } = buildService();

        exchangeGraphService.fetchFolderMessages.mockResolvedValue({
            folderId: "inbox",
            folderName: "Inbox",
            items: [
                {
                    id: "mail-99",
                    subject: "Travel Itinerary",
                    from: "Assistant <assistant@example.com>",
                    receivedAt: "2024-04-02T08:00:00Z",
                    preview: "Upcoming trip schedule...",
                    importance: "normal",
                    categories: [],
                    hasAttachments: false,
                },
            ],
        });

        mockChatCreate.mockResolvedValue({
            choices: [
                {
                    message: {
                        content: "Reminder: Confirm travel with assistant.",
                    },
                },
            ],
        });

        const result = await service.summarize({
            accessToken: "token",
        });

        expect(result.summary.globalSummary).toBe("Reminder: Confirm travel with assistant.");
        expect(result.summary.folderSummaries).toEqual([]);
        expect(result.summary.suggestedActions).toEqual([]);
        expect(result.summary.followUpReminders).toEqual([]);
        expect(result.result).toBe("Reminder: Confirm travel with assistant.");
    });

    it("supports inline text summarization without access token", async () => {
        const { service, exchangeGraphService } = buildService();

        exchangeGraphService.fetchFolderMessages.mockResolvedValue(null);

        mockChatCreate.mockResolvedValue({
            choices: [
                {
                    message: {
                        content: JSON.stringify({
                            globalSummary: "整理近期出差事项并确认议程。",
                            folderSummaries: [
                                {
                                    folderId: "inline",
                                    folderName: "当前邮件",
                                    summary: "邮件包含行程安排，请确认航班与酒店。",
                                    actionItems: ["确认航班时间", "回复秘书确认酒店"],
                                    urgentMessages: [],
                                },
                            ],
                            suggestedActions: ["同步最新行程给行政"],
                            followUpReminders: ["与行政沟通航班变更"],
                        }),
                    },
                },
            ],
        });

        const result = await service.summarize({
            subject: "【出差】上海行程确认",
            text: "本周需要确认上海出差的航班与酒店，请尽快回复。",
            mode: "actions",
        });

        expect(result.metadata.totalMessages).toBe(1);
        expect(result.summary.globalSummary).toContain("整理近期出差事项");
        expect(result.result).toContain("行程安排");
        expect(result.summary.suggestedActions).toHaveLength(1);
        expect(result.latencyMs).toBeGreaterThanOrEqual(0);
    });
});
