<template>
    <div class="guest-layout">
        <!-- 顶部导航栏 -->
        <header class="navbar">
            <div class="navbar-inner">
                <div class="navbar-left">
                    <router-link to="/guest-home/market" class="brand-link">
                        <img class="logo" src="/logo.png" alt="logo" />
                        <div class="brand-text">
                            <span class="brand">校园淘物</span>
                            <span class="brand-sub">Campus TaoWu</span>
                        </div>
                    </router-link>

                    <!-- 搜索栏 -->
                    <div class="navbar-search">
                        <div class="search-wrapper">
                            <el-icon class="search-icon"><Search /></el-icon>
                            <el-input
                                v-model="searchKeyword"
                                class="search-input"
                                placeholder="搜索商品、分类..."
                                clearable
                                @keyup.enter="doSearch"
                            />
                            <el-button class="search-btn" @click="doSearch">搜索</el-button>
                        </div>
                    </div>
                </div>

                <div class="navbar-right">
                    <el-button class="btn-login" round @click="goLogin">
                        <el-icon><User /></el-icon>
                        登录
                    </el-button>
                    <el-button class="btn-register" round @click="goRegister">
                        注册
                    </el-button>
                </div>
            </div>

        </header>

        <!-- 主内容区域 -->
        <main class="main-content">
            <router-view />
        </main>

        <!-- 底部 CTA -->
        <footer class="guest-footer">
            <div class="footer-inner">
                <p class="footer-title">还没加入校园淘物？</p>
                <p class="footer-desc">注册账号，开启你的校园闲置好物之旅</p>
                <div class="footer-actions">
                    <el-button class="footer-btn-register" round size="large" @click="goRegister">
                        立即注册
                    </el-button>
                    <el-button class="footer-btn-login" round size="large" @click="goLogin">
                        已有账号？立即登录
                    </el-button>
                </div>
            </div>
        </footer>
    </div>
</template>

<script lang="ts" setup>
/**
 * 游客端 - 布局页
 * <p>提供顶部导航栏（品牌LOGO + 搜索 + 登录/注册入口）、二级导航、主内容区、底部注册引导</p>
 *
 * @author campus-system
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search, User } from '@element-plus/icons-vue'

const router = useRouter()

/** 搜索关键词 */
const searchKeyword = ref('')

/** 执行搜索 */
const doSearch = () => {
    const keyword = searchKeyword.value.trim()
    if (keyword) {
        router.push(`/guest-home/market?search=${encodeURIComponent(keyword)}`)
    } else {
        router.push('/guest-home/market')
    }
}

/** 跳转登录页 */
const goLogin = () => {
    router.push('/login')
}

/** 跳转注册页 */
const goRegister = () => {
    router.push('/register')
}
</script>

<style scoped lang="scss">
@use '../../styles/variables' as *;
/* ====== 布局 ====== */
.guest-layout {
    display: flex;
    flex-direction: column;
    min-height: 100vh;
    background: #fff;
}

/* ====== 顶部导航栏 ====== */
.navbar {
    position: sticky;
    top: 0;
    flex-shrink: 0;
    z-index: 100;
    background: #fff;
    margin: 12px 0;
}

.navbar-inner {
    display: flex;
    align-items: center;
    height: 68px;
    padding: 0 24px;
    max-width: 1200px;
    margin: 0 auto;
    gap: 24px;
}

/* ----- 左侧品牌 + 搜索 ----- */
.navbar-left {
    display: flex;
    align-items: center;
    flex: 1;
    gap: 28px;

    .brand-link {
        display: flex;
        align-items: center;
        gap: 14px;
        text-decoration: none;
        flex-shrink: 0;
    }

    .logo {
        width: 52px;
        height: 52px;
        border-radius: 10px;
    }

    .brand-text {
        display: flex;
        flex-direction: column;
        line-height: 1.2;
    }

    .brand {
        font-size: 26px;
        font-weight: 700;
        color: #1a1a1a;
        letter-spacing: 0.5px;
    }

    .brand-sub {
        font-size: 12px;
        font-weight: 600;
        color: #636366;
        letter-spacing: 1.8px;
        text-transform: uppercase;
        font-style: italic;
        opacity: 0.75;
    }

    .navbar-search { margin-left: 0; }
}

