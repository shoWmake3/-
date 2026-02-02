<template>
  <div class="container">
    <div class="profile-card">
      <div class="avatar-section" @click="handleAvatarClick">
        <image class="avatar" :src="user.avatar || '/static/logo.png'" mode="aspectFill"></image>
        <div class="camera-icon">📷</div>
      </div>

      <div class="info-section">
        <text class="nickname">{{ user.nickname || '未登录' }}</text>
        <view class="tags-row">
          <text class="identity-tag" :class="user.identityType">
            {{ getIdentityName(user.identityType) }}
          </text>
          <text class="level-tag">
            {{ getBadgeIcon(user.reputation) }} {{ getBadgeName(user.reputation) }}
          </text>
        </view>
        <text class="bio">ID: {{ user.id || '--' }}</text>
      </div>

      <div class="reputation-bar-box">
        <div class="rep-header">
          <text class="rep-label">信誉声望</text>
          <text class="rep-val">{{ user.reputation || 0 }}</text>
        </div>
        <div class="progress-bg">
          <div class="progress-fill" :style="{ width: getProgressWidth(user.reputation) + '%' }"></div>
        </div>
        <text class="rep-tip">发帖 +5 / 评论 +2 / 获赞 +1</text>
      </div>

      <div class="stats-row">
        <div class="stat-item">
          <text class="num">0</text>
          <text class="label">关注</text>
        </div>
        <div class="stat-item">
          <text class="num">0</text>
          <text class="label">粉丝</text>
        </div>
        <div class="stat-item">
          <text class="num">{{ totalLikes || 0 }}</text>
          <text class="label">获赞</text>
        </div>
      </div>
    </div>

    <div class="menu-list">
      <div class="menu-item" @click="handleLogout">
        <text style="color: #ff2442;">退出登录</text>
        <text class="arrow">></text>
      </div>
      <div class="menu-item" v-if="user.role === 'admin'" @click="goToAdmin">
        <text style="color: #007aff; font-weight: bold;">🛡️ 内容审核后台</text>
        <text class="arrow">></text>
      </div>
      <div class="menu-item" v-if="user.role === 'admin'" @click="goToDashboard">
        <text style="color: #9c27b0; font-weight: bold;">📊 运营数据大屏</text>
        <text class="arrow">></text>
      </div>
    </div>

    <div class="history-section">
      <div class="section-title">我的发布</div>
      <div class="post-list">
        <div class="post-item" v-for="(item, index) in myPosts" :key="index" @click="goToDetail(item)">
          <text class="post-title">{{ item.title }}</text>
          <div class="post-meta">
            <text class="date">{{ formatTime(item.createTime) }}</text>
            <text class="stats">👁️ {{ item.viewCount || 0 }} · ❤️ {{ item.likeCount || 0 }}</text>
          </div>
        </div>
        <div v-if="myPosts.length === 0" class="empty-box">
          <text>暂无发布内容，快去发帖赚声望吧！</text>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';

const user = ref({
  nickname: '加载中...',
  avatar: '',
  id: '',
  identityType: '',
  reputation: 0
});

const myPosts = ref([]);
const totalLikes = ref(0); // 计算总获赞数

onShow(() => {
  const token = uni.getStorageSync('token');
  if (!token) return uni.navigateTo({ url: '/pages/login/login' });

  // 1. 获取用户信息 (改用 /user/info 以获取声望)
  uni.request({
    url: 'http://localhost:8080/user/info', // ⭐ 注意这里改成了 /user/info
    method: 'GET',
    header: { 'Authorization': token },
    success: (res) => {
      if (res.statusCode === 200) {
        user.value = res.data;
        uni.setStorageSync('user', res.data);
      } else {
        handleLogout();
      }
    },
    fail: () => {
      const cacheUser = uni.getStorageSync('user');
      if (cacheUser) user.value = cacheUser;
    }
  });

  // 2. 获取我的发布历史
  uni.request({
    url: 'http://localhost:8080/post/my',
    method: 'GET',
    header: { 'Authorization': token },
    success: (res) => {
      if (res.statusCode === 200) {
        myPosts.value = res.data;
        // 简单计算一下总获赞
        totalLikes.value = res.data.reduce((sum, p) => sum + (p.likeCount || 0), 0);
      }
    }
  });
});

