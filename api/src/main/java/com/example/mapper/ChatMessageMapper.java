package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.pojo.entity.ChatMessage;
import com.example.pojo.vo.ChatMessageVO;
import com.example.pojo.vo.ConversationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 聊天消息持久化接口
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 查询两个用户之间的聊天记录（分页）
     */
    List<ChatMessageVO> selectMessagesBetweenUsers(
            @Param("userId1") Integer userId1,
            @Param("userId2") Integer userId2,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit);

    /**
     * 查询用户的会话列表
     */
    List<ConversationVO> selectConversations(@Param("userId") Integer userId);

    /**
     * 将未读消息标记为已读
     */
    int updateMessagesRead(
            @Param("fromUserId") Integer fromUserId,
            @Param("toUserId") Integer toUserId);

    /**
     * 查询与某用户的未读消息数
     */
    Integer selectUnreadCount(
            @Param("fromUserId") Integer fromUserId,
            @Param("toUserId") Integer toUserId);

    /**
     * 查询消息总数
     */
    Integer selectMessageCountBetweenUsers(
            @Param("userId1") Integer userId1,
            @Param("userId2") Integer userId2);

    /**
     * 管理员 - 分页查询所有消息
     */
    List<ChatMessageVO> selectAdminMessages(@Param("dto") com.example.pojo.dto.AdminMessageQueryDTO dto);

    /**
     * 管理员 - 查询消息总数
     */
    Integer countAdminMessages(@Param("dto") com.example.pojo.dto.AdminMessageQueryDTO dto);

    /**
     * 查询用户的总未读消息数（所有会话的未读数之和）
     */
    Integer selectTotalUnreadCount(@Param("userId") Integer userId);
}
