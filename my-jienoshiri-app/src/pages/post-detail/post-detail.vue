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
        <image class="mini-avatar" :src="post.authorAvatar || '/static/logo.png'" mode="aspectFill"
          @click.stop="goToChat(post.userId, post.authorName, post.authorAvatar)"></image>
        <view class="author-text">
          <text class="nickname">{{ post.authorName }}</text>
          <text class="identity">{{ getIdentityName(post.authorIdentity) }}</text>
        </view>
        <button class="follow-btn">+ 关注</button>
      </view>

      <view class="main-content">
        <text class="title">{{ post.title }}</text>
        
        <view class="wiki-convert-btn" @click="handleConvertToWiki">
          📖 收录为百科词条
        </view>

        <text class="text-body">{{ post.content }}</text>
        
        <view class="post-meta-row">
          <text class="date">发布于 {{ formatTime(post.createTime) }}</text>
          <view class="inner-like-btn" :class="{ active: post.isLiked }" @click="handleLike">
             <text>{{ post.isLiked ? '❤️ 已赞' : '🤍 点赞' }} {{ post.likeCount || 0 }}</text>
          </view>
        </view>

        <view class="location-tag" v-if="post.locationName">📍 {{ post.locationName }}</view>
      </view>

      <view class="comment-section">
        <view class="section-title">共 {{ commentList.length }} 条评论</view>
        <view class="comment-item" v-for="(c, i) in commentList" :key="i">
          <image class="c-avatar" :src="c.avatar || '/static/logo.png'" 
             @click.stop="goToChat(c.userId, c.nickname, c.avatar)"></image>
          <view class="c-body">
            <view class="c-header">
              <text class="c-name">{{ c.nickname }}</text>
              <text class="c-badge" v-if="c.reputation !== undefined">
                 {{ getBadgeIcon(c.reputation) }}
              </text>
              <text class="c-identity" :class="c.identityType">{{ getIdentityName(c.identityType) }}</text>
            </view>
            <view class="c-star-row" v-if="c.score > 0">
              <text class="c-stars">{{ '★'.repeat(Math.round(c.score)) }}</text>
              <text class="c-score-num">{{ c.score }}分</text>
            </view>
            <text class="c-text">{{ c.content }}</text>
          </view>
        </view>
      </view>

      <view style="height: 120px;"></view>
    </scroll-view>

    <view class="bottom-fixed-area">
      <view class="star-row">
        <text class="star-label">打分：</text>
        <view class="star-box">
          <text v-for="i in 5" :key="i" class="star-icon" :class="{ active: i <= rating }"
            @click="setRating(i)">★</text>
        </view>
        <text class="score-text" v-if="rating > 0">{{ rating }}.0 分</text>
        <text class="score-text" v-else>未打分</text>
      </view>

      <view class="input-row">
        <input class="comment-input" v-model="newComment" placeholder="写下你的评价..." confirm-type="send"
          @confirm="sendComment" />
        <view class="send-btn" @click="sendComment">发布</view>
        <view class="action-icons" @click="handleLike" style="margin-left: 10px;">
          <text :style="{ color: post.isLiked ? '#ff2442' : '#333' }">
            ❤️ {{ post.likeCount || 0 }}
          </text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';

const post = ref({});
const mediaList = ref([]);
const commentList = ref([]);
const newComment = ref('');
const rating = ref(0);

onLoad((options) => {
  const data = uni.getStorageSync('currentPost');
  if (data) {
    post.value = data;
    if (data.mediaUrls && data.mediaUrls !== '[]') {
      try {
        mediaList.value = JSON.parse(data.mediaUrls);
      } catch (e) {
        mediaList.value = [data.mediaUrls];
      }
    }
    fetchComments(data.id);
  }
});

