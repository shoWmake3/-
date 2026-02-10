<template>
  <div class="container">
    <div class="ambient-bg">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
      <div class="noise-overlay"></div>
    </div>

    <div class="nav-bar">
      <!-- 移动端：只在PC端显示品牌Logo -->
      <div class="nav-left">
        <div class="web-brand">
          <text class="brand-text">皆の知り</text>
        </div>
      </div>
      
      <!-- 移动端：搜索框靠左 -->
      <div class="search-box mobile-search">
        <text class="search-icon">🔍</text>
        <input class="search-input" v-model="keyword" placeholder="搜百科、找攻略、看世界..." confirm-type="search"
          placeholder-style="color: #64748b; font-weight: 300;" @confirm="doSearch" />
      </div>
      
      <!-- 移动端：发布按钮靠右 -->
      <div class="add-btn-wrapper" @click="goToPublish">
        <div class="add-btn-icon">
          <div class="plus-icon-container">
            <text class="plus-icon">+</text>
          </div>
        </div>
        <div class="pulse-ring"></div>
      </div>
    </div>

    <div class="web-hero-section">
      <div class="hero-content">
        <text class="hero-title">探索未知 <br> <span class="gradient-text">连接你的日本新生活</span> <br> </text>
        <text class="hero-sub">Discover the unseen, Share the unexpected.</text>
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
            
            <view class="shine-effect"></view>
          </view>

          <div class="card-content">
            <text class="card-title">{{ item.displayTitle }}</text>

            <div class="card-footer">
              <div class="author-info">
                <view class="avatar-border">
                  <image class="mini-avatar" :src="item.authorAvatar || '/static/logo.png'" mode="aspectFill"></image>
                </view>
                <div class="name-box">
                  <text class="mini-name">{{ item.authorName }}</text>
                  <view class="reputation-pill" v-if="item.authorReputation >= 100">
                    <text class="rep-icon">👑</text>
                    <text class="rep-text">Lv.{{ Math.floor(item.authorReputation / 100) }}</text>
                  </view>
                </div>
              </div>

              <div class="like-wrapper" @click.stop="handleLike(item)">
                <view class="heart-anim" :class="{ 'is-active': item.isLiked }">
                  <text class="icon">{{ item.isLiked ? '❤️' : '🤍' }}</text>
                </view>
                <text class="like-count" :class="{ 'active-text': item.isLiked }">
                  {{ formatNumber(item.likeCount) || '赞' }}
                </text>
              </div>
            </div>

            <div class="action-row">
              <div class="glass-pill" @click.stop="handleTranslate(item)" :class="{ 'active': item.isTranslated }">
                <text class="trans-icon">🌐</text>
                <text>{{ item.isTranslated ? '原文' : ' 翻译' }}</text>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="postList.length === 0" class="empty-state">
        <div class="empty-circle">
          <view class="empty-emoji">🎌</view>
        </div>
        <text class="empty-title">开启你的探索之旅</text>
      </div>

      <view v-if="postList.length > 0" class="loading-footer">
        <view class="wave-dot"></view>
        <view class="wave-dot"></view>
        <view class="wave-dot"></view>
      </view>

      <div style="height: 60px;"></div>
    </scroll-view>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';

const postList = ref([]);
const myLocation = ref({ lat: null, lng: null });
const keyword = ref('');
const isLoadingPosts = ref(false);
let fetchSeq = 0;

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
  if (isLoadingPosts.value) return;
  isLoadingPosts.value = true;
  const currentSeq = ++fetchSeq;

  const query = [];
  if (myLocation.value.lat && myLocation.value.lng) {
    query.push(`lat=${encodeURIComponent(myLocation.value.lat)}`);
    query.push(`lng=${encodeURIComponent(myLocation.value.lng)}`);
  }
  if (keyword.value.trim()) {
    query.push(`keyword=${encodeURIComponent(keyword.value.trim())}`);
  }
  const url = `http://localhost:8080/post/list${query.length ? `?${query.join('&')}` : ''}`;

  const token = uni.getStorageSync('token');
  const header = {};
  if (token) header['Authorization'] = token;

  uni.request({
    url: url,
    method: 'GET',
    header: header,
    success: (res) => {
      if (currentSeq !== fetchSeq) return;
      if (res.statusCode === 200) {
        postList.value = res.data.map(p => ({
          ...p,
          isLiked: p.isLiked || false,
          isTranslated: false,
          displayTitle: p.title,
          displayContent: p.content,
          mediaList: parseMediaUrls(p.mediaUrls),
          isLiking: false,
          isTranslating: false
        }));
      }
    },
    complete: () => {
      if (currentSeq === fetchSeq) {
        isLoadingPosts.value = false;
      }
    }
  });
};

const formatNumber = (num) => {
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k';
  return num;
};

