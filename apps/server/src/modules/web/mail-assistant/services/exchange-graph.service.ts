import { Injectable } from "@nestjs/common";
import { Client } from "@microsoft/microsoft-graph-client";

import { FolderMessages, NormalizedMailItem } from "../interfaces/mail-summary.interface";

interface FetchOptions {
    folderId: string;
    folderName: string;
    top: number;
    includeRead: boolean;
}

interface GraphMessage {
    id: string;
    subject?: string;
    receivedDateTime?: string;
    bodyPreview?: string;
    importance?: string;
    categories?: string[];
    isRead?: boolean;
    hasAttachments?: boolean;
    from?: {
        emailAddress?: {
            name?: string;
            address?: string;
        };
    };
}

@Injectable()
export class ExchangeGraphService {
    async fetchFolderMessages(
        accessToken: string,
        options: FetchOptions,
    ): Promise<FolderMessages | null> {
        const client = Client.init({
            authProvider: (done) => {
                done(null, accessToken);
            },
        });

        const { folderId, folderName, top, includeRead } = options;

        const request = client
            .api(`/me/mailFolders/${folderId}/messages`)
            .top(top)
            .select(
                [
                    "id",
                    "subject",
                    "from",
                    "receivedDateTime",
                    "bodyPreview",
                    "importance",
                    "categories",
                    "isRead",
                    "hasAttachments",
                ].join(","),
            )
            .orderby("receivedDateTime DESC");

        if (!includeRead) {
            request.filter("isRead eq false");
        }

        const response = await request.get();

        if (!response?.value || !Array.isArray(response.value)) {
            return null;
        }

        const items: NormalizedMailItem[] = response.value
            .map((message: GraphMessage) => this.normalizeMessage(message))
            .filter((item): item is NormalizedMailItem => Boolean(item));

        return {
            folderId,
            folderName,
            items,
        };
    }

    private normalizeMessage(message: GraphMessage): NormalizedMailItem | null {
        if (!message?.id) {
            return null;
        }

        const from = message.from?.emailAddress;
        const preview = (message.bodyPreview || "").replace(/\s+/g, " ").trim();

        return {
            id: message.id,
            subject: (message.subject || "").trim(),
            from: [from?.name, from?.address].filter(Boolean).join(" ") || "Unknown sender",
            receivedAt: message.receivedDateTime || "",
            preview: preview.length > 500 ? `${preview.slice(0, 500)}…` : preview,
            importance: (message.importance || "normal").toLowerCase(),
            categories: message.categories || [],
            hasAttachments: Boolean(message.hasAttachments),
        };
    }
}
