package com.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mapper.ChatMessageMapper;
import com.example.pojo.entity.ChatMessage;
import com.example.pojo.vo.ChatMessageVO;
import com.example.pojo.vo.ConversationVO;
import com.example.service.ChatMessageService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 聊天消息业务逻辑接口实现类
 */
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {

    @Override
    public List<ChatMessageVO> getMessagesBetweenUsers(Integer userId1, Integer userId2, Integer page, Integer size) {
        Integer offset = (page - 1) * size;
        return this.baseMapper.selectMessagesBetweenUsers(userId1, userId2, offset, size);
    }

    @Override
    public Integer getMessageCountBetweenUsers(Integer userId1, Integer userId2) {
        return this.baseMapper.selectMessageCountBetweenUsers(userId1, userId2);
    }

    @Override
    public List<ConversationVO> getConversations(Integer userId) {
        return this.baseMapper.selectConversations(userId);
    }

    @Override
    public void markAsRead(Integer fromUserId, Integer toUserId) {
        this.baseMapper.updateMessagesRead(fromUserId, toUserId);
    }

    @Override
    public Integer getUnreadCount(Integer fromUserId, Integer toUserId) {
        return this.baseMapper.selectUnreadCount(fromUserId, toUserId);
    }

    @Override
    public List<ChatMessageVO> listAdminMessages(com.example.pojo.dto.AdminMessageQueryDTO dto) {
        // Pager 拦截器会将 (current-1)*size 设置到 dto.current，直接传入即可
        return this.baseMapper.selectAdminMessages(dto);
    }

    @Override
    public Integer countAdminMessages(com.example.pojo.dto.AdminMessageQueryDTO dto) {
        return this.baseMapper.countAdminMessages(dto);
    }

    @Override
    public Integer getTotalUnreadCount(Integer userId) {
        return this.baseMapper.selectTotalUnreadCount(userId);
    }
}
