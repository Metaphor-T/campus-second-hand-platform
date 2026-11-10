package com.example.exception;

import com.example.controller.UserProfileController;
import com.example.pojo.api.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@Slf4j @Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes=UserProfileController.class)
public class UserProfileExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> business(BusinessException e) { int code=e.getCode()==null?400:e.getCode();return ResponseEntity.status(code>=400&&code<=599?code:400).body(Result.fail(code,e.getMessage())); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> invalid(MethodArgumentNotValidException e) { return ResponseEntity.badRequest().body(Result.fail(400,e.getBindingResult().getAllErrors().getFirst().getDefaultMessage())); }
    @ExceptionHandler({HttpMessageNotReadableException.class,MissingServletRequestPartException.class})
    public ResponseEntity<Result<Void>> malformed(Exception e) { return ResponseEntity.badRequest().body(Result.fail(400,"资料格式无效，请检查输入和头像文件")); }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> tooLarge(Exception e) { return ResponseEntity.status(413).body(Result.fail(413,"头像不能超过2MB")); }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> unexpected(Exception e) { log.error("个人中心处理失败",e);return ResponseEntity.internalServerError().body(Result.fail(500,"个人资料暂时无法处理，请稍后重试")); }
}
