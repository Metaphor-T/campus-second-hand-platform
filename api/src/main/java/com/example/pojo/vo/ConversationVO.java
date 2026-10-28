package com.example.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话列表视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationVO {

    private Integer userId; // 对话方base_user_id

    private String username; // 对话方用户名

    private String avatar; // 对话方头像

    private String lastContent; // 最后一条消息内容

    private Integer lastMessageType; // 最后一条消息类型

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastTime; // 最后消息时间

    private Integer unreadCount; // 未读消息数

    private Boolean online; // 对话方是否在线（基于 WebSocket 连接）
}
