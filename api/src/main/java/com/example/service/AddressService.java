package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.pojo.dto.AddressAddDTO;
import com.example.pojo.dto.AddressUpdateDTO;
import com.example.pojo.entity.Address;
import com.example.pojo.vo.AddressVO;

import java.util.List;

/**
 * 收货地址业务逻辑接口
 */
public interface AddressService extends IService<Address> {

    /**
     * 查询当前用户的收货地址列表
     *
     * @param userId 用户ID
     * @return 地址VO列表
     */
    List<AddressVO> listByUserId(Integer userId);

    /**
     * 新增收货地址
     * <p>如果设为默认地址，自动清除其他默认</p>
     *
     * @param userId 用户ID
     * @param addDTO 地址参数
     */
    void add(Integer userId, AddressAddDTO addDTO);

    /**
     * 修改收货地址
     *
     * @param userId    操作者ID
     * @param updateDTO 修改参数
     */
    void update(Integer userId, AddressUpdateDTO updateDTO);

    /**
     * 删除收货地址
     *
     * @param userId    操作者ID
     * @param addressId 地址ID
     */
    void deleteById(Integer userId, Integer addressId);

    /**
     * 设为默认地址
     *
     * @param userId    操作者ID
     * @param addressId 地址ID
     */
    void setDefault(Integer userId, Integer addressId);
}
