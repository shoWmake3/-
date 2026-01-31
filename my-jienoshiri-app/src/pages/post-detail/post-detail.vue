<template>
  <view class="page-container">
    
    <scroll-view scroll-y class="scroll-area">
      <swiper class="media-swiper" :indicator-dots="mediaList.length > 1" :autoplay="false" circular>
        <swiper-item v-for="(url, index) in mediaList" :key="index">
          <view class="media-item">
            <video v-if="isVideo(url)" :src="url" class="full-media" controls object-fit="contain"></video>
            <image v-else :src="url" mode="aspectFit" class="full-media" @click="previewImage(url)"></image>
          </view>
        </swiper-item>
      </swiper>

      <view class="author-bar">
        <image class="mini-avatar" :src="post.authorAvatar || '/static/logo.png'" mode="aspectFill"></image>
        <view class="author-text">
          <text class="nickname">{{ post.authorName }}</text>
          <text class="identity">{{ post.authorIdentity === 'student' ? '留学生' : '居民' }}</text>
        </view>
        <button class="follow-btn">+ 关注</button>
      </view>

      <view class="main-content">
        <text class="title">{{ post.title }}</text>
        <text class="text-body">{{ post.content }}</text>
        <text class="date">发布于 {{ formatTime(post.createTime) }}</text>
        <view class="location-tag" v-if="post.locationName">📍 {{ post.locationName }}</view>
      </view>

      <view class="comment-section">
        <view class="section-title">共 {{ commentList.length }} 条评论</view>
        <view class="comment-item" v-for="(c, i) in commentList" :key="i">
          <image class="c-avatar" :src="c.avatar || '/static/logo.png'"></image>
          <view class="c-body">
            <text class="c-name">{{ c.nickname }}</text>
            <view class="c-content-row">
              <text class="c-text">{{ c.content }}</text>
              <text v-if="c.score > 0" class="c-score"> {{ c.score }}分</text>
            </view>
          </view>
        </view>
      </view>
      
      <view style="height: 120px;"></view>
    </scroll-view>

    <view class="bottom-fixed-area">
      <view class="star-row">
        <text class="star-label">打分：</text>
        <view class="star-box">
          <text 
            v-for="i in 5" 
            :key="i" 
            class="star-icon" 
            :class="{ active: i <= rating }" 
            @click="setRating(i)"
          >★</text>
        </view>
        <text class="score-text" v-if="rating > 0">{{ rating }}.0 分</text>
        <text class="score-text" v-else>未打分</text>
      </view>

      <view class="input-row">
        <input 
          class="comment-input" 
          v-model="newComment" 
          placeholder="写下你的评价..." 
          confirm-type="send" 
          @confirm="sendComment" 
        />
        
        <view class="send-btn" @click="sendComment">发布</view>
        
        <view class="action-icons" @click="handleLike" style="margin-left: 10px;">
          <text :style="{color: post.isLiked ? '#ff2442' : '#333'}">
            ❤️ {{ post.likeCount || 0 }}
          </text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { onLoad } from '@dcloudio/uni-app';

const post = ref({});
const mediaList = ref([]);
const commentList = ref([]);
const newComment = ref('');
const rating = ref(0); // 默认为0表示不打分

onLoad((options) => {
  // 从缓存或上个页面带过来的数据
  const data = uni.getStorageSync('currentPost');
  if (data) {
    post.value = data;
    // ⭐ 解析媒体数组
    if (data.mediaUrls && data.mediaUrls !== '[]') {
      try {
        mediaList.value = JSON.parse(data.mediaUrls);
      } catch (e) {
        mediaList.value = [data.mediaUrls]; // 兜底处理
      }
    }
    fetchComments(data.id);
  }
});

// 辅助：判断视频
const isVideo = (url) => {
  if (!url) return false;
  const lower = url.toLowerCase();
  return lower.endsWith('.mp4') || lower.endsWith('.mov') || lower.endsWith('.avi');
};

// 辅助：图片预览
const previewImage = (current) => {
  uni.previewImage({
    current: current,
    urls: mediaList.value.filter(url => !isVideo(url))
  });
};

const fetchComments = (postId) => {
  uni.request({
    url: `http://localhost:8080/post/comments?postId=${postId}`,
    success: (res) => { commentList.value = res.data; }
  });
};

