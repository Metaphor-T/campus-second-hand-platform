package com.example.exception;

import com.example.pojo.api.ResultCode;
import lombok.Getter;

/**
 * 参数校验异常
 **/
@Getter
public class ValidationException extends RuntimeException {

    private Integer code = ResultCode.BAD_REQUEST.getCode();

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(Integer code, String message) {
        super(message);
        this.code = code;
    }

}
