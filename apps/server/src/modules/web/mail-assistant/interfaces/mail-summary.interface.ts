export interface NormalizedMailItem {
    id: string;
    subject: string;
    from: string;
    receivedAt: string;
    preview: string;
    importance: string;
    categories: string[];
    hasAttachments: boolean;
}

export interface FolderMessages {
    folderId: string;
    folderName: string;
    items: NormalizedMailItem[];
}

export interface MailSummaryResult {
    metadata: {
        totalMessages: number;
        folders: Array<{
            id: string;
            name: string;
            messageCount: number;
        }>;
        processedMessageIds: string[];
    };
    summary: {
        globalSummary: string;
        folderSummaries: Array<{
            folderId: string;
            folderName: string;
            summary: string;
            actionItems: string[];
            urgentMessages: string[];
        }>;
        suggestedActions: string[];
        followUpReminders: string[];
    };
    rawResponse: string;
    result?: string;
    latencyMs?: number;
    latency_ms?: number;
}
