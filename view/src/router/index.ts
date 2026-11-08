import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getToken, getRole, clearToken, clearRole } from '@/utils/localStorage'
import { userTokenAuthApi } from '@/api/user-api'
import { adminTokenAuthApi } from '@/api/admin-api'

// 定义路由
const routes: RouteRecordRaw[] = [
    {
        path: '/',
        redirect: '/guest-home',
    },
    {
        path: '/login',
        name: 'login',
        component: () => import('@/views/Login.vue'),
        meta: {
            title: '登录页',
            requiresAuth: false
        }
    },
    {
        path: '/register',
        name: 'Register',
        component: () => import('@/views/Register.vue'),
        meta: {
            title: '注册页',
            requiresAuth: false
        }
    },
    {
        path: '/admin-home',
        name: '管理员首页',
        redirect: '/admin-home/dashboards',
        component: () => import('@/views/admin/AdminHome.vue'),
        meta: {
            title: '管理员首页',
            roles: ['ADMIN']
        },
        children: [
            {
                path: 'dashboards',
                name: '仪表盘',
                component: () => import('@/views/admin/Dashboards.vue'),
                meta: {
                    title: '仪表盘',
                    icon: 'Compass',
                    show: true,
                    menuOrder: 1
                }
            },
            {
                path: 'users',
                name: '用户列表',
                component: () => import('@/views/admin/Users.vue'),
                meta: {
                    title: '用户列表',
                    icon: 'Avatar',
                    show: true,
                    menuOrder: 2
                }
            },
            {
                path: 'admins',
                name: '管理员管理',
                component: () => import('@/views/admin/AdminManagement.vue'),
                meta: {
                    title: '管理员管理',
                    icon: 'UserFilled',
                    show: true,
                    menuOrder: 3
                }
            },
            {
                path: 'products',
                name: '商品管理',
                component: () => import('@/views/admin/ProductsLayout.vue'),
                meta: {
                    title: '商品管理',
                    icon: 'Goods',
                    show: true,
                    menuOrder: 4
                },
                children: [
                    {
                        path: '',
                        redirect: '/admin-home/products/list'
                    },
                    {
                        path: 'list',
                        name: '商品列表',
                        component: () => import('@/views/admin/ProductManagement.vue'),
                        meta: {
                            title: '商品列表'
                        }
                    },
                    {
                        path: 'orders',
                        name: '订单管理',
                        component: () => import('@/views/admin/OrderManagement.vue'),
                        meta: {
                            title: '订单管理'
                        }
                    },
                    {
                        path: 'reviews',
                        name: '评价管理',
                        component: () => import('@/views/admin/ReviewManagement.vue'),
                        meta: {
                            title: '评价管理'
                        }
                    },
                ]
            },
            {
                path: 'messages',
                name: '消息管理',
                component: () => import('@/views/admin/MessageManagement.vue'),
                meta: {
                    title: '消息管理',
                    icon: 'ChatDotSquare',
                    show: true,
                    menuOrder: 6
                }
            },
            {
                path: 'categories',
                name: '商品分类',
                component: () => import('@/views/admin/CategoryManagement.vue'),
                meta: {
                    title: '商品分类',
                    icon: 'Grid',
                    show: true,
                    menuOrder: 5
                }
            },
        ]
    },
    {
        path: '/user-home',
        name: '用户首页',
        redirect: '/user-home/market',
        component: () => import('@/views/user/UserHome.vue'),
        meta: {
            title: '用户首页',
            roles: ['USER']
        },
        children: [
            {
                path: 'market',
                name: '商品市场',
                component: () => import('@/views/user/UserMarket.vue'),
                meta: {
                    title: '商品市场'
                }
            },
            {
                path: 'products',
                name: '我的商品',
                component: () => import('@/views/user/UserProducts.vue'),
                meta: {
                    title: '我的商品'
                }
            },
            {
                path: 'favorites',
                name: '我的收藏',
                component: () => import('@/views/user/UserFavorites.vue'),
                meta: {
                    title: '我的收藏'
                }
            },
            {
                path: 'address',
                name: '收货地址',
                component: () => import('@/views/user/UserAddress.vue'),
                meta: {
                    title: '收货地址'
                }
            },
            {
                path: 'product-detail/:id',
                name: '商品详情',
                component: () => import('@/views/user/ProductDetail.vue'),
                meta: {
                    title: '商品详情'
                }
            },
            {
                path: 'create-order/:id',
                name: '创建订单',
                component: () => import('@/views/user/CreateOrder.vue'),
                meta: {
                    title: '确认订单'
                }
            },
            {
                path: 'orders',
                name: '订单中心',
                component: () => import('@/views/user/UserOrders.vue'),
                meta: {
                    title: '订单中心'
                }
            },
            {
                path: 'my-reviews',
                name: '我的评价',
                component: () => import('@/views/user/UserReviews.vue'),
                meta: {
                    title: '我的评价'
                }
            },
            {
                path: 'add-review/:orderId',
                name: '发表评价',
                component: () => import('@/views/user/UserAddReview.vue'),
                meta: {
                    title: '发表评价'
                }
            },
            {
                path: 'chat',
                name: '即时通讯',
                component: () => import('@/views/user/ChatView.vue'),
                meta: {
                    title: '即时通讯'
                }
            },
            {
                path: 'chat/:targetUserId',
                name: '即时通讯-指定用户',
                component: () => import('@/views/user/ChatView.vue'),
                meta: {
                    title: '即时通讯'
                }
            },
        ]
    },
    // ====== 游客端路由（免登录） ======
    {
        path: '/guest-home',
        name: '游客首页',
        redirect: '/guest-home/market',
        component: () => import('@/views/guest/GuestHome.vue'),
        meta: {
            title: '校园淘物 - 发现好物',
            requiresAuth: false
        },
        children: [
            {
                path: 'market',
                name: '游客市场',
                component: () => import('@/views/guest/GuestMarket.vue'),
                meta: {
                    title: '商品市场',
                    requiresAuth: false
                }
            },
            {
                path: 'product-detail/:id',
                name: '游客商品详情',
                component: () => import('@/views/guest/GuestProductDetail.vue'),
                meta: {
                    title: '商品详情',
                    requiresAuth: false
                }
            }
        ]
    },
    // 401 未授权页面
    {
        path: '/401',
        name: '401',
        component: () => import('@/views/error/401.vue'),
        meta: {
            title: '未授权'
        }
    },
    // 404 页面 - 放在最后作为兜底匹配
    {
        path: '/:pathMatch(.*)*',
        name: '404',
        component: () => import('@/views/error/404.vue'),
        meta: {
            title: '页面不存在'
        }
    }
]

