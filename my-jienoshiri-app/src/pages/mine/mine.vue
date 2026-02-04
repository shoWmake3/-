<template>
  <view class="page-container">
    
    <div class="ambient-bg">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
      <div class="noise-overlay"></div>
    </div>

    <scroll-view scroll-y class="scroll-body" show-scrollbar="false">
      
      <view class="status-bar-spacer"></view>
      
      <view style="height: 20px;"></view>

      <view class="profile-card">
        <view class="card-top-row">
          <text class="card-title">My Profile</text>
          <view class="setting-btn" @click="handleLogout">
            <text>退出登录</text>
          </view>
        </view>

        <view class="user-main">
          <view class="avatar-box" @click="handleAvatarClick">
            <image class="avatar" :src="user.avatar || '/static/logo.png'" mode="aspectFill"></image>
            <view class="edit-badge">📷</view>
          </view>
          
          <view class="info-box">
            <view class="name-row">
              <text class="nickname">{{ user.nickname || '未登录' }}</text>
              <view class="id-tag">ID: {{ user.id || '--' }}</view>
            </view>
            
            <view class="tags-row">
              <view class="tag-pill identity" :class="user.identityType">
                {{ getIdentityName(user.identityType) }}
              </view>
              <view class="tag-pill level">
                {{ getBadgeIcon(user.reputation) }} {{ getBadgeName(user.reputation) }}
              </view>
            </view>
          </view>
        </view>

        <view class="rep-section">
          <view class="rep-header">
            <text class="rep-label">信誉声望</text>
            <text class="rep-val">{{ user.reputation || 0 }}</text>
          </view>
          <view class="progress-track">
            <view class="progress-bar" :style="{ width: getProgressWidth(user.reputation) + '%' }">
              <view class="shine"></view>
            </view>
          </view>
          <text class="rep-desc">发帖 +5 / 评论 +2 / 获赞 +1</text>
        </view>

        <view class="stats-grid">
          <view class="stat-item">
            <text class="stat-num">0</text>
            <text class="stat-label">关注</text>
          </view>
          <view class="stat-divider"></view>
          <view class="stat-item">
            <text class="stat-num">0</text>
            <text class="stat-label">粉丝</text>
          </view>
          <view class="stat-divider"></view>
          <view class="stat-item">
            <text class="stat-num">{{ totalLikes || 0 }}</text>
            <text class="stat-label">获赞</text>
          </view>
        </view>
      </view>

      <view class="menu-card admin-panel" v-if="user.role === 'admin'">
        <view class="menu-header">管理中心</view>
        <view class="admin-grid">
          <view class="admin-btn blue" @click="goToAdmin">
            <text class="btn-icon">🛡️</text>
            <text>内容审核</text>
          </view>
          <view class="admin-btn purple" @click="goToDashboard">
            <text class="btn-icon">📊</text>
            <text>数据大屏</text>
          </view>
        </view>
      </view>

      <view class="menu-card">
        <view class="menu-header">我的发布</view>
        <view class="post-list">
          <view class="post-item" v-for="(item, index) in myPosts" :key="index" @click="goToDetail(item)">
            <view class="post-left">
              <text class="post-icon">📝</text>
              <view class="post-info">
                <text class="post-title">{{ item.title }}</text>
                <text class="post-date">{{ formatTime(item.createTime) }}</text>
              </view>
            </view>
            <view class="post-right">
              <text class="post-stat">👁️ {{ item.viewCount || 0 }}</text>
              <text class="post-stat">❤️ {{ item.likeCount || 0 }}</text>
            </view>
          </view>
          
          <view v-if="myPosts.length === 0" class="empty-state">
            <text class="empty-icon">📭</text>
            <text>暂无发布内容</text>
          </view>
        </view>
      </view>
      
      <view class="logout-btn-box">
        <button class="logout-btn" @click="handleLogout">退出登录</button>
      </view>

      <view class="bottom-safe-spacer"></view>
      
    </scroll-view>

  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';

const user = ref({
  nickname: '加载中...', avatar: '', id: '', identityType: '', reputation: 0, role: ''
});
const myPosts = ref([]);
const totalLikes = ref(0);

onShow(() => {
  const token = uni.getStorageSync('token');
  if (!token) return uni.navigateTo({ url: '/pages/login/login' });

  uni.request({
    url: 'http://localhost:8080/user/info',
    method: 'GET',
    header: { 'Authorization': token },
    success: (res) => {
      if (res.statusCode === 200) {
        user.value = res.data;
        uni.setStorageSync('user', res.data);
      } else { handleLogout(); }
    },
    fail: () => {
      const cacheUser = uni.getStorageSync('user');
      if (cacheUser) user.value = cacheUser;
    }
  });

  uni.request({
    url: 'http://localhost:8080/post/my',
    method: 'GET',
    header: { 'Authorization': token },
    success: (res) => {
      if (res.statusCode === 200) {
        myPosts.value = res.data;
        totalLikes.value = res.data.reduce((sum, p) => sum + (p.likeCount || 0), 0);
      }
    }
  });
});

const handleAvatarClick = () => {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    success: (res) => uploadAvatar(res.tempFilePaths[0])
  });
};

