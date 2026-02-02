<template>
  <div class="container">
    <!-- 顶部导航栏：升级为毛玻璃效果 -->
    <div class="nav-bar">
      <div class="search-box">
        <text class="search-icon">🔍</text>
        <input class="search-input" v-model="keyword" placeholder="搜百科、找攻略..." confirm-type="search"
          @confirm="doSearch" />
      </div>
      <div class="add-btn-icon" @click="goToPublish">
        <text class="plus-icon">+</text>
      </div>
    </div>

    <scroll-view scroll-y class="waterfall-container" @scrolltolower="loadMore">
      <div class="waterfall-column">
        <div class="post-card" v-for="(item, index) in postList" :key="index" @click="goToDetail(item)"
          :style="{ animationDelay: index * 0.05 + 's' }">

          <view class="card-cover-wrapper">
            <video v-if="isVideo(getCoverMedia(item))" class="card-cover" :src="getCoverMedia(item)"
              :poster="getThumbnail(item)" :controls="false" :autoplay="true" :muted="true" :loop="true"
              :show-center-play-btn="false" object-fit="cover"></video>

            <image v-else class="card-cover" :src="getCoverMedia(item)" mode="widthFix" @error="handleImgError"></image>

            <view class="video-badge" v-if="isVideo(getCoverMedia(item))">
              <view class="play-icon">▶</view>
            </view>
            
            <!-- 渐变遮罩，增加文字可读性 -->
            <view class="image-overlay"></view>
          </view>

          <div class="card-content">
            <text class="card-title">{{ item.displayTitle }}</text>

            <div class="card-footer">
              <div class="author-box">
                <image class="mini-avatar" :src="item.authorAvatar || '/static/logo.png'" mode="aspectFill"></image>

                <div class="name-col">
                  <text class="mini-name">{{ item.authorName }}</text>
                  <view class="badge-tag" v-if="item.authorReputation >= 100">
                    <text class="badge-icon">{{ getBadgeIcon(item.authorReputation) }}</text>
                    <text class="badge-text">信誉 {{ item.authorReputation }}</text>
                  </view>
                </div>
              </div>
              
              <div class="like-box" @click.stop="handleLike(item)" :class="{ 'liked': item.isLiked }">
                <view class="heart-icon">
                  <text>{{ item.isLiked ? '❤️' : '🤍' }}</text>
                </view>
                <text class="like-num">{{ formatNumber(item.likeCount) }}</text>
              </div>
            </div>

            <div class="trans-btn" @click.stop="handleTranslate(item)" :class="{ 'translated': item.isTranslated }">
              <text class="trans-icon">🌐</text>
              <text class="trans-text">{{ item.isTranslated ? '显示原文' : '智能翻译' }}</text>
            </div>
          </div>
        </div>
      </div>

      <div v-if="postList.length === 0" class="empty-state">
        <view class="empty-icon">📭</view>
        <text class="empty-text">探索世界中...</text>
      </div>
      
      <!-- 底部加载状态 -->
      <view v-if="postList.length > 0" class="loading-footer">
        <view class="loading-dot"></view>
        <view class="loading-dot"></view>
        <view class="loading-dot"></view>
      </view>
      
      <div style="height: 40px;"></div>
    </scroll-view>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';

const postList = ref([]);
const myLocation = ref({ lat: null, lng: null });
const keyword = ref('');

onShow(() => {
  uni.getLocation({
    type: 'wgs84',
    success: (res) => {
      myLocation.value = { lat: res.latitude, lng: res.longitude };
      fetchPosts();
    },
    fail: () => { fetchPosts(); }
  });
});

