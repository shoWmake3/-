<template>
  <div class="container">
    <div class="nav-bar">
      <div class="search-box">
        <text class="search-icon">🔍</text>
        <input class="search-input" v-model="keyword" placeholder="搜百科、找攻略..." confirm-type="search"
          @confirm="doSearch" />
      </div>
      <div class="add-btn-icon" @click="goToPublish">+</div>
    </div>

    <scroll-view scroll-y class="waterfall-container">
      <div class="waterfall-column">
        <div class="post-card" v-for="(item, index) in postList" :key="index" @click="goToDetail(item)">

          <view class="card-cover-wrapper">
            <video v-if="isVideo(getCoverMedia(item))" class="card-cover" :src="getCoverMedia(item)"
              :poster="getThumbnail(item)" :controls="false" :autoplay="true" :muted="true" :loop="true"
              :show-center-play-btn="false" object-fit="cover"></video>

            <image v-else class="card-cover" :src="getCoverMedia(item)" mode="widthFix" @error="handleImgError"></image>

            <view class="video-badge" v-if="isVideo(getCoverMedia(item))">▶</view>
          </view>

          <div class="card-content">
            <text class="card-title">{{ item.displayTitle }}</text>

            <div class="card-footer">
              <div class="author-box">
                <image class="mini-avatar" :src="item.authorAvatar || '/static/logo.png'" mode="aspectFill"></image>
                <text class="mini-name">{{ item.authorName }}</text>
              </div>
              <div class="like-box" @click.stop="handleLike(item)">
                <text :class="{ 'liked': item.isLiked }">❤️</text>
                <text class="like-num">{{ item.likeCount }}</text>
              </div>
            </div>

            <div class="trans-btn" @click.stop="handleTranslate(item)">
              <text>🌐 {{ item.isTranslated ? '原文' : '翻译' }}</text>
            </div>
          </div>
        </div>
      </div>

      <div v-if="postList.length === 0" class="empty-tip">
        加载中或暂无内容...
      </div>
      <div style="height: 20px;"></div>
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

// 获取封面图逻辑
const getCoverImage = (item) => {
  // 1. 如果字段为空，直接返回默认图
  if (!item.mediaUrls || item.mediaUrls === '[]') {
    return '/static/logo.png';
  }

  try {
    // 2. 尝试解析 JSON 字符串
    // 数据库存的是: ["http://.../a.jpg", "http://.../b.jpg"]
    const urls = JSON.parse(item.mediaUrls);

    // 3. 取第一张图
    if (Array.isArray(urls) && urls.length > 0) {
      return urls[0];
    }
  } catch (e) {
    // 解析失败（比如数据格式不对），返回默认图
    return '/static/logo.png';
  }

  return '/static/logo.png';
};

const handleTranslate = (item) => {
  // ... (保持原有的翻译逻辑不变) ...
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
  // ... (保持原有的点赞逻辑不变) ...
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

// 1. 判断是否是视频
const isVideo = (url) => {
  if (!url) return false;
  const lower = url.toLowerCase();
  // 加上常用的视频格式判断
  return lower.endsWith('.mp4') || lower.endsWith('.mov') || lower.endsWith('.webm') || lower.endsWith('.avi');
};

// 获取封面媒体（视频或图）
const getCoverMedia = (item) => {
  if (item.mediaUrls && item.mediaUrls !== '[]') {
    try {
      const urls = JSON.parse(item.mediaUrls);
      return urls[0]; // 返回第一个资源
    } catch (e) { return '/static/logo.png'; }
  }
  return '/static/logo.png';
};

// ⭐ 新增：获取缩略图（专门给 video 的 poster 使用）
const getThumbnail = (item) => {
  if (item.mediaUrls && item.mediaUrls !== '[]') {
    try {
      const urls = JSON.parse(item.mediaUrls);
      // 找数组里第一个以 .jpg/png/jpeg 结尾的文件
      const img = urls.find(url => {
        const u = url.toLowerCase();
        return u.endsWith('.jpg') || u.endsWith('.png') || u.endsWith('.jpeg') || u.endsWith('.webp');
      });
      return img || ''; // 如果没找到图片，返回空（video 会显示第一帧）
    } catch (e) { return ''; }
  }
  return '';
};

const handleImgError = (e) => {
  console.error("图片加载失败", e);
};
</script>

<style>
.container {
  background-color: #f2f4f6;
  min-height: 100vh;
}

/* 顶部栏 */
.nav-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 50px 15px 10px;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 99;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.search-box {
  flex: 1;
  background: #f5f5f5;
  height: 36px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  padding: 0 15px;
  margin-right: 15px;
}

.search-icon {
  font-size: 14px;
  margin-right: 5px;
  color: #999;
}

.search-input {
  flex: 1;
  font-size: 14px;
}

.add-btn-icon {
  width: 32px;
  height: 32px;
  background: #000;
  color: #fff;
  border-radius: 50%;
  text-align: center;
  line-height: 30px;
  font-size: 24px;
  font-weight: 300;
}

/* 1. 瀑布流容器 */
.waterfall-container {
  padding: 10px;
  /* 确保在 PC 端也能正常滚动 */
  height: calc(100vh - 100px);
  box-sizing: border-box;
}

/* 2. 核心布局：根据屏幕宽度自动切换列数 */
.waterfall-column {
  /* 默认：手机端双列 */
  column-count: 2;
  column-gap: 10px;

  /* 居中处理：防止 PC 端单列时太宽 */
  max-width: 100%;
  margin: 0 auto;
}

/* 3. 卡片基础样式 */
.post-card {
  break-inside: avoid;
  /* 防止卡片被分页断开 */
  background: #fff;
  border-radius: 12px;
  margin-bottom: 10px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
}

.post-card:active {
  transform: scale(0.98);
}

/* 封面媒体样式 */
.card-cover {
  width: 100%;
  display: block;
  /* 必须设置，否则没加载出来前高度为0 */
  min-height: 180px; 
  background: #333; 
}

.card-cover-wrapper {
  position: relative;
  width: 100%;
  background-color: #f0f0f0; /* 加载前的占位色 */
  overflow: hidden;
}

.card-content {
  padding: 8px 10px 12px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
  /* 限制最多显示2行，多余省略号 */
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  line-height: 1.4;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.author-box {
  display: flex;
  align-items: center;
  flex: 1;
  overflow: hidden;
}

.mini-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  margin-right: 6px;
  background: #eee;
}

.mini-name {
  font-size: 11px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.like-box {
  display: flex;
  align-items: center;
}

.like-num {
  font-size: 12px;
  color: #999;
  margin-left: 3px;
}

.liked {
  color: #ff2442;
}

.trans-btn {
  margin-top: 8px;
  background: #f0f8ff;
  color: #007aff;
  font-size: 10px;
  padding: 3px 6px;
  border-radius: 4px;
  text-align: center;
  width: fit-content;
}

.empty-tip {
  text-align: center;
  color: #999;
  margin-top: 50px;
  font-size: 14px;
}

/* ⭐ 媒体查询：当屏幕宽度大于 800px (通常是 PC 浏览器) */
@media screen and (min-width: 800px) {
  .waterfall-column {
    /* 切换为单列 */
    column-count: 1;
    /* 限制单列的最大宽度，防止在超宽屏上图片被拉得无限大 */
    max-width: 500px;
  }

  .post-card {
    /* PC 单列时，增加间距感 */
    margin-bottom: 20px;
  }
}
.video-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(0,0,0,0.4);
  color: #fff;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  z-index: 2;
}
</style>