const uploadAvatar = (filePath) => {
  uni.showLoading({ title: '上传中...' });
  uni.uploadFile({
    url: 'http://localhost:8080/oss/upload',
    filePath: filePath,
    name: 'file',
    success: (res) => {
      try {
        const data = JSON.parse(res.data);
        const url = data.data || data;
        user.value.avatar = url;
        const token = uni.getStorageSync('token');
        uni.request({
          url: 'http://localhost:8080/user/update',
          method: 'POST',
          header: { 'Authorization': token },
          data: { avatar: url },
          success: () => uni.showToast({ title: '更新成功' })
        });
      } catch (e) { console.error(e); }
    },
    complete: () => uni.hideLoading()
  });
};

const handleLogout = () => {
  uni.removeStorageSync('token');
  uni.removeStorageSync('user');
  uni.reLaunch({ url: '/pages/login/login' });
};

const goToDetail = (item) => {
  uni.setStorageSync('currentPost', item);
  uni.navigateTo({ url: '/pages/post-detail/post-detail' });
};

const formatTime = (t) => t ? t.split('T')[0] : '';
const getIdentityName = (type) => {
  const map = { 'student': '留学生', 'agent': '中介/商户', 'worker': '打工人', 'tourist': '游客' };
  return map[type] || '用户';
};
const getBadgeName = (score) => {
  score = score || 0;
  if (score < 0) return '需警惕'; if (score < 100) return '萌新'; if (score < 300) return '认证学长'; return '社区之星';
};
const getBadgeIcon = (score) => {
  score = score || 0;
  if (score < 0) return '⚠️'; if (score < 100) return '🌱'; if (score < 300) return '🎓'; return '👑';
};
const getProgressWidth = (score) => {
  if (!score || score < 0) return 0;
  let p = (score / 500) * 100; return p > 100 ? 100 : p;
};

const goToAdmin = () => { uni.navigateTo({ url: '/pages/admin/audit' }); };
const goToDashboard = () => { uni.navigateTo({ url: '/pages/admin/dashboard' }); };
</script>

<style>
:root {
  --primary: #6366f1;
  --text-main: #1e293b;
  --text-sub: #64748b;
  --glass-bg: rgba(255, 255, 255, 0.65);
}

/* =================================================================
   📱 手机端适配 (默认)
   ================================================================= */
.page-container {
  /* 关键：占据全屏 */
  height: 100vh;
  width: 100%;
  display: flex; flex-direction: column;
  background: #f8fafc;
  position: relative;
  overflow: hidden; /* 防止页面整体滚动，交给 scroll-view */
}

