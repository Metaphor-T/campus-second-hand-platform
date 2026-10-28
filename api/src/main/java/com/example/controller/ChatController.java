package com.example.controller;

import com.example.annotation.Auth;
import com.example.pojo.api.Result;
import com.example.pojo.entity.BaseUser;
import com.example.pojo.vo.ChatMessageVO;
import com.example.pojo.vo.ConversationVO;
import com.example.service.BaseUserService;
import com.example.service.ChatMessageService;
import com.example.utils.UserContext;
import com.example.websocket.WebSocketMessage;
import com.example.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

/**
 * 聊天消息控制器（REST API）
 */
@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageService chatMessageService;
    private final BaseUserService baseUserService;
    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    /**
     * 获取会话列表
     */
    @GetMapping("/conversations")
    public Result<List<ConversationVO>> getConversations() {
        Integer userId = UserContext.getUserId();
        List<ConversationVO> list = chatMessageService.getConversations(userId);
        // 填充每个会话对象的在线状态
        list.forEach(conv -> conv.setOnline(sessionManager.isOnline(conv.getUserId())));
        return Result.success(list);
    }

    /**
     * 获取与某用户的历史消息
     */
    @GetMapping("/messages")
    public Result<List<ChatMessageVO>> getMessages(
            @RequestParam("userId") Integer targetUserId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        Integer currentUserId = UserContext.getUserId();
        List<ChatMessageVO> list = chatMessageService.getMessagesBetweenUsers(currentUserId, targetUserId, page, size);
        Integer count = chatMessageService.getMessageCountBetweenUsers(currentUserId, targetUserId);
        return Result.success(list, count);
    }

    /**
     * 标记消息为已读
     */
    @PutMapping("/read")
    public Result<Void> markAsRead(@RequestParam("userId") Integer fromUserId) {
        Integer currentUserId = UserContext.getUserId();
        chatMessageService.markAsRead(fromUserId, currentUserId);
        // 标记已读后，推送 UNREAD_COUNT 给当前用户，实时更新导航栏角标
        pushUnreadCount(currentUserId);
        return Result.success();
    }

    /**
     * 获取与某用户的未读消息数
     */
    @GetMapping("/unread")
    public Result<Integer> getUnreadCount(@RequestParam("userId") Integer fromUserId) {
        Integer currentUserId = UserContext.getUserId();
        Integer count = chatMessageService.getUnreadCount(fromUserId, currentUserId);
        return Result.success(count);
    }

    /**
     * 获取总未读消息数
     */
    @GetMapping("/unread/total")
    public Result<Integer> getTotalUnreadCount() {
        Integer userId = UserContext.getUserId();
        List<ConversationVO> conversations = chatMessageService.getConversations(userId);
        int total = conversations.stream()
                .mapToInt(c -> c.getUnreadCount() != null ? c.getUnreadCount() : 0)
                .sum();
        return Result.success(total);
    }

    /**
     * 获取聊天目标用户基本信息（用于创建临时会话）
     */
    @GetMapping("/user/{userId}")
    public Result<ConversationVO> getUserForChat(@PathVariable Integer userId) {
        BaseUser baseUser = baseUserService.getById(userId);
        if (baseUser == null) {
            return Result.fail("用户不存在");
        }
        ConversationVO vo = ConversationVO.builder()
                .userId(baseUser.getId())
                .username(baseUser.getUsername())
                .avatar(baseUser.getAvatar())
                .online(sessionManager.isOnline(userId))
                .build();
        return Result.success(vo);
    }

    /**
     * 通过 WebSocket 推送当前用户的总未读消息数
     */
    private void pushUnreadCount(Integer userId) {
        try {
            WebSocketSession toSession = sessionManager.getSession(userId);
            if (toSession == null || !toSession.isOpen()) {
                return;
            }
            Integer totalUnread = chatMessageService.getTotalUnreadCount(userId);
            WebSocketMessage unreadMsg = WebSocketMessage.builder()
                    .type("UNREAD_COUNT")
                    .data(totalUnread != null ? totalUnread : 0)
                    .timestamp(System.currentTimeMillis())
                    .build();
            toSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(unreadMsg)));
        } catch (Exception e) {
            log.error("推送未读数异常: userId={}", userId, e);
        }
    }
}
