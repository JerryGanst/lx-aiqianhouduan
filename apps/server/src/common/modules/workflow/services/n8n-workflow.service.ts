import { isEnabled } from "@common/utils/is.util";
import { Injectable, Logger } from "@nestjs/common";
import axios, { AxiosError, AxiosRequestConfig } from "axios";

type SupportedMethod = "get" | "post" | "put" | "patch" | "delete";

interface TriggerOptions {
    method?: SupportedMethod;
    headers?: Record<string, string>;
    timeout?: number;
    query?: Record<string, string | number | boolean | undefined | null>;
    throwOnError?: boolean;
}

export interface UserRegisteredWorkflowPayload {
    user: Record<string, unknown>;
    ipAddress?: string;
    userAgent?: string;
    terminal?: string;
    timestamp?: string;
    metadata?: Record<string, unknown>;
}

/**
 * 封装与 n8n 的交互能力
 */
@Injectable()
export class N8nWorkflowService {
    private readonly logger = new Logger(N8nWorkflowService.name);

    private readonly enabled: boolean;
    private readonly baseUrl?: string;
    private readonly webhookBaseUrl?: string;
    private readonly authHeaderKey?: string;
    private readonly authHeaderValue?: string;
    private readonly defaultTimeout: number;
    private readonly userRegisterPath?: string;

    constructor() {
        this.enabled = isEnabled(process.env.N8N_ENABLED ?? "false");
        this.baseUrl = this.normalizeUrl(process.env.N8N_BASE_URL);
        this.webhookBaseUrl = this.normalizeUrl(
            process.env.N8N_WEBHOOK_BASE_URL || (this.baseUrl ? `${this.baseUrl}/webhook` : ""),
        );
        this.authHeaderKey = this.normalizeHeaderKey(process.env.N8N_WEBHOOK_AUTH_HEADER);
        this.authHeaderValue = process.env.N8N_WEBHOOK_AUTH_TOKEN || undefined;
        this.defaultTimeout = this.parseTimeout(process.env.N8N_TIMEOUT);
        this.userRegisterPath = this.normalizePath(process.env.N8N_WORKFLOW_USER_REGISTER_PATH);
    }

    /**
     * 触发 n8n webhook
     */
    async triggerWebhook(
        path: string,
        payload: unknown,
        options: TriggerOptions = {},
    ): Promise<unknown> {
        if (!this.enabled) {
            this.logger.debug(`n8n integration disabled, skip webhook "${path}"`);
            return null;
        }

        const webhookUrl = this.buildWebhookUrl(path);
        if (!webhookUrl) {
            this.logger.warn(
                `n8n webhook base URL is not configured, skip triggering workflow "${path}"`,
            );
            return null;
        }

        const queryString = this.buildQueryString(options.query);
        const url = `${webhookUrl}${queryString}`;

        const method = options.method ?? "post";
        const config: AxiosRequestConfig = {
            method,
            url,
            data: payload,
            timeout: options.timeout ?? this.defaultTimeout,
            headers: this.buildHeaders(options.headers),
            validateStatus: () => true,
        };

        try {
            const response = await axios.request(config);
            if (response.status >= 400) {
                const message = `n8n webhook ${method.toUpperCase()} ${url} responded with status ${
                    response.status
                }`;
                const responseBody =
                    response.data && typeof response.data === "object"
                        ? JSON.stringify(response.data)
                        : response.data;
                this.logger.error(
                    `${message}${responseBody ? ` - ${responseBody}` : ""}`,
                    "N8nWebhookResponseError",
                );
                if (options.throwOnError) {
                    throw new Error(message);
                }
            } else {
                this.logger.debug(
                    `n8n webhook ${method.toUpperCase()} ${url} succeeded with status ${response.status}`,
                );
            }
            return response.data;
        } catch (error) {
            return this.handleAxiosError(error, method, url, options.throwOnError === true);
        }
    }

