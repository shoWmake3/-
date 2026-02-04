<template>
  <view class="page-container">
    
    <div class="ambient-bg">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
      <div class="noise-overlay"></div>
    </div>

    <scroll-view scroll-y class="scroll-wrapper" show-scrollbar="false">
      <view class="register-card">
        
        <view class="header">
          <text class="title">创建账号</text>
          <text class="subtitle">加入社区，消除信息差</text>
        </view>

        <view class="form-box">
          
          <view class="section-title">基础信息</view>
          <view class="input-group">
            <text class="input-icon">👤</text>
            <input class="glass-input" v-model="form.username" placeholder="设置账号" placeholder-style="color: #94a3b8" />
          </view>
          <view class="input-group">
            <text class="input-icon">🔒</text>
            <input class="glass-input" v-model="form.password" type="password" placeholder="设置密码" placeholder-style="color: #94a3b8" />
          </view>
          <view class="input-group">
            <text class="input-icon">🏷️</text>
            <input class="glass-input" v-model="form.nickname" placeholder="你的昵称" placeholder-style="color: #94a3b8" />
          </view>

          <view class="section-title" style="margin-top: 20px;">选择身份</view>
          <view class="identity-grid">
            <view class="id-card" v-for="item in identities" :key="item.value" 
              :class="{ active: form.identityType === item.value }"
              @click="selectIdentity(item.value)">
              <text class="id-emoji">{{ item.emoji }}</text>
              <text class="id-name">{{ item.name }}</text>
              <view class="check-mark" v-if="form.identityType === item.value">✓</view>
            </view>
          </view>

          <view class="upload-section" v-if="needProof">
            <view class="section-header">
              <text class="section-title">身份验证</text>
              <text class="section-desc">请上传学生证/工作证明</text>
            </view>
            
            <view class="upload-box" @click="chooseImage">
              <image v-if="form.identityProof" :src="form.identityProof" mode="aspectFill" class="preview-img"></image>
              <view v-else class="upload-placeholder">
                <view class="plus-circle">+</view>
                <text class="upload-text">点击上传图片</text>
              </view>
            </view>
            <view class="audit-tip">
              <text class="tip-icon">🛡️</text>
              <text>您的资料将仅用于后台审核，严格保密</text>
            </view>
          </view>

          <button class="btn-submit" @click="handleRegister" :loading="loading">立即注册</button>
          
          <view class="footer-link" @click="goToLogin">
            <text>已有账号？</text>
            <text class="link-text">直接登录</text>
          </view>

        </view>
      </view>
      <view style="height: 40px;"></view>
    </scroll-view>

  </view>
</template>

<script setup>
import { ref, computed } from 'vue';

const form = ref({
  username: '',
  password: '',
  nickname: '',
  identityType: 'student',
  identityProof: ''
});

const loading = ref(false);

const identities = [
  { name: '留学生', value: 'student', emoji: '🎓' },
  { name: '中介', value: 'agent', emoji: '💼' },
  { name: '打工人', value: 'worker', emoji: '🔨' },
  { name: '游客', value: 'tourist', emoji: '✈️' }
];

const needProof = computed(() => {
  return form.value.identityType === 'student' || form.value.identityType === 'agent';
});

const selectIdentity = (val) => {
  form.value.identityType = val;
};

const goToLogin = () => {
  uni.navigateBack();
};

const chooseImage = () => {
  uni.chooseImage({
    count: 1,
    success: (res) => {
      const filePath = res.tempFilePaths[0];
      uni.showLoading({ title: '上传中...' });
      
      uni.uploadFile({
        url: 'http://localhost:8080/oss/upload',
        filePath: filePath,
        name: 'file',
        success: (uploadRes) => {
          uni.hideLoading();
          let url = uploadRes.data;
          try {
             const json = JSON.parse(url);
             if(json.data) url = json.data;
          } catch(e) {}
          
          if (url.startsWith('http')) {
             form.value.identityProof = url;
          } else {
             uni.showToast({ title: '上传失败', icon: 'none' });
          }
        },
        fail: () => {
          uni.hideLoading();
          uni.showToast({ title: '网络错误', icon: 'none' });
        }
      });
    }
  });
};

const handleRegister = () => {
  if (!form.value.username || !form.value.password) return uni.showToast({ title: '请填写完整', icon: 'none' });
  
  if (needProof.value && !form.value.identityProof) {
      return uni.showToast({ title: '请上传身份证明材料', icon: 'none' });
  }

  loading.value = true;
  uni.request({
    url: 'http://localhost:8080/user/register',
    method: 'POST',
    data: form.value,
    success: (res) => {
      loading.value = false;
      if (res.statusCode === 200) {
        uni.showToast({ title: '注册成功', icon: 'success' });
        setTimeout(() => {
          uni.navigateBack();
        }, 1500);
      } else {
        uni.showToast({ title: res.data || '注册失败', icon: 'none' });
      }
    },
    fail: () => {
      loading.value = false;
      uni.showToast({ title: '网络错误', icon: 'none' });
    }
  });
};
</script>

<style>
:root {
  --primary: #6366f1;
  --text-main: #1e293b;
  --glass-bg: rgba(255, 255, 255, 0.65);
}

.page-container {
  height: 100vh; width: 100vw;
  background: #f8fafc;
  position: relative; overflow: hidden;
}