const parseMediaUrls = (mediaUrls) => {
  if (!mediaUrls || mediaUrls === '[]') return [];
  try {
    const urls = JSON.parse(mediaUrls);
    return Array.isArray(urls) ? urls : [];
  } catch (e) {
    return [];
  }
};

const getCoverMedia = (item) => item.mediaList[0] || '/static/logo.png';

const getThumbnail = (item) => {
  const img = item.mediaList.find(url => {
    const u = url.toLowerCase();
    return u.endsWith('.jpg') || u.endsWith('.png') || u.endsWith('.jpeg') || u.endsWith('.webp');
  });
  return img || '';
};

const handleTranslate = (item) => {
  if (item.isTranslating) return;
  if (item.isTranslated) {
    item.displayTitle = item.title;
    item.displayContent = item.content;
    item.isTranslated = false;
  } else {
    item.isTranslating = true;
    uni.showLoading({ title: '翻译中...' });
    const hasJapanese = /[ぁ-んァ-ン一-龯]/.test(item.content || item.title);
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
      fail: () => { uni.hideLoading(); },
      complete: () => {
        item.isTranslating = false;
      }
    });
  }
};

const doSearch = () => { fetchPosts(); };

const handleLike = (item) => {
  const token = uni.getStorageSync('token');
  if (!token) { return uni.showToast({ title: '请先登录', icon: 'none' }); }
  if (item.isLiking) return;
  item.isLiking = true;
  uni.request({
    url: `http://localhost:8080/post/like?postId=${item.id}`,
    method: 'POST',
    header: { 'Authorization': token },
    success: (res) => {
      if (res.statusCode === 200) {
        if (res.data === '点赞成功') {
          item.likeCount = Number(item.likeCount || 0) + 1;
          item.isLiked = true;
        } else {
          item.likeCount = Math.max(0, Number(item.likeCount || 0) - 1);
          item.isLiked = false;
        }
      }
    },
    complete: () => {
      item.isLiking = false;
    }
  });
};