const fetchPosts = () => {
  let url = 'http://localhost:8080/post/list';
  const params = [];
  if (myLocation.value.lat) {
    params.push(`lat=${myLocation.value.lat}`);
    params.push(`lng=${myLocation.value.lng}`);
  }
  if (keyword.value) {
    params.push(`keyword=${keyword.value}`);
  }
  if (params.length > 0) url += '?' + params.join('&');

  const token = uni.getStorageSync('token');
  const header = {};
  if (token) header['Authorization'] = token;

  uni.request({
    url: url,
    method: 'GET',
    header: header,
    success: (res) => {
      if (res.statusCode === 200) {
        postList.value = res.data.map(p => ({
          ...p,
          isLiked: p.isLiked || false,
          isTranslated: false,
          displayTitle: p.title,
          displayContent: p.content
        }));
      }
    }
  });
};

// 数字格式化（如 1.2k）
const formatNumber = (num) => {
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k';
  }
  return num;
};

// 获取封面图逻辑
const getCoverImage = (item) => {
  if (!item.mediaUrls || item.mediaUrls === '[]') {
    return '/static/logo.png';
  }
  try {
    const urls = JSON.parse(item.mediaUrls);
    if (Array.isArray(urls) && urls.length > 0) {
      return urls[0];
    }
  } catch (e) {
    return '/static/logo.png';
  }
  return '/static/logo.png';
};

const handleTranslate = (item) => {
  if (item.isTranslated) {
    item.displayTitle = item.title;
    item.displayContent = item.content;
    item.isTranslated = false;
  } else {
    uni.showLoading({ title: '翻译中...' });
    const hasJapanese = /[ぁ-んァ-ン]/.test(item.content);
    const targetLang = hasJapanese ? 'zh' : 'jp';
    const separator = "\n\n|||\n\n";
    const fullText = `${item.title}${separator}${item.content}`;
    uni.request({
      url: `http://localhost:8080/tool/translate`,
      method: 'GET',
      data: { content: fullText, target: targetLang },
      success: (res) => {
        uni.hideLoading();
        if (res.statusCode === 200) {
          const result = res.data;
          if (result.includes('|||')) {
            const parts = result.split('|||');
            item.displayTitle = parts[0].trim();
            item.displayContent = parts[1].trim();
          } else {
            item.displayContent = result;
          }
          item.isTranslated = true;
        }
      },
      fail: () => { uni.hideLoading(); }
    });
  }
};

const doSearch = () => { fetchPosts(); };

const handleLike = (item) => {
  const token = uni.getStorageSync('token');
  if (!token) { return uni.showToast({ title: '请先登录', icon: 'none' }); }
  uni.request({
    url: `http://localhost:8080/post/like?postId=${item.id}`,
    method: 'POST',
    header: { 'Authorization': token },
    success: (res) => {
      if (res.statusCode === 200) {
        if (res.data === '点赞成功') {
          item.likeCount++;
          item.isLiked = true;
        } else {
          item.likeCount--;
          item.isLiked = false;
        }
      }
    }
  });
};

const goToDetail = (item) => {
  uni.setStorageSync('currentPost', item);
  uni.navigateTo({ url: `/pages/post-detail/post-detail?id=${item.id}` });
};
const goToPublish = () => {
  const token = uni.getStorageSync('token');
  if (!token) { return uni.navigateTo({ url: '/pages/login/login' }); }
  uni.navigateTo({ url: '/pages/publish/publish' });
};

const isVideo = (url) => {
  if (!url) return false;
  const lower = url.toLowerCase();
  return lower.endsWith('.mp4') || lower.endsWith('.mov') || lower.endsWith('.webm') || lower.endsWith('.avi');
};

const getCoverMedia = (item) => {
  if (item.mediaUrls && item.mediaUrls !== '[]') {
    try {
      const urls = JSON.parse(item.mediaUrls);
      return urls[0];
    } catch (e) { return '/static/logo.png'; }
  }
  return '/static/logo.png';
};

const getThumbnail = (item) => {
  if (item.mediaUrls && item.mediaUrls !== '[]') {
    try {
      const urls = JSON.parse(item.mediaUrls);
      const img = urls.find(url => {
        const u = url.toLowerCase();
        return u.endsWith('.jpg') || u.endsWith('.png') || u.endsWith('.jpeg') || u.endsWith('.webp');
      });
      return img || '';
    } catch (e) { return ''; }
  }
  return '';
};

