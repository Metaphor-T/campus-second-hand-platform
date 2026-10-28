/**
 * 游客端 类型定义
 *
 * @author campus-system
 */

/** 游客端市场页数据（一次性返回分类+热门+商品列表） */
export interface GuestMarketData {
  /** 分类导航列表 */
  categories: CategoryVO[]
  /** 热门推荐商品 */
  hotProducts: ProductVO[]
  /** 瀑布流商品列表 */
  productList: ProductVO[]
}

/** 分类视图对象 */
export interface CategoryVO {
  /** 分类ID */
  id?: number
  /** 分类名称 */
  name?: string
  /** 排序权重 */
  sort?: number
  /** 状态：0-禁用，1-启用 */
  status?: number
  /** 创建时间 */
  createTime?: string
}

/** 商品视图对象 */
export interface ProductVO {
  /** 商品ID */
  id?: number
  /** 商品标题 */
  title?: string
  /** 商品描述 */
  description?: string
  /** 售价 */
  price?: number
  /** 原价 */
  originalPrice?: number
  /** 库存 */
  stock?: number
  /** 新旧程度：1-全新，2-几乎全新，3-有使用痕迹，4-老旧 */
  condition?: number
  /** 交易方式：1-自提，2-邮寄，3-皆可 */
  tradeType?: number
  /** 状态 */
  status?: number
  /** 浏览量 */
  viewCount?: number
  /** 收藏数 */
  favoriteCount?: number
  /** 想要数 */
  wantCount?: number
  /** 发布者ID */
  userId?: number
  /** 发布者用户名 */
  username?: string
  /** 发布者头像 */
  userAvatar?: string
  /** 分类ID */
  categoryId?: number
  /** 分类名称 */
  categoryName?: string
  /** 商品图册 */
  images?: ProductImageVO[]
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
}

/** 商品图片 */
export interface ProductImageVO {
  /** 图片ID */
  id?: number
  /** 图片URL */
  imageUrl?: string
  /** 是否为封面：1-是，0-否 */
  isCover?: number
}

/** 评价视图对象 */
export interface ProductReviewVO {
  /** 评价ID */
  id?: number
  /** 评价内容 */
  content?: string
  /** 评分：1-5星 */
  rating?: number
  /** 买家姓名 */
  buyerName?: string
  /** 买家头像 */
  buyerAvatar?: string
  /** 卖家回复内容 */
  replyContent?: string
  /** 回复时间 */
  replyTime?: string
  /** 评价图片列表 */
  images?: string[]
  /** 创建时间 */
  createTime?: string
}

/** 评价统计 */
export interface ReviewStats {
  /** 平均评分 */
  avgRating?: number
  /** 评价总数 */
  reviewCount?: number
}

/** 商品查询参数 */
export interface GuestProductQueryDTO {
  /** 分类ID */
  categoryId?: number
  /** 标题搜索 */
  title?: string
  /** 当前页码 */
  current?: number
  /** 每页条数 */
  size?: number
}

/** 评价查询参数 */
export interface GuestReviewQueryDTO {
  /** 商品ID（必填） */
  productId?: number
  /** 当前页码 */
  current?: number
  /** 每页条数 */
  size?: number
}
