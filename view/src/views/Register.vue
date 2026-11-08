<template>
  <div class="register-container">
    <!-- 左侧品牌区 -->
    <div class="hero-section">
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <div class="brand">
          <img src="/logo.png" alt="Logo" class="brand-logo" />
          <span class="brand-name">校园淘物</span>
        </div>
        <h1 class="slogan">开启你的<br />校园淘物之旅</h1>
        <p class="sub-slogan">注册即享 · 校园好物 · 一手掌握</p>
        <div class="hero-features">
          <span class="hero-feature">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
            免费发布商品
          </span>
          <span class="hero-feature">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
            快速匹配买家
          </span>
          <span class="hero-feature">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
            安全校园交易
          </span>
        </div>
      </div>
    </div>

    <!-- 右侧注册表单 -->
    <div class="form-section">
      <div class="form-panel">
        <div class="form-header">
          <h2>创建账号</h2>
          <p>注册成为校园淘物用户</p>
        </div>

        <div class="input-group">
          <div class="input-field">
            <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
            <input v-model="account" type="text" placeholder="设置账号" class="custom-input" @keyup.enter="goRegister" />
          </div>

          <div class="input-field">
            <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
            <input v-model="username" type="text" placeholder="昵称" class="custom-input" @keyup.enter="goRegister" />
          </div>

          <div class="input-field">
            <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
              <path d="M7 11V7a5 5 0 0 1 10 0v4" />
            </svg>
            <input v-model="password" type="password" placeholder="设置密码" class="custom-input" @keyup.enter="goRegister" />
          </div>
        </div>

        <div class="login-action">
          <button class="login-btn" @click="goRegister">
            <span>立即注册</span>
          </button>
        </div>

        <div class="form-footer">
          <span>已有账号？</span>
          <span class="link" @click="router.push('/login')">返回登录</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { registerApi } from "@/api/user-api"

const account = ref('')
const password = ref('')
const username = ref('')
const router = useRouter()

const goRegister = async () => {
  try {
    if (!account.value) {
      ElMessage.warning('账号不能为空')
      return
    }
    if (!username.value) {
      ElMessage.warning('用户名不能为空')
      return
    }
    if (!password.value) {
      ElMessage.warning('密码不能为空')
      return
    }
    await registerApi({
      account: account.value,
      password: password.value,
      username: username.value
    })
    ElMessage.success('注册成功，快去登录吧')
    router.push('/login')
  } catch (error: any) {
    ElMessage.warning(`${error.message}`)
  }
}
</script>

<style lang="scss" scoped>
// ==========================================
// 校园淘物 — 橘黄主题注册页
// ==========================================
$color-primary: #e85d3a;
$color-primary-light: #ff7a4a;
$color-primary-dark: #c9442a;
$color-accent: #ff6b35;
$color-bg: #fafaf9;
$color-white: #ffffff;
$color-text: #1d1d1f;
$color-text-light: #86868b;
$color-border: #e5e5ea;

