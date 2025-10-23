/**
 * Supported external knowledge base providers.
 */
export type ExternalKnowledgeProvider = "weknora";

/**
 * External dataset configuration shared across providers.
 */
export interface ExternalDatasetConfigBase {
    /** External provider identifier */
    provider: ExternalKnowledgeProvider;
    /** Optional sync status for bookkeeping */
    syncStatus?: "pending" | "synced" | "failed";
    /** Optional ISO timestamp of the latest successful sync */
    lastSyncedAt?: string;
    /** Opaque metadata for provider-specific context */
    metadata?: Record<string, unknown>;
}

/**
 * Configuration for datasets backed by WeKnora.
 */
export interface WeKnoraDatasetConfig extends ExternalDatasetConfigBase {
    provider: "weknora";
    /** WeKnora knowledge base identifier */
    knowledgeBaseId?: string;
    /** Optional session identifier to reuse conversational context */
    sessionId?: string;
}

/**
 * Union of supported external dataset configurations.
 */
export type ExternalDatasetConfig = WeKnoraDatasetConfig;
