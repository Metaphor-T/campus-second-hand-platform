package com.example.pojo.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理员消息查询条件类
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AdminMessageQueryDTO extends QueryDTO {

    private String fromUsername; // 发送者用户名（模糊搜索）
    private String toUsername;   // 接收者用户名（模糊搜索）
    private String content;      // 消息内容（模糊搜索）
    private Integer messageType; // 消息类型：1文本，2表情，3图片
    private Integer status;      // 消息状态：0未读，1已读
    private String startTime;    // 开始时间（yyyy-MM-dd HH:mm:ss）
    private String endTime;      // 结束时间（yyyy-MM-dd HH:mm:ss）
}
