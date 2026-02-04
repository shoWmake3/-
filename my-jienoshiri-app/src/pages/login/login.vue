<template>
  <view class="page-container">
    
    <div class="ambient-bg">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
      <div class="noise-overlay"></div>
    </div>

    <view class="login-card">
      <view class="header">
        <text class="logo-emoji">🌏</text>
        <text class="title">欢迎回来</text>
        <text class="subtitle">登录「皆の知り」开启探索之旅</text>
      </view>

      <view class="form-box">
        <view class="input-group">
          <text class="input-icon">👤</text>
          <input class="glass-input" v-model="form.username" type="text" placeholder="请输入用户名/手机号" placeholder-style="color: #94a3b8" />
        </view>
        
        <view class="input-group">
          <text class="input-icon">🔒</text>
          <input class="glass-input" v-model="form.password" type="password" placeholder="请输入密码" placeholder-style="color: #94a3b8" />
        </view>
        
        <button class="btn-submit" @click="handleLogin" hover-class="btn-hover">登 录</button>
        
        <view class="footer-links">
          <text class="hint">还没有账号？</text>
          <text class="link" @click="goToRegister">立即注册</text>
        </view>
      </view>
    </view>

    <view class="copyright">
      <text>© 2026 Jienoshiri. All Rights Reserved.</text>
    </view>

  </view>
</template>

<script setup>
import { reactive } from 'vue';

const form = reactive({
  username: '',
  password: ''
});

const handleLogin = () => {
  if (!form.username || !form.password) {
    return uni.showToast({ title: '请填写完整', icon: 'none' });
  }

  uni.showLoading({ title: '登录中...' });

  uni.request({
    url: 'http://localhost:8080/auth/login',
    method: 'POST',
    data: form,
    success: (res) => {
      if (res.statusCode === 200 && res.data) {
        const token = res.data;
        uni.setStorageSync('token', token);

        // 获取用户信息
        uni.request({
          url: 'http://localhost:8080/auth/me',
          method: 'GET',
          header: { 'Authorization': token },
          success: (userRes) => {
            uni.hideLoading();
            if (userRes.statusCode === 200 && userRes.data) {
              uni.setStorageSync('user', userRes.data);
              uni.showToast({ title: '欢迎回来', icon: 'success' });
              
              // 延迟跳转，让用户看清提示
              setTimeout(() => {
                uni.reLaunch({ url: '/pages/index/index' });
              }, 1000);
            } else {
              uni.showToast({ title: '获取信息失败', icon: 'none' });
            }
          },
          fail: () => {
            uni.hideLoading();
            uni.showToast({ title: '网络异常', icon: 'none' });
          }
        });

      } else {
        uni.hideLoading();
        const msg = (typeof res.data === 'object' && res.data.message) ? res.data.message : '账号或密码错误';
        uni.showToast({ title: msg, icon: 'none' });
      }
    },
    fail: (err) => {
      uni.hideLoading();
      uni.showToast({ title: '服务器连接失败', icon: 'none' });
    }
  });
};

const goToRegister = () => {
  uni.navigateTo({ url: '/pages/register/register' });
};
</script>

<style>
:root {
  --primary: #6366f1;
  --text-main: #1e293b;
}

/* 1. 容器：全屏居中 */
.page-container {
  height: 100vh;
  width: 100vw;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
  background: #f8fafc;
}

/* 2. 动态背景 (与全局统一) */
.ambient-bg {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 0; pointer-events: none;
}
.noise-overlay { position: absolute; top: 0; left: 0; width: 100%; height: 100%; opacity: 0.03; background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E"); }
.blob { position: absolute; filter: blur(80px); opacity: 0.6; animation: float 10s infinite alternate; }
.blob-1 { top: -10%; left: -10%; width: 50vw; height: 50vw; background: #c4b5fd; }
.blob-2 { bottom: -10%; right: -10%; width: 60vw; height: 60vw; background: #a5f3fc; animation-delay: -2s; }
.blob-3 { top: 40%; left: 30%; width: 40vw; height: 40vw; background: #fecaca; opacity: 0.4; animation-delay: -4s; }
@keyframes float { 0% { transform: translate(0, 0); } 100% { transform: translate(30px, 40px); } }

/* 3. 登录卡片：玻璃拟态 */
.login-card {
  width: 85%;
  max-width: 400px; /* 限制最大宽度，适配平板 */
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 30px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 20px 50px -10px rgba(0,0,0,0.1);
  padding: 40px 30px;
  position: relative;
  z-index: 10;
  animation: cardEntry 0.8s cubic-bezier(0.2, 0.8, 0.2, 1);
}

@keyframes cardEntry {
  from { opacity: 0; transform: translateY(30px) scale(0.95); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

/* 头部信息 */
.header { text-align: center; margin-bottom: 40px; }
.logo-emoji { font-size: 48px; display: block; margin-bottom: 10px; animation: floatEmoji 3s ease-in-out infinite; }
@keyframes floatEmoji { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-10px); } }

.title { font-size: 28px; font-weight: 800; color: var(--text-main); margin-bottom: 8px; display: block; letter-spacing: -0.5px; }
.subtitle { font-size: 14px; color: #64748b; letter-spacing: 1px; }

/* 输入框组 */
.input-group {
  display: flex; align-items: center;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(255,255,255,0.5);
  border-radius: 16px;
  padding: 0 16px;
  margin-bottom: 20px;
  height: 56px;
  transition: all 0.3s;
}

.input-group:focus-within {
  background: #fff;
  border-color: #6366f1;
  box-shadow: 0 4px 20px rgba(99, 102, 241, 0.15);
  transform: translateY(-2px);
}

.input-icon { font-size: 20px; margin-right: 12px; opacity: 0.7; }
.glass-input { flex: 1; height: 100%; font-size: 16px; color: var(--text-main); }

/* 按钮 */
.btn-submit {
  width: 100%; height: 50px; line-height: 50px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff; font-size: 16px; font-weight: 600; letter-spacing: 1px;
  border-radius: 25px; margin-top: 30px;
  box-shadow: 0 10px 25px -5px rgba(99, 102, 241, 0.4);
  transition: all 0.3s;
}
.btn-submit:active { transform: scale(0.98); box-shadow: 0 5px 15px -3px rgba(99, 102, 241, 0.3); }

/* 底部链接 */
.footer-links { margin-top: 24px; text-align: center; font-size: 14px; }
.hint { color: #94a3b8; }
.link { color: #6366f1; font-weight: 600; margin-left: 6px; cursor: pointer; }

/* 版权 */
.copyright {
  position: absolute; bottom: 20px; width: 100%; text-align: center;
  font-size: 10px; color: #cbd5e1;
}

/* =================================================================
   💻 PC 端适配
   ================================================================= */
@media screen and (min-width: 800px) {
  .login-card {
    width: 420px; /* 固定宽度 */
    padding: 60px 50px; /* 加大内边距 */
    box-shadow: 0 30px 80px -20px rgba(0,0,0,0.15);
  }
  
  .title { font-size: 32px; }
  .subtitle { font-size: 15px; }
  
  .btn-submit { cursor: pointer; }
  .btn-submit:hover { transform: translateY(-2px); box-shadow: 0 15px 30px -5px rgba(99, 102, 241, 0.5); }
}
</style>