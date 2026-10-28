package com.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天消息实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id; // 消息主键ID

    private Integer fromUserId; // 发送者base_user_id

    private Integer toUserId; // 接收者base_user_id

    private String content; // 消息内容

    private Integer messageType; // 消息类型：1文本，2表情，3图片

    private Integer status; // 消息状态：0未读，1已读

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime; // 发送时间
}
