package com.example.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 消息传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    /** 消息类型：MESSAGE-聊天消息，HEARTBEAT-心跳 */
    private String type;

    /** 消息子类型：1文本，2表情，3图片 */
    private Integer messageType;

    /** 消息内容 */
    private String content;

    /** 接收者base_user_id */
    private Integer toUserId;

    /** 发送者base_user_id（服务端填充） */
    private Integer fromUserId;

    /** 消息时间戳（服务端填充） */
    private Long timestamp;

    /** 消息ID（服务端返回时填充） */
    private Long messageId;

    /** 消息状态（服务端返回时填充） */
    private Integer status;

    /** 扩展数据（服务端返回时填充） */
    private Object data;
}