// ⭐ 新增：处理转维基逻辑
const handleConvertToWiki = () => {
  const token = uni.getStorageSync('token');
  if (!token) return uni.showToast({ title: '请先登录', icon: 'none' });

  // 1. 让用户选择分类
  const categories = ['签证办理', '租房攻略', '交通出行', '学术研究', '打工兼职'];
  uni.showActionSheet({
    itemList: categories,
    success: (res) => {
      const selectedCat = categories[res.tapIndex];
      
      // 2. 调用后端接口
      uni.showLoading({ title: '收录中...' });
      uni.request({
        url: 'http://localhost:8080/wiki/convert',
        method: 'POST',
        header: { 
            'Authorization': token,
            'Content-Type': 'application/x-www-form-urlencoded' // 注意表单提交
        },
        data: {
            postId: post.value.id,
            category: selectedCat
        },
        success: (apiRes) => {
            uni.hideLoading();
            if (apiRes.statusCode === 200) {
                uni.showToast({ title: '收录成功！', icon: 'success' });
                // 3. 询问是否去查看
                setTimeout(() => {
                    uni.showModal({
                        title: '提示',
                        content: '词条已创建，是否前往维基百科查看？',
                        success: (modalRes) => {
                            if (modalRes.confirm) {
                                uni.navigateTo({ url: '/pages/wiki/wiki' });
                            }
                        }
                    });
                }, 1000);
            } else {
                uni.showToast({ title: '收录失败', icon: 'none' });
            }
        },
        fail: () => { uni.hideLoading(); uni.showToast({ title: '网络错误', icon: 'none' }); }
      });
    }
  });
};

const goToChat = (targetId, targetName, avatarUrl) => {
  const token = uni.getStorageSync('token');
  if (!token) { uni.navigateTo({ url: '/pages/login/login' }); return; }
  const me = uni.getStorageSync('user');
  if (me && me.id == targetId) { uni.showToast({ title: '不能给自己发私信哦', icon: 'none' }); return; }
  const safeAvatar = encodeURIComponent(avatarUrl || '');
  uni.navigateTo({ url: `/pages/chat/chat?targetId=${targetId}&name=${targetName || '用户'}&avatar=${safeAvatar}` });
};

const getBadgeIcon = (score) => {
  score = score || 0;
  if (score < 0) return '⚠️';
  if (score < 100) return '🌱';
  if (score < 300) return '🎓';
  return '👑';
};
const getIdentityName = (type) => { const map = { 'student': '留学生', 'agent': '中介', 'worker': '打工人', 'tourist': '游客' }; return map[type] || '用户'; };
const isVideo = (url) => { if (!url) return false; const lower = url.toLowerCase(); return lower.endsWith('.mp4') || lower.endsWith('.mov') || lower.endsWith('.avi'); };
const previewImage = (current) => { uni.previewImage({ current: current, urls: mediaList.value.filter(url => !isVideo(url)) }); };
const fetchComments = (postId) => { uni.request({ url: `http://localhost:8080/post/comments?postId=${postId}`, success: (res) => { commentList.value = res.data; } }); };
const sendComment = () => {
  if (!newComment.value && rating.value === 0) { uni.showToast({ title: '请至少打个分或写句评论~', icon: 'none' }); return; }
  const token = uni.getStorageSync('token');
  uni.request({
    url: 'http://localhost:8080/post/comment',
    method: 'POST',
    header: { 'Authorization': token },
    data: { postId: post.value.id, content: newComment.value || '', score: rating.value },
    success: () => { newComment.value = ''; rating.value = 0; fetchComments(post.value.id); uni.showToast({ title: '评价成功' }); }
  });
};
const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 16) : '';
const setRating = (val) => { rating.value = val; };
const handleLike = () => { post.value.likeCount++; post.value.isLiked = true; };
</script>

