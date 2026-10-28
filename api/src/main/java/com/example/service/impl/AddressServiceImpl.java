package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.exception.BusinessException;
import com.example.mapper.AddressMapper;
import com.example.pojo.dto.AddressAddDTO;
import com.example.pojo.dto.AddressUpdateDTO;
import com.example.pojo.entity.Address;
import com.example.pojo.vo.AddressVO;
import com.example.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 收货地址业务逻辑接口实现类
 */
@Service
@RequiredArgsConstructor
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements AddressService {

    private final AddressMapper addressMapper;

    /**
     * 查询当前用户的收货地址列表（默认地址排在前面）
     */
    @Override
    public List<AddressVO> listByUserId(Integer userId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault)  // 默认地址排最前
                .orderByDesc(Address::getCreateTime);
        List<Address> list = addressMapper.selectList(wrapper);
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 新增收货地址
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Integer userId, AddressAddDTO addDTO) {
        // 如果设为默认，先清除该用户其他默认地址
        if (addDTO.getIsDefault() != null && addDTO.getIsDefault() == 1) {
            clearDefault(userId);
        }

        Address address = Address.builder()
                .userId(userId)
                .receiverName(addDTO.getReceiverName())
                .receiverPhone(addDTO.getReceiverPhone())
                .province(addDTO.getProvince())
                .city(addDTO.getCity())
                .district(addDTO.getDistrict())
                .detailAddress(addDTO.getDetailAddress())
                .postalCode(addDTO.getPostalCode())
                .isDefault(addDTO.getIsDefault() != null ? addDTO.getIsDefault() : 0)
                .createTime(LocalDateTime.now())
                .build();
        this.save(address);
    }

    /**
     * 修改收货地址
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Integer userId, AddressUpdateDTO updateDTO) {
        Address address = this.getById(updateDTO.getId());
        if (address == null) {
            throw new BusinessException("地址不存在");
        }
        if (!Objects.equals(address.getUserId(), userId)) {
            throw new BusinessException("只能修改自己的收货地址");
        }

        // 如果设为默认，先清除该用户其他默认地址
        if (updateDTO.getIsDefault() != null && updateDTO.getIsDefault() == 1) {
            clearDefault(userId);
        }

        LambdaUpdateWrapper<Address> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Address::getId, updateDTO.getId())
                .eq(Address::getUserId, userId)
                .set(Address::getReceiverName, updateDTO.getReceiverName())
                .set(Address::getReceiverPhone, updateDTO.getReceiverPhone())
                .set(Address::getProvince, updateDTO.getProvince())
                .set(Address::getCity, updateDTO.getCity())
                .set(Address::getDistrict, updateDTO.getDistrict())
                .set(Address::getDetailAddress, updateDTO.getDetailAddress())
                .set(Address::getPostalCode, updateDTO.getPostalCode())
                .set(Address::getIsDefault, updateDTO.getIsDefault() != null ? updateDTO.getIsDefault() : 0)
                .set(Address::getUpdateTime, LocalDateTime.now());
        this.update(wrapper);
    }

    /**
     * 删除收货地址
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Integer userId, Integer addressId) {
        Address address = this.getById(addressId);
        if (address == null) {
            throw new BusinessException("地址不存在");
        }
        if (!Objects.equals(address.getUserId(), userId)) {
            throw new BusinessException("只能删除自己的收货地址");
        }
        this.removeById(addressId);
    }

    /**
     * 设为默认地址
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Integer userId, Integer addressId) {
        Address address = this.getById(addressId);
        if (address == null) {
            throw new BusinessException("地址不存在");
        }
        if (!Objects.equals(address.getUserId(), userId)) {
            throw new BusinessException("只能操作自己的收货地址");
        }

        // 清除该用户所有默认地址
        clearDefault(userId);
        // 设置新的默认地址
        LambdaUpdateWrapper<Address> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Address::getId, addressId)
                .eq(Address::getUserId, userId)
                .set(Address::getIsDefault, 1)
                .set(Address::getUpdateTime, LocalDateTime.now());
        this.update(wrapper);
    }

    // ========== 私有辅助方法 ==========

    /**
     * 清除指定用户的所有默认地址标记
     */
    private void clearDefault(Integer userId) {
        LambdaUpdateWrapper<Address> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Address::getUserId, userId)
                .set(Address::getIsDefault, 0)
                .set(Address::getUpdateTime, LocalDateTime.now());
        this.update(wrapper);
    }

    /**
     * 实体转 VO
     */
    private AddressVO toVO(Address address) {
        return AddressVO.builder()
                .id(address.getId())
                .userId(address.getUserId())
                .receiverName(address.getReceiverName())
                .receiverPhone(address.getReceiverPhone())
                .province(address.getProvince())
                .city(address.getCity())
                .district(address.getDistrict())
                .detailAddress(address.getDetailAddress())
                .postalCode(address.getPostalCode())
                .isDefault(address.getIsDefault())
                .createTime(address.getCreateTime())
                .updateTime(address.getUpdateTime())
                .build();
    }
}
