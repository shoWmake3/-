<template>
  <view class="page-container" @click="closeRating">
    
    <div class="pc-ambient-bg">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
      <div class="noise-overlay"></div>
    </div>

    <scroll-view scroll-y class="scroll-area" show-scrollbar="true">
      <view class="detail-card" @click.stop="closeRating">
        
        <view class="media-wrapper">
          <swiper class="media-swiper" :indicator-dots="mediaList.length > 1" 
            indicator-active-color="#ffffff" indicator-color="rgba(255,255,255,0.5)" 
            :autoplay="false" circular>
            <swiper-item v-for="(url, index) in mediaList" :key="index">
              <view class="media-item">
                <video v-if="isVideo(url)" :src="url" class="full-media" controls object-fit="contain"></video>
                <image v-else :src="url" mode="aspectFill" class="full-media" @click="previewImage(url)"></image>
              </view>
            </swiper-item>
          </swiper>
          <view class="mobile-curve-mask"></view>
        </view>

        <view class="content-body">
          <view class="author-header">
            <view class="author-left" @click.stop="goToChat(post.userId, post.authorName, post.authorAvatar)">
              <view class="avatar-ring">
                <image class="mini-avatar" :src="post.authorAvatar || '/static/logo.png'" mode="aspectFill"></image>
              </view>
              <view class="author-info">
                <text class="nickname">{{ post.authorName }}</text>
                <view class="identity-badge" :class="post.authorIdentity">
                  {{ getIdentityName(post.authorIdentity) }}
                </view>
              </view>
            </view>
            <button class="follow-btn">关注</button>
          </view>

          <view class="main-text-area">
            <text class="post-title">{{ post.title }}</text>

            <view class="wiki-card" @click="handleConvertToWiki">
              <view class="wiki-icon-box">📖</view>
              <view class="wiki-text-col">
                <text class="wiki-main-text">收录至维基百科</text>
                <text class="wiki-sub-text">将此优质内容沉淀为公共知识</text>
              </view>
              <view class="wiki-arrow">→</view>
            </view>

            <text class="text-content">{{ post.content }}</text>

            <view class="meta-footer">
              <text class="date-text">{{ formatTime(post.createTime) }}</text>
              <view class="location-pill" v-if="post.locationName">
                <text class="loc-icon">📍</text>
                <text>{{ post.locationName }}</text>
              </view>
            </view>
          </view>

          <view class="divider-line"></view>

          <view class="comment-section">
            <view class="section-header">
              <text class="section-title">全部评论</text>
              <text class="comment-count">({{ commentList.length }})</text>
            </view>
            
            <view class="comment-list">
              <view class="comment-item" v-for="(c, i) in commentList" :key="i">
                <image class="c-avatar" :src="c.avatar || '/static/logo.png'" 
                  @click.stop="goToChat(c.userId, c.nickname, c.avatar)"></image>
                
                <view class="c-content-box">
                  <view class="c-top-row">
                    <text class="c-name">{{ c.nickname }}</text>
                    <view class="c-tags">
                      <text class="c-badge" v-if="c.reputation >= 100">{{ getBadgeIcon(c.reputation) }}</text>
                      <text class="c-role">{{ getIdentityName(c.identityType) }}</text>
                    </view>
                  </view>
                  
                  <view class="c-rating-row" v-if="c.score > 0">
                    <text class="c-stars">{{ '★'.repeat(Math.round(c.score)) }}</text>
                  </view>
                  
                  <text class="c-text">{{ c.content }}</text>
                </view>
              </view>
            </view>
            
            <view v-if="commentList.length === 0" class="empty-comment">
              <text>还没有评论，快来抢沙发~</text>
            </view>
          </view>

          <view class="mobile-bottom-spacer"></view>
        </view>
      </view>
    </scroll-view>

    <view class="glass-bottom-bar" @click.stop>
      <view class="rating-floater" v-if="rating > 0">
        <text class="rating-val">{{ rating }}.0</text>
        <text class="rating-label">评分</text>
      </view>

      <view class="action-container">
        <view class="input-wrapper">
          <text class="edit-icon">✎</text>
          <input class="comment-input" v-model="newComment" placeholder="说点什么..." confirm-type="send"
            @confirm="sendComment" placeholder-style="color:#94a3b8" />
        </view>

        <view class="icon-btn star-trigger" @click.stop="toggleRating">
          <view class="star-popover" :class="{ 'show': isRatingOpen }" @click.stop>
            <text v-for="i in 5" :key="i" class="pop-star" :class="{ active: i <= rating }"
              @click.stop="setRating(i)">★</text>
          </view>
          <text class="btn-emoji">⭐</text>
        </view>

        <view class="icon-btn like-btn" :class="{ liked: post.isLiked }" @click.stop="handleLike">
          <text class="btn-emoji">{{ post.isLiked ? '❤️' : '🤍' }}</text>
          <text class="btn-num" v-if="post.likeCount">{{ post.likeCount }}</text>
        </view>
        
        <view class="send-circle" v-if="newComment || rating > 0" @click.stop="sendComment">
          <text>↑</text>
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
const isRatingOpen = ref(false); // 星星开关

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