/* ----- 搜索栏 ----- */
.navbar-search {
    max-width: 440px;

    .search-wrapper {
        display: flex;
        align-items: center;
        background: #fff;
        border-radius: 24px;
        padding: 6px 6px 6px 16px;
        border: 1px solid #1a1a1a;
        transition: all 0.3s ease;

        &:focus-within {
            border-color: #1a1a1a;
            box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
        }

        .search-icon {
            color: #aeaeb2;
            font-size: 20px;
            flex-shrink: 0;
        }

        .search-input {
            flex: 1;
            :deep(.el-input__wrapper) {
                background: transparent !important;
                box-shadow: none !important;
                padding: 0 8px;
            }
            :deep(.el-input__inner) {
                font-size: 15px;
                border: none;
                background: transparent;
                color: #1a1a1a;
                &::placeholder { color: #aeaeb2; }
            }
        }

        .search-btn {
            flex-shrink: 0;
            height: 36px;
            padding: 0 20px;
            border-radius: 18px;
            font-size: 14px;
            background: #1a1a1a;
            color: #fff;
            border: none;
            cursor: pointer;
            transition: all 0.25s ease;
            font-weight: 600;

            &:hover {
                background: #333;
                transform: scale(1.04);
            }
        }
    }
}

/* ----- 右侧登录区 ----- */
.navbar-right {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    gap: 12px;

    .btn-login {
        height: 38px;
        font-size: 15px;
        font-weight: 600;
        background: #1a1a1a;
        color: #fff;
        border: none;
        padding: 0 22px;
        transition: all 0.25s ease;

        &:hover {
            background: #333;
            transform: translateY(-1px);
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        }
    }

    .btn-register {
        height: 38px;
        font-size: 15px;
        font-weight: 600;
        background: linear-gradient(135deg, #ff6b35, #e85d3a);
        color: #fff;
        border: none;
        padding: 0 22px;
        transition: all 0.25s ease;

        &:hover {
            background: linear-gradient(135deg, #ff8c5a, #ff7a4a);
            transform: translateY(-1px);
            box-shadow: 0 4px 12px rgba(232, 93, 58, 0.3);
        }
    }
}

/* ====== 主内容区 ====== */
.main-content {
    flex: 1;
    overflow-y: auto;
    max-width: 1200px;
    width: 100%;
    margin: 0 auto;
    padding: 20px 16px;
}

/* ====== 底部注册引导 ====== */
.guest-footer {
    background: linear-gradient(135deg, #1a1a1a, #2d2d2d);
    color: #fff;
    padding: 48px 16px 40px;
    margin-top: 48px;

    .footer-inner {
        max-width: 1200px;
        margin: 0 auto;
        text-align: center;

        .footer-title {
            font-size: 28px;
            font-weight: 700;
            margin: 0 0 8px;
            letter-spacing: 0.5px;
        }

        .footer-desc {
            font-size: 16px;
            color: #aeaeb2;
            margin: 0 0 28px;
        }

        .footer-actions {
            display: flex;
            justify-content: center;
            gap: 16px;
        }

        .footer-btn-register {
            height: 48px;
            font-size: 16px;
            font-weight: 700;
            background: linear-gradient(135deg, #ff6b35, #e85d3a);
            color: #fff;
            border: none;
            padding: 0 40px;
            transition: all 0.25s ease;

            &:hover {
                transform: translateY(-2px);
                box-shadow: 0 8px 20px rgba(255, 107, 53, 0.4);
            }
        }

        .footer-btn-login {
            height: 48px;
            font-size: 16px;
            font-weight: 600;
            background: transparent;
            color: #fff;
            border: 1.5px solid rgba(255, 255, 255, 0.3);
            padding: 0 36px;
            transition: all 0.25s ease;

            &:hover {
                border-color: #fff;
                background: rgba(255, 255, 255, 0.1);
            }
        }
    }
}

/* ====== 响应式 ====== */
@media (max-width: 768px) {
    .navbar-inner { padding: 0 12px; gap: 10px; }
    .navbar-search { max-width: none; }
    .brand { display: none; }
    .main-content { padding: 12px 8px; }
    .guest-footer { padding: 32px 16px 28px; }
    .footer-actions { flex-direction: column; align-items: center; }
}
</style>