    /**
     * 发送用户注册事件
     */
    async emitUserRegistered(payload: UserRegisteredWorkflowPayload): Promise<void> {
        if (!this.userRegisterPath) {
            this.logger.debug(
                "n8n user registration workflow path not configured, skip event emit",
            );
            return;
        }

        const eventPayload: Record<string, unknown> = {
            event: "user.registered",
            timestamp: payload.timestamp ?? new Date().toISOString(),
            user: payload.user,
        };

        const context: Record<string, unknown> = {};
        if (payload.ipAddress) {
            context.ipAddress = payload.ipAddress;
        }
        if (payload.userAgent) {
            context.userAgent = payload.userAgent;
        }
        if (payload.terminal) {
            context.terminal = payload.terminal;
        }
        if (payload.metadata && Object.keys(payload.metadata).length > 0) {
            context.metadata = payload.metadata;
        }

        if (Object.keys(context).length > 0) {
            eventPayload.context = context;
        }

        await this.triggerWebhook(this.userRegisterPath, eventPayload, { throwOnError: false });
    }

    private buildHeaders(customHeaders?: Record<string, string>): Record<string, string> {
        const headers: Record<string, string> = {
            "Content-Type": "application/json",
        };

        if (this.authHeaderKey && this.authHeaderValue) {
            headers[this.authHeaderKey] = this.authHeaderValue;
        }

        if (customHeaders) {
            for (const [key, value] of Object.entries(customHeaders)) {
                if (key && value != null) {
                    headers[key] = value;
                }
            }
        }

        return headers;
    }

    private buildQueryString(
        query?: Record<string, string | number | boolean | undefined | null>,
    ): string {
        if (!query) {
            return "";
        }

        const params = new URLSearchParams();
        for (const [key, rawValue] of Object.entries(query)) {
            if (rawValue === undefined || rawValue === null) {
                continue;
            }
            params.append(key, String(rawValue));
        }

        const serialized = params.toString();
        return serialized ? `?${serialized}` : "";
    }

    private buildWebhookUrl(path: string): string | undefined {
        if (!this.webhookBaseUrl) {
            return undefined;
        }

        const normalizedPath = this.normalizePath(path);
        if (!normalizedPath) {
            return undefined;
        }

        return `${this.webhookBaseUrl}/${normalizedPath}`;
    }

    private normalizeUrl(value?: string | null): string | undefined {
        if (!value) {
            return undefined;
        }

        return value.replace(/\/+$/, "");
    }

    private normalizePath(path?: string | null): string | undefined {
        if (!path) {
            return undefined;
        }

        return path.replace(/^\/+/, "").trim();
    }

    private normalizeHeaderKey(key?: string | null): string | undefined {
        if (!key) {
            return undefined;
        }

        return key.trim();
    }

    private parseTimeout(value?: string | null): number {
        if (!value) {
            return 10000;
        }

        const parsed = Number.parseInt(value, 10);
        if (Number.isNaN(parsed) || parsed <= 0) {
            return 10000;
        }

        return parsed;
    }

    private handleAxiosError(
        error: unknown,
        method: SupportedMethod,
        url: string,
        throwOnError: boolean,
    ): null {
        if (axios.isAxiosError(error)) {
            const axiosError = error as AxiosError;
            const status = axiosError.response?.status;
            const statusMessage = status ? ` with status ${status}` : "";
            const detail =
                typeof axiosError.response?.data === "object"
                    ? JSON.stringify(axiosError.response.data)
                    : axiosError.response?.data || axiosError.message;

            this.logger.error(
                `n8n webhook ${method.toUpperCase()} ${url} failed${statusMessage}: ${detail}`,
                axiosError.stack,
            );

            if (throwOnError) {
                throw axiosError;
            }
        } else if (error instanceof Error) {
            this.logger.error(
                `n8n webhook ${method.toUpperCase()} ${url} failed: ${error.message}`,
                error.stack,
            );
            if (throwOnError) {
                throw error;
            }
        } else if (throwOnError) {
            throw error;
        }

        return null;
    }
}
