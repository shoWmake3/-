<template>
  <view class="container">
    <swiper class="media-swiper" :indicator-dots="mediaList.length > 1" :autoplay="false" circular>
      <swiper-item v-for="(url, index) in mediaList" :key="index">
        <view class="media-item">
          <video 
            v-if="isVideo(url)" 
            :src="url" 
            class="full-media" 
            controls 
            object-fit="contain"
          ></video>
          <image 
            v-else 
            :src="url" 
            mode="aspectFit" 
            class="full-media" 
            @click="previewImage(url)"
          ></image>
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

    <scroll-view scroll-y class="content-scroll">
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
            <text class="c-text">{{ c.content }}</text>
          </view>
        </view>
      </view>
      <view style="height: 100px;"></view>
    </scroll-view>

    <view class="bottom-bar">
      <input class="comment-input" v-model="newComment" placeholder="说点什么吧..." confirm-type="send" @confirm="sendComment" />
      <view class="action-icons">
        <text @click="handleLike" :style="{color: post.isLiked ? 'red' : '#333'}">❤️ {{ post.likeCount }}</text>
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
  if (!newComment.value) return;
  const token = uni.getStorageSync('token');
  uni.request({
    url: 'http://localhost:8080/post/comment',
    method: 'POST',
    header: { 'Authorization': token },
    data: { postId: post.value.id, content: newComment.value },
    success: () => {
      newComment.value = '';
      fetchComments(post.value.id);
    }
  });
};

const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 16) : '';
</script>

<style>
.container { display: flex; flex-direction: column; height: 100vh; background: #fff; }

/* 轮播图高度固定，适合展示 */
.media-swiper { width: 100%; height: 750rpx; background: #000; }
.media-item { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
.full-media { width: 100%; height: 100%; }

.author-bar { display: flex; align-items: center; padding: 10px 15px; border-bottom: 1px solid #f5f5f5; }
.mini-avatar { width: 35px; height: 35px; border-radius: 50%; margin-right: 10px; }
.author-text { flex: 1; display: flex; flex-direction: column; }
.nickname { font-size: 14px; font-weight: bold; }
.identity { font-size: 11px; color: #999; }
.follow-btn { font-size: 12px; background: #ff2442; color: #fff; border-radius: 20px; padding: 0 15px; height: 28px; line-height: 28px; }

.content-scroll { flex: 1; overflow: hidden; }
.main-content { padding: 15px; }
.title { font-size: 18px; font-weight: bold; margin-bottom: 10px; display: block; }
.text-body { font-size: 15px; color: #333; line-height: 1.6; }
.date { font-size: 12px; color: #ccc; margin-top: 15px; display: block; }
.location-tag { margin-top: 10px; font-size: 12px; color: #007aff; background: #f0f7ff; width: fit-content; padding: 2px 8px; border-radius: 4px; }

.comment-section { padding: 15px; border-top: 10px solid #f5f5f5; }
.section-title { font-size: 14px; font-weight: bold; margin-bottom: 15px; }
.comment-item { display: flex; margin-bottom: 15px; }
.c-avatar { width: 30px; height: 30px; border-radius: 50%; margin-right: 10px; }
.c-body { flex: 1; display: flex; flex-direction: column; }
.c-name { font-size: 12px; color: #999; margin-bottom: 4px; }
.c-text { font-size: 14px; color: #333; }

.bottom-bar { position: fixed; bottom: 0; width: 100%; background: #fff; border-top: 1px solid #eee; padding: 10px 15px; display: flex; align-items: center; padding-bottom: 30px; }
.comment-input { flex: 1; background: #f5f5f5; height: 36px; border-radius: 18px; padding: 0 15px; font-size: 14px; margin-right: 15px; }
.action-icons { font-size: 16px; font-weight: bold; }
</style>