const goBack = () => { uni.navigateBack(); };

// ⭐ 交互修复：点击开关评分
const toggleRating = () => { isRatingOpen.value = !isRatingOpen.value; };
const closeRating = () => { isRatingOpen.value = false; };
const setRating = (val) => {
  rating.value = val;
  setTimeout(() => { isRatingOpen.value = false; }, 300);
};

// ⭐ 交互修复：点赞
const handleLike = () => {
  const token = uni.getStorageSync('token');
  if (!token) return uni.showToast({ title: '请先登录', icon: 'none' });
  uni.request({
    url: `http://localhost:8080/post/like?postId=${post.value.id}`,
    method: 'POST',
    header: { 'Authorization': token },
    success: (res) => {
      // 适配后端返回 "点赞成功" / "取消点赞"
      if (res.data === '点赞成功') {
        post.value.isLiked = true;
        post.value.likeCount++;
        uni.vibrateShort();
      } else {
        post.value.isLiked = false;
        post.value.likeCount = Math.max(0, post.value.likeCount - 1);
      }
    }
  });
};

const handleConvertToWiki = () => {
  const token = uni.getStorageSync('token');
  if (!token) return uni.showToast({ title: '请先登录', icon: 'none' });

  const categories = ['签证办理', '租房攻略', '交通出行', '学术研究', '打工兼职'];
  uni.showActionSheet({
    itemList: categories,
    success: (res) => {
      const selectedCat = categories[res.tapIndex];
      uni.showLoading({ title: '正在收录...' });
      uni.request({
        url: 'http://localhost:8080/wiki/convert',
        method: 'POST',
        header: { 'Authorization': token, 'Content-Type': 'application/x-www-form-urlencoded' },
        data: { postId: post.value.id, category: selectedCat },
        success: (apiRes) => {
    uni.hideLoading();
    if (apiRes.statusCode === 200) {
        uni.showToast({ title: '收录成功', icon: 'success' });
        setTimeout(() => {
            uni.showModal({
                title: '📚 知识库更新',
                content: '等待审核后，您的内容将被收录至维基百科，供更多用户查阅。',
                success: (modalRes) => {
                    if (modalRes.confirm) {
                        // ⭐ 关键修复：将 navigateTo 改为 switchTab
                        uni.switchTab({ 
                            url: '/pages/wiki/wiki' 
                        });
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

const getBadgeIcon = (score) => { score = score || 0; if (score < 0) return '⚠️'; if (score < 100) return '🌱'; if (score < 300) return '🎓'; return '👑'; };
const getIdentityName = (type) => { const map = { 'student': '留学生', 'agent': '中介', 'worker': '打工人', 'tourist': '游客' }; return map[type] || '用户'; };
const isVideo = (url) => { if (!url) return false; const lower = url.toLowerCase(); return lower.endsWith('.mp4') || lower.endsWith('.mov') || lower.endsWith('.avi'); };
const previewImage = (current) => { uni.previewImage({ current: current, urls: mediaList.value.filter(url => !isVideo(url)) }); };
const fetchComments = (postId) => { uni.request({ url: `http://localhost:8080/post/comments?postId=${postId}`, success: (res) => { commentList.value = res.data; } }); };
const sendComment = () => {
  if (!newComment.value && rating.value === 0) { uni.showToast({ title: '写点什么吧~', icon: 'none' }); return; }
  const token = uni.getStorageSync('token');
  uni.request({
    url: 'http://localhost:8080/post/comment',
    method: 'POST',
    header: { 'Authorization': token },
    data: { postId: post.value.id, content: newComment.value || '', score: rating.value },
    success: () => { newComment.value = ''; rating.value = 0; fetchComments(post.value.id); uni.showToast({ title: '已发送' }); }
  });
};
const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 16) : '';
</script>

<style>
:root {
  --primary: #6366f1;
  --bg-page: #ffffff;
  --text-main: #0f172a;
  --text-sub: #64748b;
}

/* =================================================================
   📱 1. 手机端样式 (默认) - 沉浸式流体布局
   ================================================================= */

/* 页面容器 */
.page-container {
  display: flex; flex-direction: column; height: 100vh;
  background-color: var(--bg-page); position: relative;
}

/* 手机端隐藏 PC 元素 */
.pc-ambient-bg { display: none; }
.pc-back-btn { display: none; }

/* 滚动区域 */
.scroll-area { flex: 1; height: 0; }

/* Detail Card (手机端占满) */
.detail-card { width: 100%; background: #fff; }

/* 媒体区 */
.media-wrapper { position: relative; height: 500rpx; }
.media-swiper { width: 100%; height: 100%; }
.media-item, .full-media { width: 100%; height: 100%; }

/* 手机端专属弧形遮罩 (Mobile Curve) */
.mobile-curve-mask {
  position: absolute; bottom: -1px; left: 0; width: 100%; height: 24px;
  background: #fff; border-radius: 24px 24px 0 0; z-index: 10;
}

/* 内容区 */
.content-body { padding: 0 20px; position: relative; z-index: 20; }

.author-header { display: flex; justify-content: space-between; align-items: center; padding: 10px 0 20px; }
.author-left { display: flex; align-items: center; gap: 12px; }
.avatar-ring { padding: 2px; border: 1px solid #e2e8f0; border-radius: 50%; }
.mini-avatar { width: 40px; height: 40px; border-radius: 50%; display: block; }
.author-info { display: flex; flex-direction: column; }
.nickname { font-size: 16px; font-weight: 700; color: var(--text-main); margin-bottom: 2px; }
.identity-badge { font-size: 10px; background: #f1f5f9; color: var(--text-sub); padding: 2px 6px; border-radius: 4px; width: fit-content; }
.follow-btn { font-size: 13px; font-weight: 600; color: var(--primary); background: rgba(99, 102, 241, 0.1); padding: 6px 18px; border-radius: 20px; margin: 0; }

.main-text-area { margin-bottom: 30px; }
.post-title { font-size: 22px; font-weight: 800; color: var(--text-main); line-height: 1.4; margin-bottom: 16px; display: block; }
.text-content { font-size: 16px; color: #334155; line-height: 1.8; letter-spacing: 0.01em; white-space: pre-wrap; }

/* Wiki Card */
.wiki-card { background: linear-gradient(135deg, #1e293b 0%, #334155 100%); border-radius: 12px; padding: 12px 16px; display: flex; align-items: center; gap: 12px; margin-bottom: 24px; box-shadow: 0 8px 20px rgba(30, 41, 59, 0.15); color: #fff; transition: transform 0.2s; }
.wiki-card:active { transform: scale(0.98); }
.wiki-icon-box { width: 32px; height: 32px; background: rgba(255,255,255,0.1); border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 18px; }
.wiki-text-col { flex: 1; display: flex; flex-direction: column; }
.wiki-main-text { font-size: 14px; font-weight: 600; color: #f8fafc; }
.wiki-sub-text { font-size: 11px; color: #94a3b8; margin-top: 2px; }
.wiki-arrow { font-size: 14px; color: #94a3b8; }

.meta-footer { margin-top: 30px; display: flex; justify-content: space-between; align-items: center; }
.date-text { font-size: 12px; color: #94a3b8; }
.location-pill { display: flex; align-items: center; gap: 4px; font-size: 12px; color: var(--primary); background: rgba(99, 102, 241, 0.05); padding: 4px 10px; border-radius: 20px; }
.divider-line { height: 1px; background: #f1f5f9; margin: 0 20px; }

/* 评论 */
.comment-section { padding: 20px 0; }
.section-header { margin-bottom: 20px; display: flex; align-items: center; gap: 6px; }
.section-title { font-size: 16px; font-weight: 700; color: var(--text-main); }
.comment-count { font-size: 14px; color: #94a3b8; }
.comment-item { display: flex; gap: 12px; margin-bottom: 20px; }
.c-avatar { width: 36px; height: 36px; border-radius: 50%; flex-shrink: 0; background: #f1f5f9; }
.c-content-box { flex: 1; }
.c-top-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.c-name { font-size: 13px; font-weight: 600; color: #64748b; }
.c-tags { display: flex; align-items: center; gap: 6px; }
.c-role { font-size: 10px; color: #94a3b8; background: #f8fafc; padding: 1px 4px; border-radius: 4px; border: 1px solid #e2e8f0; }
.c-rating-row { margin-bottom: 6px; }
.c-stars { color: #f59e0b; font-size: 10px; letter-spacing: 1px; }
.c-text { font-size: 14px; color: #1e293b; line-height: 1.5; }
.empty-comment { text-align: center; color: #cbd5e1; font-size: 13px; padding: 20px 0; }
.mobile-bottom-spacer { height: 140px; }

/* 底部操作栏 (手机端：通栏) */
.glass-bottom-bar {
  position: fixed; bottom: 30px; left: 20px; right: 20px; z-index: 100;
  display: flex; flex-direction: column; align-items: flex-end; 
  pointer-events: none; /* 穿透，只响应子元素 */
}

.action-container {
  width: 100%; height: 56px; background: rgba(255, 255, 255, 0.9); backdrop-filter: blur(20px);
  border-radius: 30px; box-shadow: 0 10px 40px -10px rgba(0,0,0,0.1); border: 1px solid rgba(255,255,255,0.5);
  display: flex; align-items: center; padding: 0 6px 0 16px; box-sizing: border-box; 
  pointer-events: auto; /* 容器可点 */
}

.input-wrapper { flex: 1; display: flex; align-items: center; gap: 8px; }
.edit-icon { color: #94a3b8; font-size: 16px; }
.comment-input { flex: 1; font-size: 14px; height: 100%; color: var(--text-main); }
.icon-btn { width: 44px; height: 44px; display: flex; flex-direction: column; align-items: center; justify-content: center; border-radius: 50%; position: relative; }
.btn-emoji { font-size: 20px; line-height: 1; }
.btn-num { font-size: 9px; color: #64748b; position: absolute; bottom: 4px; }
.send-circle { width: 44px; height: 44px; background: var(--text-main); border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #fff; font-weight: 800; font-size: 18px; margin-left: 4px; animation: scaleIn 0.2s; }
@keyframes scaleIn { from { transform: scale(0); } to { transform: scale(1); } }

/* 评分气泡 (复用) */
.rating-floater {
  background: rgba(0,0,0,0.8); backdrop-filter: blur(10px); padding: 6px 12px; border-radius: 20px;
  display: flex; align-items: center; gap: 6px; margin-bottom: 10px; pointer-events: auto; animation: popIn 0.3s;
}
.rating-val { color: #f59e0b; font-weight: bold; font-size: 14px; }
.rating-label { color: #fff; font-size: 10px; opacity: 0.8; }
@keyframes popIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

/* 关键：评分弹窗控制 */
.star-popover {
  position: absolute; bottom: 100%; left: 50%; transform: translateX(-50%) translateY(10px); 
  background: #2079b4; padding: 8px 12px; border-radius: 30px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); 
  display: flex; gap: 4px; margin-bottom: 10px; z-index: 1001;
  opacity: 0; pointer-events: none; transition: all 0.2s;
}
.star-popover.show { opacity: 1; pointer-events: auto !important; transform: translateX(-50%) translateY(0); }
.pop-star { font-size: 22px; color: #e2e8f0; transition: color 0.2s; padding: 4px; }
.pop-star.active { color: #f59e0b; }

/* =================================================================
   💻 2. PC 端专属样式 (Media Query) - 绝对分离，绝对居中
   ================================================================= */
@media screen and (min-width: 960px) {
  
  /* --- 2.1 容器重置：强制块级，接管滚动 --- */
  .page-container {
    height: 100vh;
    overflow-y: auto; /* 使用原生滚动 */
    display: block;   /* 取消flex */
    position: relative;
  }

  /* --- 2.2 显示 PC 背景 --- */
  .pc-ambient-bg {
    display: block; position: fixed; top: 0; left: 0; width: 100%; height: 100%; z-index: 0; pointer-events: none; background: #f1f5f9;
  }
  .noise-overlay { position: absolute; top: 0; left: 0; width: 100%; height: 100%; opacity: 0.03; background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E"); }
  .blob { position: absolute; filter: blur(90px); opacity: 0.6; animation: float 12s infinite alternate; }
  .blob-1 { top: -10%; left: -10%; width: 50vw; height: 50vw; background: #c4b5fd; }
  .blob-2 { bottom: -10%; right: -10%; width: 60vw; height: 60vw; background: #a5f3fc; animation-delay: -3s; }
  .blob-3 { top: 40%; left: 30%; width: 40vw; height: 40vw; background: #fecaca; opacity: 0.4; animation-delay: -5s; }
  @keyframes float { 0% { transform: translate(0, 0); } 100% { transform: translate(40px, 60px); } }

  /* --- 2.3 显示返回按钮 --- */
  .pc-back-btn {
    display: flex; position: fixed; top: 30px; left: 40px; z-index: 200;
    font-size: 14px; font-weight: 600; color: #475569; cursor: pointer;
    background: rgba(255,255,255,0.6); padding: 8px 16px; border-radius: 20px;
    backdrop-filter: blur(10px); border: 1px solid rgba(255,255,255,0.5); transition: all 0.2s;
  }
  .pc-back-btn:hover { background: #fff; transform: translateX(-4px); box-shadow: 0 4px 12px rgba(0,0,0,0.05); }

  /* --- 2.4 滚动区变成容器 --- */
  .scroll-area {
    height: auto; 
    overflow: visible; 
    width: 100%; 
    display: block; /* 关键：取消 flex，让内部 margin: auto 生效 */
    padding-bottom: 120px;
  }

  /* --- 2.5 卡片样式：绝对居中，大圆角 --- */
  .detail-card {
    width: 720px;
    margin: 60px auto 0; /* 上60，左右auto=居中 */
    background: #ffffff;
    border-radius: 24px;
    box-shadow: 0 20px 60px -10px rgba(15, 23, 42, 0.1);
    overflow: hidden;
    position: relative;
    border: 1px solid rgba(255, 255, 255, 0.8);
    z-index: 10;
  }

  /* 去掉手机端的弧形遮罩 */
  .mobile-curve-mask { display: none; }
  
  /* 媒体区 */
  .media-wrapper { height: 450px; }

  /* 内容内边距 */
  .content-body { padding: 40px 50px; }

  /* 字体与排版优化 */
  .post-title { font-size: 28px; margin-bottom: 24px; line-height: 1.3; }
  .text-content { font-size: 17px; line-height: 2; color: #334155; }
  
  .wiki-card { cursor: pointer; transition: transform 0.2s, box-shadow 0.2s; }
  .wiki-card:hover { transform: translateY(-2px); box-shadow: 0 12px 30px rgba(30, 41, 59, 0.2); }

  .mobile-bottom-spacer { height: 0; }

  /* --- 2.6 底部栏：绝对居中 --- */
  .glass-bottom-bar {
    position: fixed;
    bottom: 30px;
    width: 600px; /* PC端固定宽度 */
    
    /* 绝对水平居中 */
    left: 50%;
    transform: translateX(-50%);
    right: auto;
    
    background: rgba(255, 255, 255, 0.9);
    border: 1px solid rgba(255,255,255,0.8);
    box-shadow: 0 20px 40px rgba(0,0,0,0.15);
    z-index: 1000;
  }

  .action-container { box-shadow: none; border: none; background: transparent; }
  .comment-item { padding: 10px; border-radius: 12px; transition: background 0.2s; }
  .comment-item:hover { background: #f8fafc; }
}
</style>