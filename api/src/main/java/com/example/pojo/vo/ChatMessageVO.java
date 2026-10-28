package com.example.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天消息视图对象（含发送方信息）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageVO {

    private Long id; // 消息ID

    private Integer fromUserId; // 发送者base_user_id

    private String fromUsername; // 发送者用户名

    private String fromAvatar; // 发送者头像

    private Integer toUserId; // 接收者base_user_id

    private String toUsername; // 接收者用户名

    private String toAvatar; // 接收者头像

    private String content; // 消息内容

    private Integer messageType; // 消息类型：1文本，2表情，3图片

    private Integer status; // 消息状态：0未读，1已读

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime; // 发送时间
}
