package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.pojo.entity.ChatMessage;
import com.example.pojo.vo.ChatMessageVO;
import com.example.pojo.vo.ConversationVO;

import java.util.List;

/**
 * 聊天消息业务逻辑接口
 */
public interface ChatMessageService extends IService<ChatMessage> {

    /**
     * 查询两个用户之间的聊天记录
     */
    List<ChatMessageVO> getMessagesBetweenUsers(Integer userId1, Integer userId2, Integer page, Integer size);

    /**
     * 查询用户的消息总数
     */
    Integer getMessageCountBetweenUsers(Integer userId1, Integer userId2);

    /**
     * 查询用户的会话列表
     */
    List<ConversationVO> getConversations(Integer userId);

    /**
     * 将未读消息标记为已读
     */
    void markAsRead(Integer fromUserId, Integer toUserId);

    /**
     * 查询与某用户的未读消息数
     */
    Integer getUnreadCount(Integer fromUserId, Integer toUserId);

    /**
     * 管理员 - 分页查询所有消息
     */
    List<ChatMessageVO> listAdminMessages(com.example.pojo.dto.AdminMessageQueryDTO dto);

    /**
     * 管理员 - 查询消息总数
     */
    Integer countAdminMessages(com.example.pojo.dto.AdminMessageQueryDTO dto);

    /**
     * 查询用户的总未读消息数
     */
    Integer getTotalUnreadCount(Integer userId);
}