const handleImgError = (e) => {
  console.error("图片加载失败", e);
};

const getBadgeIcon = (score) => {
  score = score || 0;
  if (score < 0) return '⚠️';
  if (score < 100) return '';
  if (score < 300) return '🎓';
  return '👑';
};
</script>

<style>
/* CSS 变量定义：便于主题统一管理 */
:root {
  --primary-color: #6366f1;
  --primary-light: #818cf8;
  --bg-color: #f8fafc;
  --card-bg: #ffffff;
  --text-primary: #1e293b;
  --text-secondary: #64748b;
  --border-radius: 16px;
  --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  --shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

.container {
  background: linear-gradient(135deg, #f0f4f8 0%, #e2e8f0 100%);
  min-height: 100vh;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* 顶部导航栏 - 毛玻璃效果 */
.nav-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 50px 20px 15px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  position: sticky;
  top: 0;
  z-index: 99;
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.05);
}

.search-box {
  flex: 1;
  background: rgba(241, 245, 249, 0.8);
  height: 42px;
  border-radius: 21px;
  display: flex;
  align-items: center;
  padding: 0 18px;
  margin-right: 15px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  transition: all 0.3s ease;
}

.search-box:focus-within {
  background: rgba(255, 255, 255, 0.95);
  border-color: var(--primary-light);
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
  transform: translateY(-1px);
}

.search-icon {
  font-size: 16px;
  margin-right: 8px;
  color: #94a3b8;
  transition: color 0.3s;
}

.search-box:focus-within .search-icon {
  color: var(--primary-color);
}

.search-input {
  flex: 1;
  font-size: 15px;
  color: var(--text-primary);
  font-weight: 500;
}

.search-input::placeholder {
  color: #94a3b8;
  font-weight: 400;
}

/* 发布按钮 - 悬浮操作按钮样式 */
.add-btn-icon {
  width: 42px;
  height: 42px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.add-btn-icon::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.2) 0%, transparent 100%);
  opacity: 0;
  transition: opacity 0.3s;
}

.add-btn-icon:active {
  transform: scale(0.92);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.add-btn-icon:hover::before {
  opacity: 1;
}

.plus-icon {
  font-weight: 300;
  z-index: 1;
}

/* 瀑布流容器 */
.waterfall-container {
  padding: 12px;
  height: calc(100vh - 110px);
  box-sizing: border-box;
}

.waterfall-column {
  column-count: 2;
  column-gap: 12px;
  max-width: 100%;
  margin: 0 auto;
}

/* 卡片样式 - 现代悬浮效果 */
.post-card {
  break-inside: avoid;
  background: var(--card-bg);
  border-radius: var(--border-radius);
  margin-bottom: 12px;
  overflow: hidden;
  box-shadow: var(--shadow-md);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  animation: slideUp 0.6s ease-out backwards;
  border: 1px solid rgba(226, 232, 240, 0.6);
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.post-card:active {
  transform: scale(0.98) translateY(2px);
  box-shadow: var(--shadow-sm);
}

/* 图片容器 */
.card-cover-wrapper {
  position: relative;
  width: 100%;
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);
  overflow: hidden;
  border-radius: var(--border-radius) var(--border-radius) 0 0;
}

.card-cover {
  width: 100%;
  display: block;
  min-height: 200px;
  transition: transform 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

.post-card:hover .card-cover {
  transform: scale(1.05);
}

/* 图片渐变遮罩 */
.image-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: linear-gradient(to top, rgba(0,0,0,0.3) 0%, transparent 100%);
  opacity: 0;
  transition: opacity 0.3s;
}

.post-card:hover .image-overlay {
  opacity: 1;
}

/* 视频徽章 */
.video-badge {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  transition: all 0.3s;
}

.play-icon {
  color: white;
  font-size: 16px;
  margin-left: 4px;
  text-shadow: 0 2px 4px rgba(0,0,0,0.3);
}

/* 卡片内容区 */
.card-content {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  letter-spacing: -0.01em;
}

/* 底部信息栏 */
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
}