<style>
/* 保持原有样式... */
.page-container { display: flex; flex-direction: column; height: 100vh; background-color: #fff; overflow: hidden; }
.scroll-area { flex: 1; height: 0; width: 100%; }
.media-swiper { width: 100%; height: 750rpx; background: #000; }
.media-item { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
.full-media { width: 100%; height: 100%; }
.author-bar { display: flex; align-items: center; padding: 10px 15px; border-bottom: 1px solid #f5f5f5; }
.mini-avatar { width: 35px; height: 35px; border-radius: 50%; margin-right: 10px; }
.author-text { flex: 1; display: flex; flex-direction: column; }
.nickname { font-size: 14px; font-weight: bold; }
.identity { font-size: 11px; color: #999; }
.follow-btn { font-size: 12px; background: #ff2442; color: #fff; border-radius: 20px; padding: 0 15px; height: 28px; line-height: 28px; }
.main-content { padding: 15px; }
.title { font-size: 18px; font-weight: bold; margin-bottom: 10px; display: block; }
.text-body { font-size: 15px; color: #333; line-height: 1.6; }

/* ⭐ 新增按钮样式 */
.wiki-convert-btn {
    background: #eef6ff;
    color: #007aff;
    font-size: 12px;
    padding: 8px 12px;
    border-radius: 6px;
    margin-bottom: 15px;
    text-align: center;
    font-weight: bold;
    border: 1px dashed #007aff;
}
.wiki-convert-btn:active { background: #d0e4ff; }

.post-meta-row { display: flex; justify-content: space-between; align-items: center; margin-top: 15px; }
.date { font-size: 12px; color: #ccc; }
.inner-like-btn { background: #f5f5f5; padding: 4px 12px; border-radius: 20px; font-size: 12px; color: #666; transition: all 0.2s; }
.inner-like-btn.active { background: #ffebeb; color: #ff2442; }
.location-tag { margin-top: 10px; font-size: 12px; color: #007aff; background: #f0f7ff; width: fit-content; padding: 2px 8px; border-radius: 4px; }
.comment-section { padding: 15px; border-top: 10px solid #f5f5f5; }
.section-title { font-size: 14px; font-weight: bold; margin-bottom: 15px; }
.comment-item { display: flex; margin-bottom: 15px; }
.c-avatar { width: 30px; height: 30px; border-radius: 50%; margin-right: 10px; }
.c-body { flex: 1; display: flex; flex-direction: column; }
.c-name { font-size: 12px; color: #999; margin-bottom: 4px; }
.c-text { font-size: 14px; color: #333; }
.bottom-fixed-area { background: #fff; border-top: 1px solid #eee; padding: 10px 15px; padding-bottom: 30px; display: flex; flex-direction: column; z-index: 100; }
.star-row { display: flex; align-items: center; margin-bottom: 10px; }
.star-label { font-size: 14px; color: #666; margin-right: 10px; }
.star-icon { font-size: 24px; color: #e0e0e0; margin-right: 8px; }
.star-icon.active { color: #ffca3e; }
.score-text { font-size: 14px; color: #ffca3e; font-weight: bold; margin-left: 10px; }
.input-row { display: flex; align-items: center; width: 100%; }
.comment-input { flex: 1; background: #f5f5f5; height: 36px; border-radius: 18px; padding: 0 15px; font-size: 14px; margin-right: 10px; }
.send-btn { background-color: #007aff; color: #fff; font-size: 14px; padding: 6px 15px; border-radius: 20px; margin-right: 5px; transition: opacity 0.2s; }
.send-btn:active { opacity: 0.8; }
.action-icons { font-size: 16px; display: flex; align-items: center; }
.c-header { display: flex; align-items: center; margin-bottom: 4px; }
.c-badge { font-size: 10px; margin-left: 5px; margin-right: 5px; }
.c-identity { font-size: 10px; padding: 1px 4px; border-radius: 4px; background: #f0f0f0; color: #999; }
.c-identity.student { background: #e3f2fd; color: #007aff; }
.c-star-row { display: flex; align-items: center; margin-bottom: 4px; }
.c-stars { color: #ffca3e; font-size: 12px; letter-spacing: 1px; }
.c-score-num { font-size: 11px; color: #999; margin-left: 5px; }
</style>