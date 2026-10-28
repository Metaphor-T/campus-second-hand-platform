package com.example.controller;

import com.example.annotation.Auth;
import com.example.pojo.api.Result;
import com.example.pojo.dto.AddressAddDTO;
import com.example.pojo.dto.AddressUpdateDTO;
import com.example.pojo.enums.RoleEnum;
import com.example.pojo.vo.AddressVO;
import com.example.service.AddressService;
import com.example.utils.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收货地址控制器
 * <p>用户端：增删改查收货地址</p>
 */
@RestController
@RequestMapping(value = "/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    /**
     * 查询当前用户的收货地址列表
     *
     * @return 地址列表
     */
    @Auth({RoleEnum.USER})
    @GetMapping(value = "/list")
    public Result<List<AddressVO>> list() {
        Integer userId = UserContext.getUserId();
        List<AddressVO> list = addressService.listByUserId(userId);
        return Result.success(list);
    }

    /**
     * 新增收货地址
     *
     * @param addDTO 地址参数
     * @return 操作结果
     */
    @Auth({RoleEnum.USER})
    @PostMapping(value = "/add")
    public Result<Void> add(@Valid @RequestBody AddressAddDTO addDTO) {
        Integer userId = UserContext.getUserId();
        addressService.add(userId, addDTO);
        return Result.success();
    }

    /**
     * 修改收货地址
     *
     * @param updateDTO 修改参数
     * @return 操作结果
     */
    @Auth({RoleEnum.USER})
    @PutMapping(value = "/update")
    public Result<Void> update(@Valid @RequestBody AddressUpdateDTO updateDTO) {
        Integer userId = UserContext.getUserId();
        addressService.update(userId, updateDTO);
        return Result.success();
    }

    /**
     * 删除收货地址
     *
     * @param id 地址ID
     * @return 操作结果
     */
    @Auth({RoleEnum.USER})
    @DeleteMapping(value = "/{id}")
    public Result<Void> deleteById(@PathVariable Integer id) {
        Integer userId = UserContext.getUserId();
        addressService.deleteById(userId, id);
        return Result.success();
    }

    /**
     * 设为默认地址
     *
     * @param id 地址ID
     * @return 操作结果
     */
    @Auth({RoleEnum.USER})
    @PutMapping(value = "/default/{id}")
    public Result<Void> setDefault(@PathVariable Integer id) {
        Integer userId = UserContext.getUserId();
        addressService.setDefault(userId, id);
        return Result.success();
    }
}