.register-container {
  width: 100%;
  min-height: 100vh;
  display: flex;
  flex-wrap: wrap;
  background-color: $color-bg;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'PingFang SC', 'Segoe UI', sans-serif;
  overflow: hidden;
  position: relative;

  // ========== 左侧品牌区 ==========
  .hero-section {
    flex: 1.15;
    position: relative;
    background-image: url('/bag.png');
    background-size: cover;
    background-position: center 35%;
    background-repeat: no-repeat;
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1;

    .hero-overlay {
      position: absolute;
      inset: 0;
      background: linear-gradient(160deg,
          rgba(232, 93, 58, 0.82) 0%,
          rgba(232, 93, 58, 0.55) 40%,
          rgba(232, 93, 58, 0.75) 100%);
      z-index: 1;
    }

    .hero-content {
      position: relative;
      z-index: 2;
      text-align: center;
      padding: 2.5rem;
      max-width: 480px;
      animation: fadeInUp 0.9s cubic-bezier(0.16, 1, 0.3, 1);

      .brand {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 12px;
        margin-bottom: 2.5rem;

        .brand-logo {
          width: 44px;
          height: 44px;
          border-radius: 12px;
          background: rgba(255, 255, 255, 0.2);
          padding: 6px;
          backdrop-filter: blur(4px);
        }

        .brand-name {
          font-size: 1.6rem;
          font-weight: 700;
          color: rgba(255, 255, 255, 0.95);
          letter-spacing: 3px;
          text-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
        }
      }

      .slogan {
        font-size: 2.8rem;
        font-weight: 700;
        line-height: 1.3;
        color: #ffffff;
        margin-bottom: 1rem;
        letter-spacing: 2px;
        text-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
      }

      .sub-slogan {
        font-size: 0.95rem;
        color: rgba(255, 255, 255, 0.85);
        letter-spacing: 3px;
        font-weight: 400;
        margin-bottom: 2.5rem;
      }

      .hero-features {
        display: flex;
        flex-wrap: wrap;
        justify-content: center;
        gap: 1.2rem;

        .hero-feature {
          display: flex;
          align-items: center;
          gap: 6px;
          color: rgba(255, 255, 255, 0.9);
          font-size: 0.85rem;
          font-weight: 400;
          letter-spacing: 1px;

          svg {
            width: 14px;
            height: 14px;
            color: rgba(255, 255, 255, 0.7);
          }
        }
      }
    }
  }

  // ========== 右侧表单区 ==========
  .form-section {
    flex: 0.85;
    background: $color-white;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 3rem;
    position: relative;
    z-index: 1;

    .form-panel {
      width: 100%;
      max-width: 420px;

      .form-header {
        margin-bottom: 2rem;
        text-align: center;

        h2 {
          font-size: 2rem;
          font-weight: 700;
          color: $color-text;
          margin-bottom: 0.5rem;
        }

        p {
          color: $color-text-light;
          font-size: 0.95rem;
          font-weight: 400;
        }
      }

      .input-group {
        margin-bottom: 2rem;
        width: 100%;

        .input-field {
          position: relative;
          margin-bottom: 1.25rem;
          width: 100%;

          .input-icon {
            position: absolute;
            left: 14px;
            top: 50%;
            transform: translateY(-50%);
            width: 20px;
            height: 20px;
            color: #aeaeb2;
            pointer-events: none;
            transition: color 0.3s ease;
            z-index: 2;
          }

          .custom-input {
            width: 100%;
            height: 52px;
            padding: 0 1rem 0 46px;
            font-size: 0.95rem;
            font-family: inherit;
            background: #f5f5f7;
            border: 2px solid transparent;
            border-radius: 12px;
            outline: none;
            transition: all 0.3s ease;
            color: $color-text;
            font-weight: 400;
            box-sizing: border-box;

            &::placeholder {
              color: #aeaeb2;
            }

            &:focus {
              border-color: $color-primary;
              background: #fff5f0;
            }
          }

          &:focus-within .input-icon {
            color: $color-primary;
          }
        }
      }

      .login-action {
        margin-bottom: 1.5rem;
        width: 100%;

        .login-btn {
          width: 100%;
          height: 52px;
          background: linear-gradient(135deg, $color-primary 0%, $color-accent 100%);
          color: $color-white;
          border: none;
          border-radius: 12px;
          font-size: 1rem;
          font-weight: 600;
          font-family: inherit;
          cursor: pointer;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          gap: 10px;
          letter-spacing: 1.5px;
          transition: all 0.35s cubic-bezier(0.25, 0.8, 0.25, 1);
          position: relative;
          overflow: hidden;
          box-sizing: border-box;
          box-shadow: 0 4px 14px rgba(232, 93, 58, 0.35);

          &:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(232, 93, 58, 0.45);
          }

          &:active {
            transform: translateY(0);
            box-shadow: 0 2px 8px rgba(232, 93, 58, 0.3);
          }
        }
      }

      .form-footer {
        text-align: center;
        color: $color-text-light;
        font-size: 0.9rem;

        .link {
          color: $color-primary;
          cursor: pointer;
          font-weight: 600;
          margin-left: 4px;
          transition: color 0.2s;

          &:hover {
            color: $color-primary-dark;
            text-decoration: underline;
          }
        }
      }
    }
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1024px) {
  .register-container {
    flex-direction: column;

    .hero-section {
      flex: auto;
      min-height: 40vh;

      .hero-content {
        padding: 2rem;
        max-width: 100%;

        .slogan {
          font-size: 2rem;
        }

        .sub-slogan {
          font-size: 0.8rem;
        }

        .hero-features {
          gap: 0.8rem;

          .hero-feature {
            font-size: 0.75rem;
          }
        }

        .brand {
          margin-bottom: 1.5rem;

          .brand-logo {
            width: 36px;
            height: 36px;
          }

          .brand-name {
            font-size: 1.3rem;
          }
        }
      }
    }

    .form-section {
      flex: auto;
      padding: 2rem;

      .form-panel {
        max-width: 100%;
      }
    }
  }
}

@media (max-width: 768px) {
  .register-container {
    .form-section {
      padding: 1.5rem;

      .form-panel {
        .form-header h2 {
          font-size: 1.6rem;
        }

        .input-field .custom-input {
          height: 46px;
          font-size: 0.9rem;
          padding-left: 42px;
        }

        .input-field .input-icon {
          width: 18px;
          height: 18px;
          left: 12px;
        }

        .login-btn {
          height: 46px;
          font-size: 0.9rem;
        }
      }
    }
  }
}
</style>
