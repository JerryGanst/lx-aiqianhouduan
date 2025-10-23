import { Injectable, Logger } from "@nestjs/common";
import { ConfigService } from "@nestjs/config";
import axios, { AxiosInstance } from "axios";
import FormData from "form-data";
import { createReadStream } from "fs";
import { basename } from "path";

import { ExternalDatasetConfig } from "../interfaces/external-config.interface";
import { RetrievalConfig } from "../interfaces/retrieval-config.interface";

interface WeKnoraHybridSearchPayload {
    query_text: string;
    vector_threshold?: number;
    keyword_threshold?: number;
    match_count?: number;
}

interface WeKnoraHybridSearchResult {
    id: string;
    content: string;
    knowledge_id: string;
    chunk_index: number;
    knowledge_title?: string;
    knowledge_filename?: string;
    score: number;
    match_type?: string;
    metadata?: Record<string, string>;
    sub_chunk_id?: string[];
    chunk_type?: string;
    parent_chunk_id?: string;
    image_info?: string;
    knowledge_source?: string;
}

interface WeKnoraHybridSearchResponse {
    success: boolean;
    data?: WeKnoraHybridSearchResult[];
    message?: string;
}

interface WeKnoraKnowledgeBase {
    id: string;
    name: string;
    description?: string;
}

interface CreateKnowledgeBasePayload {
    name: string;
    description?: string;
    chunking_config?: {
        chunk_size: number;
        chunk_overlap: number;
        separators: string[];
        enable_multimodal?: boolean;
    };
}

@Injectable()
export class WeknoraIntegrationService {
    private readonly logger = new Logger(WeknoraIntegrationService.name);
    private readonly enabled: boolean;
    private readonly autoBind: boolean;
    private readonly baseUrl?: string;
    private readonly apiKey?: string;
    private readonly timeout: number;
    private readonly axiosInstance?: AxiosInstance;

    constructor(private readonly configService: ConfigService) {
        this.enabled = this.configService.get<string>("WEKNORA_ENABLED", "false") === "true";
        this.autoBind =
            this.configService.get<string>("WEKNORA_AUTO_BIND_DATASETS", "false") === "true";
        this.baseUrl = this.configService.get<string>("WEKNORA_BASE_URL");
        this.apiKey = this.configService.get<string>("WEKNORA_API_KEY");
        this.timeout = Number(this.configService.get<string>("WEKNORA_TIMEOUT") || 20000);

        if (this.enabled && this.baseUrl && this.apiKey) {
            this.axiosInstance = axios.create({
                baseURL: this.baseUrl,
                timeout: this.timeout,
                headers: {
                    "X-API-Key": this.apiKey,
                },
            });
        } else if (this.enabled) {
            this.logger.warn(
                "WeKnora integration enabled but missing base URL or API key. Please set WEKNORA_BASE_URL and WEKNORA_API_KEY.",
            );
        }
    }

    /**
     * Whether the integration is fully configured and ready.
     */
    isConfigured(): boolean {
        return Boolean(this.enabled && this.axiosInstance && this.baseUrl && this.apiKey);
    }

    /**
     * Whether datasets without explicit config should automatically bind to WeKnora.
     */
    shouldAutoBind(): boolean {
        return this.isConfigured() && this.autoBind;
    }

    /**
     * Ensure a matching WeKnora knowledge base exists and return the config.
     */
    async ensureKnowledgeBase(
        datasetId: string,
        datasetName: string,
        externalConfig?: ExternalDatasetConfig,
        chunkingConfig?: CreateKnowledgeBasePayload["chunking_config"],
        description?: string,
    ): Promise<ExternalDatasetConfig | undefined> {
        if (!this.isConfigured()) {
            return externalConfig;
        }

        if (externalConfig?.provider === "weknora" && externalConfig.knowledgeBaseId) {
            return externalConfig;
        }

        try {
            const response = await this.axiosInstance!.post<{
                success: boolean;
                data: WeKnoraKnowledgeBase;
                message?: string;
            }>("/api/v1/knowledge-bases", {
                name: datasetName,
                description,
                chunking_config: chunkingConfig,
            } satisfies CreateKnowledgeBasePayload);

            if (!response.data?.success) {
                this.logger.warn(
                    `Failed to create WeKnora knowledge base for dataset ${datasetId}: ${response.data?.message}`,
                );
                return externalConfig;
            }

            const knowledgeBase = response.data.data;
            this.logger.log(
                `Created WeKnora knowledge base ${knowledgeBase.id} for dataset ${datasetId}`,
            );

            return {
                provider: "weknora",
                knowledgeBaseId: knowledgeBase.id,
                syncStatus: "pending",
                metadata: {
                    autoCreated: true,
                    datasetId,
                },
            };
        } catch (error) {
            this.logger.error(
                `Error creating WeKnora knowledge base for dataset ${datasetId}: ${error?.message}`,
                (error as Error)?.stack,
            );
            return externalConfig;
        }
    }

    /**
     * Upload a local file into the mapped WeKnora knowledge base.
     */
    async uploadKnowledgeFile(
        knowledgeBaseId: string,
        filePath: string,
        metadata?: Record<string, unknown>,
        formOptions?: { enableMultimodal?: boolean },
    ): Promise<boolean> {
        if (!this.isConfigured()) {
            return false;
        }

        try {
            const form = new FormData();
            form.append("file", createReadStream(filePath), basename(filePath));

            if (metadata) {
                form.append("metadata", JSON.stringify(metadata));
            }

            if (formOptions?.enableMultimodal !== undefined) {
                form.append("enable_multimodel", String(formOptions.enableMultimodal));
            }

            await this.axiosInstance!.post(
                `/api/v1/knowledge-bases/${knowledgeBaseId}/knowledge/file`,
                form,
                {
                    headers: {
                        ...form.getHeaders(),
                    },
                    maxContentLength: Infinity,
                    maxBodyLength: Infinity,
                },
            );

            this.logger.debug(
                `Uploaded file ${filePath} to WeKnora knowledge base ${knowledgeBaseId}`,
            );
            return true;
        } catch (error) {
            this.logger.error(
                `Failed to upload file ${filePath} to WeKnora knowledge base ${knowledgeBaseId}: ${error?.message}`,
            );
            return false;
        }
    }

    /**
     * Execute hybrid search against WeKnora and return raw results.
     */
    async hybridSearch(
        knowledgeBaseId: string,
        query: string,
        retrievalConfig: RetrievalConfig,
    ): Promise<WeKnoraHybridSearchResult[]> {
        if (!this.isConfigured() || !knowledgeBaseId) {
            return [];
        }

        const payload: WeKnoraHybridSearchPayload = {
            query_text: query,
            vector_threshold: retrievalConfig.scoreThreshold,
            keyword_threshold: retrievalConfig.scoreThreshold,
            match_count: retrievalConfig.topK ?? 3,
        };

        try {
            const response = await this.axiosInstance!.post<WeKnoraHybridSearchResponse>(
                `/api/v1/knowledge-bases/${knowledgeBaseId}/hybrid-search`,
                payload,
            );

            if (!response.data?.success) {
                this.logger.warn(
                    `WeKnora hybrid search unsuccessful for KB ${knowledgeBaseId}: ${response.data?.message}`,
                );
                return [];
            }

            return response.data.data ?? [];
        } catch (error) {
            this.logger.error(
                `WeKnora hybrid search failed for KB ${knowledgeBaseId}: ${error?.message}`,
                (error as Error)?.stack,
            );
            return [];
        }
    }
}
