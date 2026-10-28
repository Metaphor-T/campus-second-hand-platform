package com.example.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话管理器
 * 维护 userId -> WebSocketSession 的映射关系
 */
@Slf4j
@Component
public class WebSocketSessionManager {

    /** 在线用户会话映射（userId -> WebSocketSession） */
    private final ConcurrentHashMap<Integer, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * 添加会话
     */
    public void addSession(Integer userId, WebSocketSession session) {
        // 如果该用户已有旧连接，先关闭
        WebSocketSession oldSession = sessions.get(userId);
        if (oldSession != null && oldSession.isOpen()) {
            try {
                oldSession.close();
            } catch (Exception e) {
                log.warn("关闭旧连接异常: userId={}", userId, e);
            }
        }
        sessions.put(userId, session);
        log.info("用户上线: userId={}, 当前在线人数: {}", userId, sessions.size());
    }

    /**
     * 移除会话
     */
    public void removeSession(Integer userId) {
        sessions.remove(userId);
        log.info("用户离线: userId={}, 当前在线人数: {}", userId, sessions.size());
    }

    /**
     * 获取用户会话
     */
    public WebSocketSession getSession(Integer userId) {
        return sessions.get(userId);
    }

    /**
     * 判断用户是否在线
     */
    public boolean isOnline(Integer userId) {
        WebSocketSession session = sessions.get(userId);
        return session != null && session.isOpen();
    }

    /**
     * 根据 Session 移除（通过遍历找到对应的 userId）
     */
    public void removeBySession(WebSocketSession session) {
        sessions.entrySet().removeIf(entry -> {
            if (entry.getValue().equals(session) || entry.getValue().getId().equals(session.getId())) {
                log.info("用户离线: userId={}, 当前在线人数: {}", entry.getKey(), sessions.size() - 1);
                return true;
            }
            return false;
        });
    }

    /**
     * 获取在线用户数
     */
    public int getOnlineCount() {
        return sessions.size();
    }

    /**
     * 获取所有在线用户 ID
     */
    public java.util.Set<Integer> getAllOnlineUserIds() {
        return sessions.keySet();
    }

    /**
     * 获取所有会话副本（用于广播）
     */
    public java.util.Collection<WebSocketSession> getAllSessions() {
        return sessions.values();
    }
}