.author-box {
  display: flex;
  align-items: center;
  flex: 1;
  overflow: hidden;
  gap: 8px;
}

.mini-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  transition: transform 0.3s;
}

.post-card:hover .mini-avatar {
  transform: scale(1.1);
}

.name-col {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 2px;
  overflow: hidden;
}

.mini-name {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 信誉徽章 */
.badge-tag {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 10px;
  color: #d97706;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  padding: 2px 6px;
  border-radius: 10px;
  font-weight: 700;
  width: fit-content;
  border: 1px solid rgba(217, 119, 6, 0.2);
}

.badge-icon {
  font-size: 10px;
}

.badge-text {
  font-size: 9px;
  text-transform: uppercase;
  letter-spacing: 0.02em;
}

/* 点赞按钮 */
.like-box {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  background: rgba(241, 245, 249, 0.6);
  border-radius: 20px;
  transition: all 0.3s;
  cursor: pointer;
}

.like-box:active {
  transform: scale(0.9);
}

.like-box.liked {
  background: rgba(254, 226, 226, 0.6);
}

.heart-icon {
  font-size: 14px;
  transition: transform 0.3s cubic-bezier(0.68, -0.55, 0.265, 1.55);
}

.like-box.liked .heart-icon {
  transform: scale(1.2);
}

.like-num {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 600;
  min-width: 20px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.like-box.liked .like-num {
  color: #ef4444;
}

/* 翻译按钮 */
.trans-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  background: rgba(238, 242, 255, 0.8);
  color: var(--primary-color);
  font-size: 11px;
  font-weight: 600;
  padding: 6px 12px;
  border-radius: 20px;
  border: 1px solid rgba(99, 102, 241, 0.2);
  transition: all 0.3s;
  width: fit-content;
  cursor: pointer;
}

.trans-btn.translated {
  background: rgba(236, 253, 245, 0.8);
  color: #059669;
  border-color: rgba(5, 150, 105, 0.2);
}

.trans-btn:active {
  transform: scale(0.95);
}

.trans-icon {
  font-size: 12px;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 16px;
  animation: fadeIn 0.5s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.empty-icon {
  font-size: 64px;
  opacity: 0.6;
  filter: grayscale(0.3);
}

.empty-text {
  color: var(--text-secondary);
  font-size: 15px;
  font-weight: 500;
}

/* 底部加载动画 */
.loading-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  padding: 20px;
  height: 60px;
}

.loading-dot {
  width: 8px;
  height: 8px;
  background: var(--primary-color);
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
  opacity: 0.6;
}

.loading-dot:nth-child(1) { animation-delay: -0.32s; }
.loading-dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* PC 端适配 */
@media screen and (min-width: 800px) {
  .waterfall-column {
    column-count: 3;
    column-gap: 20px;
    max-width: 1200px;
    padding: 0 20px;
  }

  .post-card {
    margin-bottom: 20px;
    border-radius: 20px;
  }
  
  .post-card:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-xl);
  }

  .nav-bar {
    padding: 20px 40px;
    justify-content: center;
    gap: 20px;
  }

  .search-box {
    max-width: 600px;
    height: 48px;
    border-radius: 24px;
  }
  
  .add-btn-icon {
    width: 48px;
    height: 48px;
    font-size: 28px;
  }
}

/* 超小屏适配 */
@media screen and (max-width: 375px) {
  .waterfall-column {
    column-count: 2;
    column-gap: 8px;
  }
  
  .post-card {
    margin-bottom: 8px;
    border-radius: 12px;
  }
  
  .card-content {
    padding: 10px;
  }
  
  .card-title {
    font-size: 13px;
  }
}
</style>