const loadMore = () => {
  // 当前后端接口未提供分页，这里避免触底时报错，后续可替换为分页请求
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
const handleImgError = () => {};
const isVideo = (url) => url && (url.endsWith('.mp4') || url.endsWith('.mov'));
</script>

<style>
:root {
  --primary: #6366f1;
  --bg-page: #f8fafc;
  --text-main: #1e293b;
  --glass-border: rgba(255, 255, 255, 0.4);
  --glass-highlight: rgba(255, 255, 255, 0.8);
}

.container {
  min-height: 100vh;
  position: relative;
  background: var(--bg-page);
  font-family: -apple-system, sans-serif;
  overflow-x: hidden;
}

/* --- 1. 动态流体背景 & 噪点 --- */
.ambient-bg {
  position: fixed;
  top: 0; left: 0; width: 100%; height: 100%;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
  background: #f1f5f9;
}

.noise-overlay {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  opacity: 0.03;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E");
}

.blob {
  position: absolute;
  filter: blur(90px);
  opacity: 0.7;
  animation: float 12s infinite alternate cubic-bezier(0.4, 0, 0.2, 1);
}

.blob-1 { top: -10%; left: -10%; width: 50vw; height: 50vw; background: #c4b5fd; animation-delay: 0s; }
.blob-2 { bottom: -10%; right: -10%; width: 60vw; height: 60vw; background: #a5f3fc; animation-delay: -3s; animation-duration: 15s; }
.blob-3 { top: 40%; left: 30%; width: 40vw; height: 40vw; background: #fecaca; opacity: 0.5; animation-delay: -5s; }

@keyframes float {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(40px, 60px) scale(1.1); }
}

/* --- 2. 导航栏 --- */
.nav-bar {
  position: sticky;
  top: 0; z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between; /* 移动端：两边对齐 */
  padding: 50px 20px 15px;
  background: rgba(255, 255, 255, 0.2); 
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-bottom: 1px solid var(--glass-border);
}

/* 移动端：隐藏左侧品牌Logo容器 */
.nav-left {
  display: none;
}

.web-hero-section { display: none; }

/* 移动端搜索框样式 */
.mobile-search {
  flex: 1; /* 占据剩余空间 */
  height: 44px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 50px;
  display: flex; 
  align-items: center;
  padding: 0 16px;
  border: 1px solid rgba(255,255,255,0.8);
  box-shadow: 0 2px 10px rgba(0,0,0,0.03);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  margin-right: 12px; /* 与右边按钮保持间距 */
}

.mobile-search:focus-within {
  background: #fff;
  box-shadow: 0 8px 30px rgba(99, 102, 241, 0.15);
  transform: translateY(-1px) scale(1.01);
  border-color: #fff;
}

.search-icon { opacity: 0.5; margin-right: 8px; }
.search-input { flex: 1; font-size: 14px; color: #1e293b; }

/* 发布按钮 & 呼吸光环 */
.add-btn-wrapper { 
  position: relative; 
  width: 44px; 
  height: 44px; 
  flex-shrink: 0; /* 防止按钮被压缩 */
}

.add-btn-icon {
  width: 100%; 
  height: 100%;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-radius: 50%;
  display: flex; 
  align-items: center; 
  justify-content: center;
  color: #fff; 
  font-size: 26px; 
  font-weight: 300;
  box-shadow: 0 8px 20px rgba(99, 102, 241, 0.3);
  position: relative; 
  z-index: 2;
  transition: transform 0.2s;
}

/* 新增：加号容器，确保加号居中 */
.plus-icon-container {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  position: relative;
}

/* 优化加号位置 */
.plus-icon {
  display: block;
  font-size: 26px;
  font-weight: 300;
  line-height: 1;
  transform: translateY(-1px);
}

.add-btn-wrapper:active .add-btn-icon { transform: scale(0.92); }

.pulse-ring {
  position: absolute; 
  top: 0; 
  left: 0; 
  width: 100%; 
  height: 100%;
  border-radius: 50%;
  border: 2px solid rgba(99, 102, 241, 0.5);
  animation: pulse 2s infinite;
  z-index: 1;
}

@keyframes pulse {
  0% { transform: scale(1); opacity: 0.8; }
  100% { transform: scale(1.5); opacity: 0; }
}

/* --- 3. 瀑布流 --- */
.waterfall-container {
  height: calc(100vh - 110px);
  padding: 12px;
  box-sizing: border-box;
  position: relative; z-index: 1;
}

.waterfall-column { column-count: 2; column-gap: 12px; }

.post-card {
  break-inside: avoid;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 20px;
  margin-bottom: 12px;
  overflow: hidden;
  box-shadow: 0 10px 40px -10px rgba(0,0,0,0.05);
  border: 1px solid rgba(255,255,255,0.4);
  border-top: 1px solid rgba(255,255,255,0.9);
  
  transform: translateZ(0);
  animation: fadeUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) backwards;
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.card-cover-wrapper { position: relative; overflow: hidden; background: #e2e8f0; }
.card-cover { width: 100%; display: block; min-height: 150px; opacity: 1; transition: opacity 0.3s; }

/* 视频角标 */
.video-badge {
  position: absolute; top: 10px; right: 10px;
  width: 28px; height: 28px;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  border: 1px solid rgba(255,255,255,0.3);
}
.play-icon { color: #fff; font-size: 10px; margin-left: 2px; }

/* 悬停光扫过 */
.shine-effect {
  position: absolute; top: 0; left: -100%; width: 50%; height: 100%;
  background: linear-gradient(to right, transparent, rgba(255,255,255,0.4), transparent);
  transform: skewX(-25deg);
  transition: 0.6s; pointer-events: none;
}

.card-content { padding: 14px; }

.card-title {
  font-size: 15px; font-weight: 700; color: #1e293b;
  margin-bottom: 12px; line-height: 1.5;
  display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; line-clamp: 2; overflow: hidden;
  letter-spacing: -0.2px;
}

.card-footer { display: flex; justify-content: space-between; align-items: center; }

.author-info { display: flex; align-items: center; gap: 8px; }
.avatar-border { padding: 1px; background: #fff; border-radius: 50%; box-shadow: 0 2px 5px rgba(0,0,0,0.05); }
.mini-avatar { width: 24px; height: 24px; border-radius: 50%; display: block; }
.name-box { display: flex; flex-direction: column; }
.mini-name { font-size: 12px; font-weight: 600; color: #475569; max-width: 80px; overflow: hidden; white-space: nowrap; text-overflow: ellipsis;}
.reputation-pill { display: flex; align-items: center; gap: 2px; margin-top: 2px; }
.rep-icon { font-size: 8px; }
.rep-text { font-size: 9px; font-weight: 700; color: #d97706; background: rgba(251, 191, 36, 0.15); padding: 0 4px; border-radius: 4px; }

.like-wrapper { display: flex; align-items: center; gap: 4px; background: rgba(241,245,249,0.5); padding: 4px 8px; border-radius: 12px; }
.like-count { font-size: 11px; font-weight: 600; color: #94a3b8; }
.like-count.active-text { color: #ef4444; }

.action-row { margin-top: 12px; padding-top: 10px; border-top: 1px dashed rgba(0,0,0,0.05); }
.glass-pill {
  font-size: 10px; font-weight: 600; color: #6366f1;
  background: rgba(99, 102, 241, 0.06);
  padding: 4px 10px; border-radius: 20px;
  display: inline-flex; align-items: center; gap: 4px;
  border: 1px solid rgba(99, 102, 241, 0.1);
  transition: all 0.2s;
}
.glass-pill.active { background: #6366f1; color: #fff; border-color: #6366f1; }

/* 空状态 */
.empty-state {
  display: flex; flex-direction: column; align-items: center;
  padding-top: 100px; opacity: 0.8;
}
.empty-circle {
  width: 80px; height: 80px; background: #fff; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 10px 30px rgba(0,0,0,0.05);
  margin-bottom: 20px;
}
.empty-emoji { font-size: 36px; }
.empty-title { color: #64748b; font-size: 15px; font-weight: 500; letter-spacing: 1px; }

.loading-footer { display: flex; justify-content: center; gap: 8px; padding: 24px; }
.wave-dot { width: 8px; height: 8px; background: #cbd5e1; border-radius: 50%; animation: wave 1.4s infinite ease-in-out; }
.wave-dot:nth-child(2) { animation-delay: 0.2s; }
.wave-dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes wave { 0%, 100% { transform: translateY(0); opacity: 0.5; } 50% { transform: translateY(-8px); opacity: 1; } }

/* --- 💻 PC/Web 端专属适配 (Media Query) --- */
@media screen and (min-width: 800px) {
  .nav-bar { 
    padding: 20px 60px; 
    background: rgba(255, 255, 255, 0.6); 
  }

  /* PC端：显示左侧品牌Logo容器 */
  .nav-left {
    display: flex;
    flex: 1;
    justify-content: flex-start;
    align-items: center;
  }

  /* PC端：显示品牌Logo */
  .web-brand { 
    display: flex; 
    align-items: center; 
    gap: 10px; 
  }
  
  .brand-text { 
    font-size: 26px; 
    font-weight: 900; 
    color: #0f172a; 
    letter-spacing: -1px; 
  }

  /* PC端：隐藏移动端搜索框类，使用新的布局 */
  .mobile-search {
    display: none;
  }

  /* PC端：重新创建三栏布局 */
  .nav-bar {
    display: flex;
    justify-content: space-between;
  }
  
  /* PC端：创建新的搜索框容器 */
  .search-box {
    display: flex;
    justify-content: center;
    align-items: center;
    flex: 2;
  }
  
  .search-box:not(.mobile-search) {
    max-width: 600px; 
    height: 52px; 
    background: #fff; 
    box-shadow: 0 8px 30px rgba(0,0,0,0.04);
    border-radius: 50px;
    padding: 0 16px;
    border: 1px solid rgba(255,255,255,0.8);
    transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  }
  
  .search-box:not(.mobile-search):focus-within {
    background: #fff;
    box-shadow: 0 8px 30px rgba(99, 102, 241, 0.15);
    transform: translateY(-1px) scale(1.01);
    border-color: #fff;
  }

  /* Hero 区域：极光文字 */
  .web-hero-section {
    display: flex; 
    justify-content: center;
    padding: 100px 20px 80px;
    position: relative; 
    z-index: 1;
    text-align: center;
  }
  
  .hero-title {
    font-size: 56px; 
    font-weight: 900; 
    color: #0f172a; 
    line-height: 1.1; 
    margin-bottom: 20px; 
    letter-spacing: -2px;
    animation: fadeUp 1s cubic-bezier(0.2, 0.8, 0.2, 1);
  }
  
  /* 渐变文字特效 */
  .gradient-text {
    background: linear-gradient(135deg, #6366f1 0%, #a855f7 50%, #ec4899 100%);
    background-clip: text;
    -webkit-background-clip: text; 
    -webkit-text-fill-color: transparent;
  }
  
  .hero-sub { 
    font-size: 18px; 
    color: #64748b; 
    font-weight: 400; 
    max-width: 600px; 
    line-height: 1.6; 
  }

  /* PC 瀑布流布局 */
  .waterfall-container { 
    height: auto; 
    max-width: 1280px; 
    margin: 0 auto; 
    overflow: visible; 
  }
  
  .waterfall-column { 
    column-count: 4; 
    column-gap: 24px; 
  }

  .post-card {
    background: #fff; 
    margin-bottom: 24px; 
    border-radius: 24px; 
    cursor: pointer;
    border: none; 
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
    transition: all 0.4s cubic-bezier(0.2, 0.8, 0.2, 1);
  }
  
  /* PC 悬停磁吸效果 */
  .post-card:hover { 
    transform: translateY(-12px); 
    box-shadow: 0 25px 50px -12px rgba(99, 102, 241, 0.25); 
  }
  
  .post-card:hover .shine-effect { 
    left: 100%; 
  }

  .waterfall-container div[style*="height: 60px"] { 
    display: none; 
  }
  
  /* PC端加号微调 */
  .add-btn-wrapper {
    width: 52px;
    height: 52px;
  }
  
  .add-btn-icon {
    font-size: 30px;
  }
  
  .plus-icon {
    font-size: 30px;
    transform: translateY(-1px);
  }
}
</style>
