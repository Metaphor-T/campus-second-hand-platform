package com.example.controller;

import com.example.annotation.Auth;
import com.example.annotation.Pager;
import com.example.pojo.api.Result;
import com.example.pojo.dto.AdminMessageQueryDTO;
import com.example.pojo.enums.RoleEnum;
import com.example.pojo.vo.ChatMessageVO;
import com.example.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员 - 消息管理控制器
 */
@RestController
@RequestMapping("/admin/messages")
@RequiredArgsConstructor
public class AdminMessageController {

    private final ChatMessageService chatMessageService;

    /**
     * 分页查询所有消息（管理员视角）
     */
    @Pager
    @Auth({RoleEnum.ADMIN})
    @PostMapping("/listPage")
    public Result<List<ChatMessageVO>> listPage(@RequestBody AdminMessageQueryDTO queryDTO) {
        List<ChatMessageVO> list = chatMessageService.listAdminMessages(queryDTO);
        Integer count = chatMessageService.countAdminMessages(queryDTO);
        return Result.success(list, count);
    }

    /**
     * 删除消息
     */
    @Auth({RoleEnum.ADMIN})
    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        chatMessageService.removeById(id);
        return Result.success();
    }
}
