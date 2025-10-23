package org.example.ai_api.Exception;

import lombok.Getter;

@Getter
public abstract class ApiException extends RuntimeException {
    int code;

    public ApiException(int code, String message) {
        super(message);
        this.code = code;
    }

}