const sendComment = () => {
  // ⭐ 优化校验逻辑：
  // 如果既没有写评论，也没有打分，才拦截。
  // 只要写了字，或者打了分，都可以发送。
  if (!newComment.value && rating.value === 0) {
    uni.showToast({ title: '请至少打个分或写句评论~', icon: 'none' });
    return;
  }

  const token = uni.getStorageSync('token');
  
  uni.request({
    url: 'http://localhost:8080/post/comment',
    method: 'POST',
    header: { 'Authorization': token },
    data: { 
        postId: post.value.id, 
        content: newComment.value || '', // 没写字就传空字符串
        score: rating.value 
    },
    success: () => {
      newComment.value = '';
      rating.value = 0; // 重置
      fetchComments(post.value.id);
      uni.showToast({ title: '评价成功' });
    }
  });
};

const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 16) : '';

const setRating = (val) => {
  rating.value = val;
};
</script>

<style>
/* 容器占满全屏，垂直布局 */
.page-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #fff;
  overflow: hidden; /* 防止整个页面出现双重滚动条 */
}

/* 中间滚动区：自动占据剩余高度 */
.scroll-area {
  flex: 1;
  height: 0; /* 配合 flex:1 使用，防止撑破 */
  width: 100%;
}

/* 轮播图 */
.media-swiper { width: 100%; height: 750rpx; background: #000; }
.media-item { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
.full-media { width: 100%; height: 100%; }

/* 作者栏 */
.author-bar { display: flex; align-items: center; padding: 10px 15px; border-bottom: 1px solid #f5f5f5; }
.mini-avatar { width: 35px; height: 35px; border-radius: 50%; margin-right: 10px; }
.author-text { flex: 1; display: flex; flex-direction: column; }
.nickname { font-size: 14px; font-weight: bold; }
.identity { font-size: 11px; color: #999; }
.follow-btn { font-size: 12px; background: #ff2442; color: #fff; border-radius: 20px; padding: 0 15px; height: 28px; line-height: 28px; }

/* 正文 */
.main-content { padding: 15px; }
.title { font-size: 18px; font-weight: bold; margin-bottom: 10px; display: block; }
.text-body { font-size: 15px; color: #333; line-height: 1.6; }
.date { font-size: 12px; color: #ccc; margin-top: 15px; display: block; }
.location-tag { margin-top: 10px; font-size: 12px; color: #007aff; background: #f0f7ff; width: fit-content; padding: 2px 8px; border-radius: 4px; }

/* 评论区 */
.comment-section { padding: 15px; border-top: 10px solid #f5f5f5; }
.section-title { font-size: 14px; font-weight: bold; margin-bottom: 15px; }
.comment-item { display: flex; margin-bottom: 15px; }
.c-avatar { width: 30px; height: 30px; border-radius: 50%; margin-right: 10px; }
.c-body { flex: 1; display: flex; flex-direction: column; }
.c-name { font-size: 12px; color: #999; margin-bottom: 4px; }
.c-text { font-size: 14px; color: #333; }
.c-score { font-size: 12px; color: #ffca3e; margin-left: 5px; }

/* ⭐ 底部区域：不再使用 fixed，而是作为 Flex 的一部分 */
.bottom-fixed-area {
  background: #fff;
  border-top: 1px solid #eee;
  padding: 10px 15px;
  /* 适配 iPhone 底部黑条，如果不生效可以加 padding-bottom: constant(safe-area-inset-bottom); */
  padding-bottom: 30px; 
  display: flex;
  flex-direction: column;
  z-index: 100;
}

/* 星星行 */
.star-row { display: flex; align-items: center; margin-bottom: 10px; }
.star-label { font-size: 14px; color: #666; margin-right: 10px; }
.star-icon { font-size: 24px; color: #e0e0e0; margin-right: 8px; }
.star-icon.active { color: #ffca3e; }
.score-text { font-size: 14px; color: #ffca3e; font-weight: bold; margin-left: 10px; }

/* 输入行调整 */
.input-row { 
  display: flex; 
  align-items: center; 
  width: 100%;
}

.comment-input { 
  flex: 1; 
  background: #f5f5f5; 
  height: 36px; 
  border-radius: 18px; 
  padding: 0 15px; 
  font-size: 14px; 
  margin-right: 10px; /* 稍微缩小间距 */
}

/* ⭐ 新增按钮样式 */
.send-btn {
  background-color: #007aff; /* 蓝色按钮 */
  color: #fff;
  font-size: 14px;
  padding: 6px 15px;
  border-radius: 20px;
  margin-right: 5px;
  /* 增加点击效果 */
  transition: opacity 0.2s;
}
.send-btn:active {
  opacity: 0.8;
}

.action-icons { 
  font-size: 16px; 
  display: flex;
  align-items: center;
}
</style>