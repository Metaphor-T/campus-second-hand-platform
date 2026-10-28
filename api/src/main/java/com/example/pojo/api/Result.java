package com.example.pojo.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 后端通用响应类 - 泛型
 **/
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    private Integer code; // 响应码
    private String message; // 响应消息
    private T data; // 数据
    private Integer count; // 数据量
    private Long timestamp; // 时间戳

    // ========== 私有构造方法 ==========
    private Result() {
        this.timestamp = System.currentTimeMillis();
    }

    private Result(Integer code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    private Result(Integer code, String message, T data, Integer count) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.count = count;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 失败 - 使用枚举预定义
     */
    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage());
    }

    /**
     * 失败 - 自定义消息（默认500）
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message);
    }

    /**
     * 失败 - 自定义状态码和消息
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message);
    }

    /**
     * 失败 - 使用枚举，自定义消息覆盖
     */
    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message);
    }

    /**
     * 成功 - 无数据
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage());
    }

    /**
     * 成功 - 有数据
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功 - 自定义消息，有数据
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 成功 - 列表分页场景（带数据量）
     */
    public static <T> Result<T> success(T data, Integer count) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data, count);
    }

    /**
     * 成功 - 列表分页场景（自定义消息）
     */
    public static <T> Result<T> success(String message, T data, Integer count) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data, count);
    }

}
