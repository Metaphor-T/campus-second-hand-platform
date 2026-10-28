package com.example.exception;

import com.example.pojo.api.ResultCode;
import lombok.Getter;

/**
 * 业务异常处理
 **/
@Getter
public class BusinessException extends RuntimeException {

    private Integer code; // 响应码

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.ERROR.getCode();
    }

    public BusinessException(String message,Integer code) {
        super(message);
        this.code = code;
    }

}
