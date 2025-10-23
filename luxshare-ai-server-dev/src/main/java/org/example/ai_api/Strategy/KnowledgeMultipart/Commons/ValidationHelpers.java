package org.example.ai_api.Strategy.KnowledgeMultipart.Commons;

import org.example.ai_api.Bean.WebRequest.MultipartCompletedPart;
import org.example.ai_api.Exception.BadRequestException;

import java.util.List;

/**
 * 通用校验辅助工具：聚合常见的非空与分片校验
 */
public final class ValidationHelpers {

    private ValidationHelpers() {}

    /**
     * 校验字符串非空，否则抛出 BadRequestException（消息格式："<label> 不能为空"）
     */
    public static void requireNonEmpty(String value, String label) {
        if (value == null || value.isEmpty()) {
            throw new BadRequestException(label + " 不能为空");
        }
    }

    /**
     * 校验分片列表非空
     */
    public static void requirePartsNotEmpty(List<MultipartCompletedPart> parts) {
        if (parts == null || parts.isEmpty()) {
            throw new BadRequestException("分片信息 不能为空");
        }
    }
}