/* 1. 动态背景 */
.ambient-bg { position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 0; pointer-events: none; }
.noise-overlay { position: absolute; top: 0; left: 0; width: 100%; height: 100%; opacity: 0.03; background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E"); }
.blob { position: absolute; filter: blur(80px); opacity: 0.6; animation: float 10s infinite alternate; }
.blob-1 { top: -10%; left: -10%; width: 60vw; height: 60vw; background: #c4b5fd; }
.blob-2 { bottom: -10%; right: -10%; width: 60vw; height: 60vw; background: #a5f3fc; animation-delay: -2s; }
.blob-3 { top: 40%; left: 30%; width: 40vw; height: 40vw; background: #fecaca; opacity: 0.4; animation-delay: -4s; }
@keyframes float { 0% { transform: translate(0, 0); } 100% { transform: translate(30px, 40px); } }

/* 2. 滚动区域设置 */
.scroll-body {
  flex: 1; 
  height: 0; /* 关键：让 flex 生效 */
  width: 100%;
  position: relative; z-index: 1;
}

/* 垫片：解决顶部刘海遮挡 */
.status-bar-spacer {
  /* 获取系统状态栏高度，如果没有则默认为 20px */
  height: var(--status-bar-height, 20px);
  width: 100%;
}

/* 垫片：解决底部遮挡 */
.bottom-safe-spacer {
  /* 适配 iPhone X 等底部横条 */
  height: calc(40px + env(safe-area-inset-bottom));
  width: 100%;
}

/* 3. 个人信息卡片 */
.profile-card {
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255,255,255,0.8);
  border-radius: 24px; padding: 24px;
  box-shadow: 0 10px 40px -10px rgba(0,0,0,0.05);
  margin: 0 20px 20px; /* 左右留边距 */
}

.card-top-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.card-title { font-size: 14px; font-weight: 800; color: rgba(0,0,0,0.3); text-transform: uppercase; letter-spacing: 1px; }
.setting-btn { font-size: 18px; opacity: 0.6; }

.user-main { display: flex; align-items: center; margin-bottom: 24px; }
.avatar-box { position: relative; margin-right: 16px; }
.avatar { width: 72px; height: 72px; border-radius: 50%; border: 3px solid #fff; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.edit-badge { position: absolute; bottom: 0; right: 0; width: 24px; height: 24px; background: var(--primary); color: #fff; border-radius: 50%; font-size: 12px; display: flex; align-items: center; justify-content: center; border: 2px solid #fff; }

.info-box { flex: 1; }
.name-row { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.nickname { font-size: 20px; font-weight: 800; color: var(--text-main); }
.id-tag { font-size: 10px; color: #94a3b8; background: rgba(255,255,255,0.5); padding: 1px 6px; border-radius: 4px; }

.tags-row { display: flex; gap: 6px; }
.tag-pill { font-size: 11px; padding: 3px 8px; border-radius: 6px; font-weight: 600; }
.tag-pill.identity { background: rgba(99, 102, 241, 0.1); color: var(--primary); }
.tag-pill.level { background: rgba(251, 191, 36, 0.15); color: #d97706; }

/* 声望条 */
.rep-section { background: rgba(255,255,255,0.5); border-radius: 16px; padding: 12px 16px; margin-bottom: 20px; }
.rep-header { display: flex; justify-content: space-between; margin-bottom: 6px; }
.rep-label { font-size: 12px; font-weight: 700; color: #64748b; }
.rep-val { font-size: 14px; font-weight: 800; color: #f59e0b; }

.progress-track { height: 8px; background: rgba(0,0,0,0.05); border-radius: 4px; overflow: hidden; margin-bottom: 6px; position: relative; }
.progress-bar { height: 100%; background: linear-gradient(90deg, #fcd34d, #f59e0b); border-radius: 4px; position: relative; transition: width 0.5s ease-out; }
.shine { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: linear-gradient(90deg, transparent, rgba(255,255,255,0.4), transparent); animation: shine 2s infinite; }
@keyframes shine { from { transform: translateX(-100%); } to { transform: translateX(100%); } }
.rep-desc { font-size: 10px; color: #94a3b8; }

/* 数据统计 */
.stats-grid { display: flex; justify-content: space-around; align-items: center; }
.stat-item { display: flex; flex-direction: column; align-items: center; }
.stat-num { font-size: 18px; font-weight: 800; color: var(--text-main); margin-bottom: 2px; }
.stat-label { font-size: 11px; color: #94a3b8; }
.stat-divider { width: 1px; height: 20px; background: rgba(0,0,0,0.05); }

/* 4. 菜单卡片 */
.menu-card { background: #fff; border-radius: 20px; padding: 20px; margin: 0 20px 16px; box-shadow: 0 4px 20px rgba(0,0,0,0.03); }
.menu-header { font-size: 15px; font-weight: 700; color: var(--text-main); margin-bottom: 16px; }

/* 管理员按钮 */
.admin-grid { display: flex; gap: 12px; }
.admin-btn { flex: 1; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center; gap: 6px; font-size: 13px; font-weight: 600; color: #fff; }
.admin-btn.blue { background: linear-gradient(135deg, #60a5fa, #3b82f6); box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3); }
.admin-btn.purple { background: linear-gradient(135deg, #a78bfa, #8b5cf6); box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3); }
.btn-icon { font-size: 16px; }

/* 发布列表 */
.post-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px dashed rgba(0,0,0,0.05); }
.post-item:last-child { border-bottom: none; }
.post-left { display: flex; align-items: center; gap: 10px; flex: 1; overflow: hidden; }
.post-icon { font-size: 18px; background: #f1f5f9; width: 32px; height: 32px; border-radius: 8px; display: flex; align-items: center; justify-content: center; }
.post-info { display: flex; flex-direction: column; flex: 1; overflow: hidden; }
.post-title { font-size: 14px; font-weight: 600; color: #334155; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.post-date { font-size: 11px; color: #94a3b8; margin-top: 2px; }
.post-right { display: flex; flex-direction: column; align-items: flex-end; gap: 2px; }
.post-stat { font-size: 10px; color: #94a3b8; }

.empty-state { text-align: center; padding: 20px 0; color: #cbd5e1; font-size: 13px; display: flex; flex-direction: column; align-items: center; }
.empty-icon { font-size: 24px; margin-bottom: 6px; opacity: 0.5; }

/* 5. 退出按钮 */
.logout-btn-box { padding: 0 20px; }
.logout-btn {
  background: #fff; border: 1px solid #fee2e2; color: #ef4444; font-size: 14px; font-weight: 600;
  border-radius: 16px; height: 44px; line-height: 44px; margin-top: 10px;
}
.logout-btn:active { background: #fef2f2; }

/* =================================================================
   💻 PC 端适配 (Media Query)
   ================================================================= */
@media screen and (min-width: 800px) {
  /* 1. 容器重置：启用页面级滚动，而非 scroll-view */
  .page-container {
    overflow-y: auto; /* 页面滚动 */
    display: block;   /* 取消 flex */
    height: 100vh;
  }

  /* 2. 滚动区域变容器 */
  .scroll-body {
    display: block; 
    height: auto; 
    overflow: visible;
    padding-bottom: 60px;
  }
  
  /* 3. 内容居中 */
  .profile-card, .menu-card, .logout-btn-box {
    width: 600px; 
    margin-left: auto; 
    margin-right: auto;
  }

  /* PC端去掉顶部的占位 */
  .status-bar-spacer { display: none; }
  
  /* PC端顶部增加边距 */
  .profile-card { margin-top: 40px; }
}
</style>