package org.example.ai_api;

import org.example.ai_api.Bean.Model.ResultData;
import org.example.ai_api.Exception.BaseException;
import org.example.ai_api.Exception.RateLimitException;
import org.example.ai_api.Exception.StreamApiException;
import org.example.ai_api.Exception.SyncApiException;
import org.example.ai_api.Utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.PrematureCloseException;
import org.apache.tika.exception.WriteLimitReachedException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;


/**
 * 全局异常处理.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class.getName());
    @Value("${base_package}")
    private String BASE_PACKAGE;

    /**
     * 处理自定义限流注解报错.
     *
     * @param ex      自定义异常
     * @param request 请求体
     * @return 根据请求类型返回不同数据
     */
    @ExceptionHandler(RateLimitException.class)
    public Object handleRateLimit(RateLimitException ex, WebRequest request) {
        // 创建统一错误响应结构
        ResultData<Void> errorResult = new ResultData<>(
                429,
                false,
                ex.getMessage(),
                null
        );
        // 判断请求类型
        if (Utils.isStreamRequest(request)) {
            // 流式响应：将错误包装成SSE事件
            return Flux.just(
                    ServerSentEvent.<ResultData<Void>>builder()
                            .event("error")
                            .data(errorResult)
                            .build()
            );
        } else {
            // 普通HTTP响应
            return ResponseEntity
                    .status(429)
                    .body(errorResult);
        }
    }

    /**
     * 处理文件上传超出大小限制的异常.
     *
     * @param e 文件上传超出大小限制异常
     * @return 包含错误信息的统一响应对象
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResultData<String> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e,WebRequest request) {
        String errorDetail = getErrorDetail(e, request);
        return ResultData.fail(413, "上传文件大小单个不能超过50MB");
    }

    /**
     * 处理请求方式不支持的异常.
     *
     * @param e 请求方式不支持异常
     * @return 包含错误信息的统一响应对象
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResultData<String> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e, WebRequest request) {
        String errorDetail = getErrorDetail(e, request);
        String requestUri = request.getDescription(false).replace("uri=", "");
        return ResultData.fail(405, String.format("请求方式不支持,请求路径:[%s]",requestUri), errorDetail);
    }

    /**
     * 处理Tika字符写入限制异常.
     *
     * @param e Tika字符写入限制异常
     * @return 包含错误信息的统一响应对象
     */
    @ExceptionHandler(WriteLimitReachedException.class)
    public ResultData<String> handleWriteLimitReachedException(WriteLimitReachedException e,WebRequest request) {
        String errorDetail = getErrorDetail(e,request);
        return ResultData.fail(400, "文件内容过长，已超出系统处理限制，请尝试缩短内容", errorDetail);
    }

    /**
     * 处理文件名验证异常.
     *
     * @param e 文件名验证异常
     * @return 包含错误信息的统一响应对象
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResultData<String> handleIllegalArgumentException(IllegalArgumentException e,WebRequest request) {
        String errorDetail = getErrorDetail(e, request);
        String message = e.getMessage();
        return ResultData.fail(400, "参数错误: " + message, errorDetail);
    }

    /**
     * 处理非流式接口报错的异常.
     *
     * @param ex 非流式抛出异常
     * @return 包含错误信息的统一响应对象
     */
    @ExceptionHandler(SyncApiException.class)
    public ResultData<String> handleSyncApiException(SyncApiException ex) {
        String errorDetail = getErrorDetail(ex, null);
        String message = ex.getMessage();
        if(ex.getCode() == 400){
            message = "文本过长，请重新尝试";
        }
        return ResultData.fail(ex.getCode(), message,errorDetail);
    }

    /**
     * 处理流式接口报错的异常.
     *
     * @param ex 流式抛出异常
     * @return 包含错误信息的统一响应对象
     */
    @ExceptionHandler(StreamApiException.class)
    public ResultData<String> handleStreamApiException(StreamApiException ex) {
        String errorDetail = getErrorDetail(ex, null);
        String message = ex.getMessage();
        return ResultData.fail(ex.getCode(),message,errorDetail);
    }

    /**
     * 处理自定义异常.
     *
     * @param ex 自定义异常
     * @param request 请求体
     * @return 根据请求类型返回不同数据
     */
    @ExceptionHandler(BaseException.class) // 捕获所有继承BaseException的异常
    public Object handleCustomException(BaseException ex, WebRequest request) {
        String errorDetail = getErrorDetail(ex, request);
        ResultData<String> errorResult = new ResultData<>(
                ex.getErrorCode().getCode(),
                false,
                ex.getMessage(),
                errorDetail
        );

        // 判断请求类型
        if (Utils.isStreamRequest(request)) {
            // 流式响应：将错误包装成SSE事件
            return Flux.just(
                    ServerSentEvent.<ResultData<String>>builder()
                            .event("error")
                            .data(errorResult)
                            .build()
            );
        } else {
            // 普通HTTP响应
            return ResponseEntity
                    .status(ex.getErrorCode().getCode())
                    .body(errorResult);
        }
    }

    /**
     * 处理其他所有异常.
     *
     * @param ex 其他所有异常
     * @return 包含错误信息的统一响应对象
     */
    @ExceptionHandler(Exception.class)
    public ResultData<String> handleException(Exception ex,WebRequest request) {
        String errorDetail = getErrorDetail(ex, request);
        // 返回包含详细信息的响应
        return new ResultData<>(
                500,
                false,
                "系统内部错误",
                errorDetail
        );
    }

    /**
     * 处理WebClient连接相关异常（包括连接超时、连接中断等）.
     *
     * @param ex      WebClient请求异常
     * @param request 请求体
     * @return 根据请求类型返回不同数据
     */
    @ExceptionHandler(WebClientRequestException.class)
    public Object handleWebClientRequestException(WebClientRequestException ex, WebRequest request) {
        String errorMessage = "第三方服务连接异常";
        String errorDetail = getErrorDetail(ex, request);
        
        // 根据具体异常类型提供更精确的错误信息
        Throwable cause = ex.getCause();
        if (cause instanceof ConnectException) {
            errorMessage = "无法连接到第三方服务，请检查服务状态";
        } else if (cause instanceof SocketTimeoutException) {
            errorMessage = "连接第三方服务超时，请稍后重试";
        } else if (cause instanceof TimeoutException) {
            errorMessage = "请求第三方服务超时，请稍后重试";
        } else if (cause instanceof PrematureCloseException) {
            errorMessage = "与第三方服务的连接意外中断，请稍后重试";
        } else if (ex.getMessage() != null && ex.getMessage().contains("Connection refused")) {
            errorMessage = "无法连接到第三方服务，请检查服务状态";
        } else if (ex.getMessage() != null && ex.getMessage().contains("timeout")) {
            errorMessage = "请求第三方服务超时，请稍后重试";
        }
        
        // 创建统一错误响应结构
        ResultData<String> errorResult = new ResultData<>(
                503,
                false,
                errorMessage,
                errorDetail
        );
        
        // 判断请求类型
        if (Utils.isStreamRequest(request)) {
            // 流式响应：将错误包装成SSE事件
            return Flux.just(
                    ServerSentEvent.<ResultData<String>>builder()
                            .event("error")
                            .data(errorResult)
                            .build()
            );
        } else {
            // 普通HTTP响应
            return ResponseEntity
                    .status(503)
                    .body(errorResult);
        }
    }

    /**
     * 处理连接异常.
     *
     * @param ex      连接异常
     * @param request 请求体
     * @return 根据请求类型返回不同数据
     */
    @ExceptionHandler(ConnectException.class)
    public Object handleConnectException(ConnectException ex, WebRequest request) {
        String errorDetail = getErrorDetail(ex, request);
        ResultData<String> errorResult = new ResultData<>(
                503,
                false,
                "无法连接到第三方服务，请检查服务状态",
                errorDetail
        );
        
        if (Utils.isStreamRequest(request)) {
            return Flux.just(
                    ServerSentEvent.<ResultData<String>>builder()
                            .event("error")
                            .data(errorResult)
                            .build()
            );
        } else {
            return ResponseEntity
                    .status(503)
                    .body(errorResult);
        }
    }

    /**
     * 处理Socket超时异常.
     *
     * @param ex      Socket超时异常
     * @param request 请求体
     * @return 根据请求类型返回不同数据
     */
    @ExceptionHandler(SocketTimeoutException.class)
    public Object handleSocketTimeoutException(SocketTimeoutException ex, WebRequest request) {
        String errorDetail = getErrorDetail(ex, request);
        ResultData<String> errorResult = new ResultData<>(
                504,
                false,
                "连接第三方服务超时，请稍后重试",
                errorDetail
        );
        
        if (Utils.isStreamRequest(request)) {
            return Flux.just(
                    ServerSentEvent.<ResultData<String>>builder()
                            .event("error")
                            .data(errorResult)
                            .build()
            );
        } else {
            return ResponseEntity
                    .status(504)
                    .body(errorResult);
        }
    }

    /**
     * 处理超时异常.
     *
     * @param ex      超时异常
     * @param request 请求体
     * @return 根据请求类型返回不同数据
     */
    @ExceptionHandler(TimeoutException.class)
    public Object handleTimeoutException(TimeoutException ex, WebRequest request) {
        String errorDetail = getErrorDetail(ex, request);
        ResultData<String> errorResult = new ResultData<>(
                504,
                false,
                "请求第三方服务超时，请稍后重试",
                errorDetail
        );
        
        if (Utils.isStreamRequest(request)) {
            return Flux.just(
                    ServerSentEvent.<ResultData<String>>builder()
                            .event("error")
                            .data(errorResult)
                            .build()
            );
        } else {
            return ResponseEntity
                    .status(504)
                    .body(errorResult);
        }
    }

    /**
     * 处理连接意外中断异常.
     *
     * @param ex      连接意外中断异常
     * @param request 请求体
     * @return 根据请求类型返回不同数据
     */
    @ExceptionHandler(PrematureCloseException.class)
    public Object handlePrematureCloseException(PrematureCloseException ex, WebRequest request) {
        String errorDetail = getErrorDetail(ex, request);
        ResultData<String> errorResult = new ResultData<>(
                503,
                false,
                "与第三方服务的连接意外中断，请稍后重试",
                errorDetail
        );
        
        if (Utils.isStreamRequest(request)) {
            return Flux.just(
                    ServerSentEvent.<ResultData<String>>builder()
                            .event("error")
                            .data(errorResult)
                            .build()
            );
        } else {
            return ResponseEntity
                    .status(503)
                    .body(errorResult);
        }
    }

    /**
     * 获取异常的详细信息.
     * @param ex 异常对象
     * @return 异常的详细信息
     */
    private String getErrorDetail(Exception ex, WebRequest request){
        // 获取完整的堆栈跟踪信息
        StackTraceElement[] stackTrace = ex.getStackTrace();
        // 寻找第一个属于项目代码的堆栈帧
        String errorDetail = "异常发生位置未知";
        for (StackTraceElement element : stackTrace) {
            if (BASE_PACKAGE != null && !BASE_PACKAGE.isEmpty() && element.getClassName().startsWith(BASE_PACKAGE)) {
                errorDetail = String.format("异常类型：%s,异常位置：%s.%s(%s:%d)",
                        ex.getClass().getSimpleName(),
                        element.getClassName(),
                        element.getMethodName(),
                        element.getFileName(),
                        element.getLineNumber());
                break;
            }
        }
        // Add request URI to error detail if available
        if (request != null) {
            String requestUri = request.getDescription(false).replace("uri=", "");
            errorDetail += String.format(", 请求路径:[%s]", requestUri);
        }
        // 记录完整异常日志（包含堆栈跟踪）
        logger.error("Exception: {}", errorDetail, ex);
        return errorDetail;
    }
}