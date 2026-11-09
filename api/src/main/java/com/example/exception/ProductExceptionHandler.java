package com.example.exception;

import com.example.controller.*;
import com.example.pojo.api.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/** 商品及订单接口使用明确 HTTP 状态，其他模块保留现有响应约定。 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {ProductController.class, GuestProductController.class, ProductImageUploadController.class, OrderController.class})
public class ProductExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> business(BusinessException e) {
        int code = e.getCode() == null ? 400 : e.getCode();
        return ResponseEntity.status(code >= 400 && code <= 599 ? code : 400).body(Result.fail(code, e.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> invalid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().stream().map(err -> err.getDefaultMessage()).sorted().findFirst().orElse("商品参数无效");
        return ResponseEntity.badRequest().body(Result.fail(400, message));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, MissingServletRequestPartException.class})
    public ResponseEntity<Result<Void>> malformed(Exception e) {
        return ResponseEntity.badRequest().body(Result.fail(400, "请求内容无效，请检查数字、商品编号和图片文件"));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> tooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(413).body(Result.fail(413, "图片过大，单张商品图片最多2MB"));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> unexpected(Exception e) {
        log.error("商品模块处理失败", e);
        return ResponseEntity.internalServerError().body(Result.fail(500, "服务暂不可用，请稍后重试"));
    }
}