// --- 头像上传逻辑 (保留您原有的) ---
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
        const url = data.data || data; // 兼容不同返回格式

        // 更新前端
        user.value.avatar = url;

        // 更新后端
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

// --- 辅助函数 ---
const formatTime = (t) => t ? t.split('T')[0] : '';

// 身份名称映射
const getIdentityName = (type) => {
  const map = { 'student': '留学生', 'agent': '中介/商户', 'worker': '打工人', 'tourist': '游客' };
  return map[type] || '用户';
};

// 声望相关逻辑
const getBadgeName = (score) => {
  score = score || 0;
  if (score < 0) return '需警惕';
  if (score < 100) return '萌新';
  if (score < 300) return '认证学长';
  return '社区之星';
};

const getBadgeIcon = (score) => {
  score = score || 0;
  if (score < 0) return '⚠️';
  if (score < 100) return '🌱';
  if (score < 300) return '🎓';
  return '👑';
};

const getProgressWidth = (score) => {
  if (!score || score < 0) return 0;
  let p = (score / 500) * 100; // 假设500分满级
  return p > 100 ? 100 : p;
};

const goToAdmin = () => {
  uni.navigateTo({ url: '/pages/admin/audit' });
};

const goToDashboard = () => {
  uni.navigateTo({ url: '/pages/admin/dashboard' });
};
</script>

<style>
.container {
  background-color: #f8f8f8;
  min-height: 100vh;
  padding: 15px;
}

/* 1. 卡片样式 */
.profile-card {
  background: #fff;
  border-radius: 12px;
  padding: 25px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
  margin-bottom: 15px;
}

/* 头像 */
.avatar-section {
  position: relative;
  margin-bottom: 15px;
}

.avatar {
  width: 70px;
  height: 70px;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.camera-icon {
  position: absolute;
  bottom: 0;
  right: 0;
  background: #007aff;
  color: #fff;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  text-align: center;
  line-height: 22px;
  font-size: 12px;
  border: 2px solid #fff;
}

/* 信息 */
.info-section {
  text-align: center;
  width: 100%;
}

.nickname {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
  display: block;
}

.tags-row {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 5px;
}

.identity-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f0f0f0;
  color: #666;
}

.identity-tag.student {
  background: #e3f2fd;
  color: #007aff;
}

.identity-tag.agent {
  background: #fff3e0;
  color: #ff9800;
}

.level-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #fff9c4;
  color: #fbc02d;
  font-weight: bold;
}

.bio {
  font-size: 12px;
  color: #ccc;
  margin-top: 5px;
  display: block;
}

/* ⭐ 声望条 */
.reputation-bar-box {
  width: 100%;
  margin: 20px 0;
  background: #fafafa;
  padding: 10px;
  border-radius: 8px;
}

.rep-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: bold;
}

.rep-val {
  color: #ff9800;
}

.progress-bg {
  height: 6px;
  background: #e0e0e0;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 6px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #ffc107, #ff9800);
  border-radius: 3px;
  transition: width 0.5s;
}

.rep-tip {
  font-size: 10px;
  color: #999;
  text-align: center;
  display: block;
}

/* 统计 */
.stats-row {
  display: flex;
  justify-content: space-around;
  width: 100%;
  border-top: 1px solid #eee;
  padding-top: 15px;
  margin-top: 5px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.num {
  font-weight: bold;
  font-size: 16px;
  color: #333;
}

.label {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

/* 2. 菜单 */
.menu-list {
  background: #fff;
  border-radius: 12px;
  padding: 0 20px;
  margin-bottom: 15px;
}

.menu-item {
  display: flex;
  justify-content: space-between;
  padding: 15px 0;
  font-size: 15px;
  color: #333;
}

.arrow {
  color: #ccc;
}

/* 3. 历史记录 */
.history-section {
  background: #fff;
  border-radius: 12px;
  padding: 15px;
  min-height: 200px;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 15px;
  padding-left: 10px;
  border-left: 4px solid #007aff;
}

.post-item {
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}

.post-title {
  font-size: 15px;
  color: #333;
  margin-bottom: 6px;
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.post-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
}

.empty-box {
  text-align: center;
  padding: 40px 0;
  color: #ccc;
  font-size: 13px;
}
</style>