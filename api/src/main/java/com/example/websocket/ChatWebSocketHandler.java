package com.example.websocket;

import com.example.pojo.entity.ChatMessage;
import com.example.service.ChatMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;

/**
 * 聊天 WebSocket 处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionManager sessionManager;
    private final ChatMessageService chatMessageService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从会话属性中获取 userId
        Integer userId = (Integer) session.getAttributes().get("userId");
        if (userId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        sessionManager.addSession(userId, session);

        // 连接建立后立即推送当前总未读消息数
        pushUnreadCount(userId);

        // 广播用户上线状态给所有其他在线用户
        broadcastUserStatus(userId, true);

        log.info("WebSocket 连接建立: userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Integer fromUserId = (Integer) session.getAttributes().get("userId");
        if (fromUserId == null) {
            return;
        }

        try {
            // 解析消息
            WebSocketMessage wsMessage = objectMapper.readValue(
                    message.getPayload(), WebSocketMessage.class);

            String type = wsMessage.getType();

            // 处理心跳
            if ("HEARTBEAT".equals(type)) {
                WebSocketMessage pong = WebSocketMessage.builder()
                        .type("HEARTBEAT")
                        .timestamp(System.currentTimeMillis())
                        .build();
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pong)));
                return;
            }

            // 处理聊天消息
            if ("MESSAGE".equals(type)) {
                Integer toUserId = wsMessage.getToUserId();
                if (toUserId == null) {
                    sendError(session, "缺少接收者ID");
                    return;
                }

                // 保存消息到数据库
                ChatMessage chatMessage = ChatMessage.builder()
                        .fromUserId(fromUserId)
                        .toUserId(toUserId)
                        .content(wsMessage.getContent())
                        .messageType(wsMessage.getMessageType() != null ? wsMessage.getMessageType() : 1)
                        .status(0) // 未读
                        .createTime(LocalDateTime.now())
                        .build();
                chatMessageService.save(chatMessage);

                // 构造返回消息
                WebSocketMessage responseMsg = WebSocketMessage.builder()
                        .type("MESSAGE")
                        .messageId(chatMessage.getId())
                        .fromUserId(fromUserId)
                        .toUserId(toUserId)
                        .content(wsMessage.getContent())
                        .messageType(chatMessage.getMessageType())
                        .status(0)
                        .timestamp(System.currentTimeMillis())
                        .data(chatMessage.getCreateTime().toString())
                        .build();

                String responseJson = objectMapper.writeValueAsString(responseMsg);

                // 发送回发送者（确认消息已发送）
                session.sendMessage(new TextMessage(responseJson));

                // 推送给接收者（如果在线）
                WebSocketSession toSession = sessionManager.getSession(toUserId);
                if (toSession != null && toSession.isOpen()) {
                    // 为接收者补全消息内容
                    WebSocketMessage toMsg = WebSocketMessage.builder()
                            .type("MESSAGE")
                            .messageId(chatMessage.getId())
                            .fromUserId(fromUserId)
                            .toUserId(toUserId)
                            .content(wsMessage.getContent())
                            .messageType(chatMessage.getMessageType())
                            .status(0)
                            .timestamp(System.currentTimeMillis())
                            .data(chatMessage.getCreateTime().toString())
                            .build();
                    String toJson = objectMapper.writeValueAsString(toMsg);
                    toSession.sendMessage(new TextMessage(toJson));

                    // 同时推送 UNREAD_COUNT 给接收者（实时更新导航栏角标）
                    pushUnreadCount(toUserId);
                }
            }
        } catch (Exception e) {
            log.error("处理 WebSocket 消息异常", e);
            sendError(session, "消息处理失败");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Integer userId = (Integer) session.getAttributes().get("userId");
        if (userId != null) {
            sessionManager.removeSession(userId);
            // 广播用户下线状态给所有其他在线用户
            broadcastUserStatus(userId, false);
        }
        log.info("WebSocket 连接关闭: sessionId={}, status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Integer userId = (Integer) session.getAttributes().get("userId");
        log.error("WebSocket 传输错误: userId={}", userId, exception);
        sessionManager.removeBySession(session);
        session.close(CloseStatus.SERVER_ERROR);
    }

    /**
     * 发送错误消息给客户端
     */
    private void sendError(WebSocketSession session, String errorMsg) {
        try {
            WebSocketMessage error = WebSocketMessage.builder()
                    .type("ERROR")
                    .content(errorMsg)
                    .timestamp(System.currentTimeMillis())
                    .build();
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
        } catch (Exception e) {
            log.error("发送错误消息失败", e);
        }
    }

    /**
     * 广播用户上下线状态给所有其他在线用户
     */
    private void broadcastUserStatus(Integer userId, boolean online) {
        WebSocketMessage statusMsg = WebSocketMessage.builder()
                .type("USER_STATUS")
                .fromUserId(userId)
                .data(online)
                .timestamp(System.currentTimeMillis())
                .build();
        try {
            String json = objectMapper.writeValueAsString(statusMsg);
            for (WebSocketSession otherSession : sessionManager.getAllSessions()) {
                Integer otherUserId = (Integer) otherSession.getAttributes().get("userId");
                // 不发送给自己
                if (otherUserId != null && !otherUserId.equals(userId) && otherSession.isOpen()) {
                    try {
                        otherSession.sendMessage(new TextMessage(json));
                    } catch (Exception e) {
                        log.warn("广播用户状态失败: fromUserId={}, toUserId={}", userId, otherUserId, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("广播用户状态异常: userId={}", userId, e);
        }
    }

    /**
     * 推送当前用户的总未读消息数
     */
    private void pushUnreadCount(Integer userId) {
        try {
            WebSocketSession targetSession = sessionManager.getSession(userId);
            if (targetSession == null || !targetSession.isOpen()) {
                return;
            }
            Integer totalUnread = chatMessageService.getTotalUnreadCount(userId);
            WebSocketMessage unreadMsg = WebSocketMessage.builder()
                    .type("UNREAD_COUNT")
                    .data(totalUnread != null ? totalUnread : 0)
                    .timestamp(System.currentTimeMillis())
                    .build();
            targetSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(unreadMsg)));
        } catch (Exception e) {
            log.error("推送未读数异常: userId={}", userId, e);
        }
    }
}
