package com.example.exception;

import com.example.controller.StatisticsController;
import com.example.pojo.api.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes=StatisticsController.class)
public class StatisticsExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> business(BusinessException e) {
        int code=e.getCode()==null?400:e.getCode();
        return ResponseEntity.status(code>=400&&code<=599?code:400).body(Result.fail(code,e.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> unexpected(Exception e) {
        log.error("仪表盘查询失败",e);
        return ResponseEntity.internalServerError().body(Result.fail(500,"统计数据暂时无法加载，请稍后重试"));
    }
}