/* 1. 背景 */
.ambient-bg { position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 0; pointer-events: none; }
.noise-overlay { position: absolute; top: 0; left: 0; width: 100%; height: 100%; opacity: 0.03; background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E"); }
.blob { position: absolute; filter: blur(80px); opacity: 0.6; animation: float 10s infinite alternate; }
.blob-1 { top: -10%; left: -10%; width: 50vw; height: 50vw; background: #c4b5fd; }
.blob-2 { bottom: -10%; right: -10%; width: 60vw; height: 60vw; background: #a5f3fc; animation-delay: -2s; }
.blob-3 { top: 40%; left: 30%; width: 40vw; height: 40vw; background: #fecaca; opacity: 0.4; animation-delay: -4s; }
@keyframes float { 0% { transform: translate(0, 0); } 100% { transform: translate(30px, 40px); } }

/* 2. 滚动容器 */
.scroll-wrapper {
  flex: 1; height: 100%;
  position: relative; z-index: 10;
  display: flex; justify-content: center;
  padding-top: 40px;
}

/* 3. 注册卡片 */
.register-card {
  width: 85%; max-width: 440px;
  background: var(--glass-bg);
  backdrop-filter: blur(20px); -webkit-backdrop-filter: blur(20px);
  border-radius: 30px; border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 20px 50px -10px rgba(0,0,0,0.1);
  padding: 30px;
  box-sizing: border-box;
  margin: 0 auto; /* 居中 */
  animation: slideUp 0.6s ease-out;
}
@keyframes slideUp { from { opacity: 0; transform: translateY(40px); } to { opacity: 1; transform: translateY(0); } }

.header { text-align: center; margin-bottom: 30px; }
.title { font-size: 24px; font-weight: 800; color: var(--text-main); display: block; margin-bottom: 4px; }
.subtitle { font-size: 13px; color: #64748b; }

/* 4. 表单区 */
.section-title { font-size: 14px; font-weight: 700; color: #475569; margin-bottom: 12px; margin-left: 4px; }

.input-group {
  display: flex; align-items: center;
  background: rgba(255,255,255,0.5); border: 1px solid rgba(255,255,255,0.6);
  border-radius: 16px; padding: 0 16px; height: 50px; margin-bottom: 12px;
  transition: all 0.3s;
}
.input-group:focus-within { background: #fff; border-color: #6366f1; box-shadow: 0 4px 15px rgba(99, 102, 241, 0.15); }
.input-icon { margin-right: 10px; font-size: 18px; }
.glass-input { flex: 1; height: 100%; font-size: 15px; color: var(--text-main); }

/* 5. 身份选择网格 */
.identity-grid { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 20px; }
.id-card {
  width: 48%; /* 两列布局 */
  background: rgba(255,255,255,0.5); border: 1px solid rgba(255,255,255,0.6);
  border-radius: 16px; padding: 12px; box-sizing: border-box;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  position: relative; transition: all 0.2s; cursor: pointer;
}
.id-card.active {
  background: rgba(99, 102, 241, 0.1); border-color: #6366f1;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.2);
}
.id-emoji { font-size: 24px; margin-bottom: 4px; }
.id-name { font-size: 13px; font-weight: 600; color: var(--text-main); }
.check-mark {
  position: absolute; top: 6px; right: 6px;
  width: 16px; height: 16px; background: #6366f1; border-radius: 50%;
  color: #fff; font-size: 10px; line-height: 16px; text-align: center;
}

/* 6. 上传区 */
.upload-section { margin-top: 20px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; padding: 0 4px; }
.section-desc { font-size: 11px; color: #94a3b8; }

.upload-box {
  width: 100%; height: 140px;
  background: rgba(255,255,255,0.4); border: 2px dashed #cbd5e1;
  border-radius: 16px; overflow: hidden;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.3s;
}
.upload-box:active { background: rgba(99, 102, 241, 0.05); border-color: #6366f1; }
.preview-img { width: 100%; height: 100%; object-fit: cover; }
.upload-placeholder { display: flex; flex-direction: column; align-items: center; color: #94a3b8; }
.plus-circle {
  width: 40px; height: 40px; background: #fff; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; color: #6366f1; box-shadow: 0 4px 10px rgba(0,0,0,0.05);
  margin-bottom: 8px;
}
.upload-text { font-size: 12px; }

.audit-tip {
  display: flex; align-items: center; gap: 6px; margin-top: 8px;
  font-size: 11px; color: #f59e0b; background: rgba(251, 191, 36, 0.1);
  padding: 6px 10px; border-radius: 8px;
}

/* 7. 按钮与底部 */
.btn-submit {
  width: 100%; height: 50px; line-height: 50px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff; font-size: 16px; font-weight: 600; letter-spacing: 1px;
  border-radius: 25px; margin-top: 30px;
  box-shadow: 0 10px 25px -5px rgba(99, 102, 241, 0.4);
  transition: all 0.2s;
}
.btn-submit:active { transform: scale(0.98); }

.footer-link {
  margin-top: 20px; text-align: center; font-size: 13px; color: #94a3b8;
  display: flex; justify-content: center; gap: 4px; cursor: pointer;
}
.link-text { color: #6366f1; font-weight: 600; }

/* PC 适配 */
@media screen and (min-width: 800px) {
  .scroll-wrapper { align-items: center; } /* 垂直居中 */
  .register-card { margin-top: 40px; margin-bottom: 40px; } /* 上下留白 */
}
</style>