// src/types/address.ts 收货地址类型文件

/**
 * 收货地址视图对象
 */
export interface AddressVO {
    id?: number           // 地址主键ID
    userId?: number       // 用户ID
    receiverName?: string // 收货人姓名
    receiverPhone?: string // 收货人手机号
    province?: string     // 省份
    city?: string         // 城市
    district?: string     // 区/县
    detailAddress?: string // 详细地址（街道、门牌号等）
    postalCode?: string   // 邮政编码
    isDefault?: number    // 是否为默认地址：1-是，0-否
    createTime?: string   // 创建时间
    updateTime?: string   // 更新时间
}

/**
 * 新增收货地址参数
 */
export interface AddressAddDTO {
    receiverName?: string   // 收货人姓名
    receiverPhone?: string  // 收货人手机号
    province?: string       // 省份
    city?: string           // 城市
    district?: string       // 区/县
    detailAddress?: string  // 详细地址（街道、门牌号等）
    postalCode?: string     // 邮政编码
    isDefault?: number      // 是否为默认地址：1-是，0-否
}

/**
 * 修改收货地址参数
 */
export interface AddressUpdateDTO {
    id?: number             // 地址ID
    receiverName?: string   // 收货人姓名
    receiverPhone?: string  // 收货人手机号
    province?: string       // 省份
    city?: string           // 城市
    district?: string       // 区/县
    detailAddress?: string  // 详细地址（街道、门牌号等）
    postalCode?: string     // 邮政编码
    isDefault?: number      // 是否为默认地址：1-是，0-否
}
