<template>
  <div class="container">
    <div class="profile-card">
      <div class="avatar-section" @click="handleAvatarClick">
        <image class="avatar" :src="user.avatar || '/static/logo.png'" mode="aspectFill"></image>
        <div class="camera-icon">📷</div> </div>
      
      <div class="info-section">
        <text class="nickname">{{ user.nickname || '未登录' }}</text>
        <text class="identity-tag" v-if="user.identityType === 'student'">留学生</text>
        <text class="identity-tag" v-else-if="user.identityType === 'worker'">打工人</text>
        <text class="bio">ID: {{ user.id || '--' }}</text>
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
          <text class="num">0</text>
          <text class="label">获赞</text>
        </div>
      </div>
    </div>

    <div class="menu-list">
      <div class="menu-item">
        <text>我的发布</text>
        <text class="arrow">></text>
      </div>
      <div class="menu-item">
        <text>我的收藏</text>
        <text class="arrow">></text>
      </div>
      <div class="menu-item" @click="handleLogout">
        <text style="color: red;">退出登录</text>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';

const user = ref({
    nickname: '未登录',
    avatar: '',
    id: '',
    identityType: ''
});

onShow(() => {
  const token = uni.getStorageSync('token');
  if (!token) {
    // 没 Token，直接跳登录
    uni.navigateTo({ url: '/pages/login/login' });
    return;
  }

  // ⭐ 核心修复：去后端拉取最新的“我”
  uni.request({
    url: 'http://localhost:8080/auth/me',
    method: 'GET',
    header: { 'Authorization': token },
    success: (res) => {
      if (res.statusCode === 200 && res.data) {
        // 1. 更新页面显示
        user.value = res.data;
        // 2. 顺便更新一下本地缓存，方便其他页面用
        uni.setStorageSync('user', res.data);
      } else {
        // Token 失效了 (后端没返回用户)
        handleLogout();
      }
    },
    fail: () => {
      // 网络断了，尝试用缓存兜底
      const cacheUser = uni.getStorageSync('user');
      if (cacheUser) user.value = cacheUser;
    }
  });
});

const handleAvatarClick = () => {
  uni.chooseImage({
    count: 1, // 只选1张
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      const tempFilePath = res.tempFilePaths[0];
      uploadAvatar(tempFilePath);
    }
  });
};

const uploadAvatar = (filePath) => {
  uni.showLoading({ title: '上传头像中...' });
  
  uni.uploadFile({
    url: 'http://localhost:8080/oss/upload',
    filePath: filePath,
    name: 'file',
    success: (uploadFileRes) => {
      if (uploadFileRes.statusCode === 200) {
        // 1. 拿到 MinIO 返回的新头像地址
        const newAvatarUrl = uploadFileRes.data;
        
        // 2. 前端先变身 (为了体验快)
        user.value.avatar = newAvatarUrl;
        
        // 3. ⭐ 核心修复：调用后端接口，把这个地址存进数据库！
        const token = uni.getStorageSync('token');
        uni.request({
            url: 'http://localhost:8080/user/update',
            method: 'POST',
            header: { 'Authorization': token },
            data: { 
                avatar: newAvatarUrl // 告诉后端：我的头像变了
            },
            success: (res) => {
                if (res.statusCode === 200) {
                    uni.showToast({ title: '头像保存成功', icon: 'success' });
                    // 更新本地缓存，防止下次进来还要闪一下
                    uni.setStorageSync('user', user.value);
                } else {
                    uni.showToast({ title: '保存失败', icon: 'none' });
                }
            }
        });

      } else {
        uni.hideLoading();
        uni.showToast({ title: '上传失败', icon: 'none' });
      }
    },
    fail: () => {
      uni.hideLoading();
      uni.showToast({ title: '网络错误', icon: 'none' });
    },
    complete: () => {
        // 放在最后统一关闭 loading
        uni.hideLoading();
    }
  });
};

const handleLogout = () => {
  uni.removeStorageSync('token');
  uni.removeStorageSync('user');
  uni.reLaunch({ url: '/pages/login/login' });
};
</script>

<style>
.container { background-color: #f5f5f5; min-height: 100vh; padding: 20px; }
.profile-card { background: #fff; border-radius: 12px; padding: 30px 20px; display: flex; flex-direction: column; align-items: center; box-shadow: 0 2px 10px rgba(0,0,0,0.05); }

/* 头像区域 */
.avatar-section { position: relative; margin-bottom: 15px; }
.avatar { width: 80px; height: 80px; border-radius: 50%; border: 2px solid #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.camera-icon { position: absolute; bottom: 0; right: 0; background: #007aff; color: #fff; width: 24px; height: 24px; border-radius: 50%; text-align: center; line-height: 24px; font-size: 12px; border: 2px solid #fff; }

.info-section { text-align: center; margin-bottom: 25px; }
.nickname { font-size: 20px; font-weight: bold; color: #333; display: block; margin-bottom: 5px; }
.identity-tag { font-size: 12px; background: #e3f2fd; color: #007aff; padding: 2px 8px; border-radius: 10px; margin-right: 5px; }
.bio { font-size: 12px; color: #999; }

.stats-row { display: flex; justify-content: space-around; width: 100%; border-top: 1px solid #eee; padding-top: 20px; }
.stat-item { display: flex; flex-direction: column; align-items: center; }
.num { font-weight: bold; font-size: 16px; color: #333; }
.label { font-size: 12px; color: #999; margin-top: 4px; }

.menu-list { margin-top: 20px; background: #fff; border-radius: 12px; padding: 0 20px; }
.menu-item { display: flex; justify-content: space-between; padding: 18px 0; border-bottom: 1px solid #f5f5f5; font-size: 15px; color: #333; }
.menu-item:last-child { border-bottom: none; }
.arrow { color: #ccc; }
</style>