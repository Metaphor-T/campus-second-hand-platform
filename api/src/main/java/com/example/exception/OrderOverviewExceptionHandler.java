package com.example.exception;

import com.example.controller.OrderOverviewController;
import com.example.pojo.api.Result;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Order(Ordered.HIGHEST_PRECEDENCE) @RestControllerAdvice(assignableTypes=OrderOverviewController.class)
public class OrderOverviewExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> business(BusinessException e){int code=e.getCode()==null?400:e.getCode();return ResponseEntity.status(code>=400&&code<=599?code:400).body(Result.fail(code,e.getMessage()));}
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> invalid(Exception e){return ResponseEntity.badRequest().body(Result.fail(400,"订单编号无效"));}
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> failure(Exception e){log.error("订单详情读取失败",e);return ResponseEntity.internalServerError().body(Result.fail(500,"订单详情暂时无法加载，请稍后重试"));}
}