// 创建路由实例
const router = createRouter({
    // 使用 HTML5 历史模式
    history: createWebHistory(import.meta.env.BASE_URL),
    routes,
})

// ==================== 路由守卫 ====================
router.beforeEach(async (to, from) => {
    console.log(from)
    // 设置页面标题
    const title = to.meta?.title as string | undefined
    document.title = title ? `${title} - 校园淘物` : '校园淘物'

    // 获取 token 和 角色
    const token = getToken()
    const role = getRole()

    // 1. 目标路由需要角色校验
    if (to.meta?.roles) {
        const requiredRoles = to.meta.roles as string[]

        // 没有 token → 跳转登录页
        if (!token) {
            return { path: '/login', query: { redirect: to.fullPath } }
        }

        // 没有角色或角色不匹配 → 401
        if (!role || !requiredRoles.includes(role)) {
            return '/401'
        }
    }

    // 2. 已有 token 访问登录页 → 根据角色跳转
    if (token && to.path === '/login') {
        return role === 'USER' ? '/user-home' : '/admin-home'
    }

    // 3. 已有 token 访问游客页 → 调用 API 校验 token 有效性
    if (token && to.path.startsWith('/guest-home')) {
        try {
            if (role === 'USER') {
                await userTokenAuthApi()
                return '/user-home'
            } else if (role === 'ADMIN') {
                await adminTokenAuthApi()
                return '/admin-home'
            }
        } catch {
            // token 无效，清除凭证，继续留在游客页
            clearToken()
            clearRole()
        }
    }
})

export default router