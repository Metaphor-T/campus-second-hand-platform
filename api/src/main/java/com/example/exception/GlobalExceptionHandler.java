package com.example.exception;

import com.example.pojo.api.Result;
import com.example.pojo.api.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 **/
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 业务异常处理
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常
     */
    @ExceptionHandler(ValidationException.class)
    public Result<Void> handleValidationException(ValidationException e) {
        log.warn("参数校验异常：{}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 兜底异常 - 未知错误
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常：", e);
        return Result.fail(ResultCode.ERROR.getCode(), "系统繁忙，请稍后再试");
    }


    /**
     * Spring Boot 参数校验异常（@Valid 校验失败）
     * 这里会自动捕获实体上校验失败的异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        // 获取第一个校验失败的提示
        String message = e.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .findFirst()
                .orElse("参数校验失败");

        log.warn("参数校验失败：{}", message);
        return Result.fail(400, message);
    }

}
