<template>
  <view class="page-container">
    
    <view class="nav-header">
      <view class="tab-pill-box">
        <view class="active-bg" :style="{ transform: currentTab === 0 ? 'translateX(0)' : 'translateX(100%)' }"></view>
        
        <view class="tab-item" :class="{ active: currentTab === 0 }" @click="currentTab = 0">
          <text>通知</text>
          <view class="red-dot" v-if="unreadNotiCount > 0"></view>
        </view>
        <view class="tab-item" :class="{ active: currentTab === 1 }" @click="currentTab = 1">
          <text>私信</text>
        </view>
      </view>
    </view>

    <scroll-view scroll-y class="scroll-list" v-if="currentTab === 0" show-scrollbar="false">
      <view class="list-content">
        <view class="msg-card" v-for="(item, index) in notifications" :key="index" @click="handleNotiClick(item)">
          <view class="icon-box" :class="'type-' + item.type">
            <text class="emoji" v-if="item.type === 1">❤️</text> <text class="emoji" v-else-if="item.type === 2">💬</text> <text class="emoji" v-else>🔔</text> </view>
          
          <view class="content-box">
            <view class="top-line">
              <text class="title">{{ item.title }}</text>
              <text class="time">{{ formatTime(item.createTime) }}</text>
            </view>
            <text class="desc">{{ item.content }}</text>
          </view>
          
          <view class="unread-dot" v-if="!item.isRead"></view>
        </view>

        <view v-if="notifications.length === 0" class="empty-state">
          <view class="empty-icon">🔕</view>
          <text>暂无新通知</text>
        </view>
      </view>
    </scroll-view>

    <scroll-view scroll-y class="scroll-list" v-if="currentTab === 1" show-scrollbar="false">
      <view class="list-content">
        <view class="chat-card" v-for="(chat, index) in conversations" :key="index" @click="goToChat(chat)">
          <view class="avatar-wrap">
            <image class="avatar" :src="chat.targetAvatar || '/static/logo.png'" mode="aspectFill"></image>
            <view class="online-dot"></view>
          </view>
          
          <view class="content-box">
            <view class="top-line">
              <text class="name">{{ chat.targetName }}</text>
              <text class="time">{{ formatTime(chat.latestTime) }}</text>
            </view>
            <text class="last-msg">{{ chat.latestContent }}</text>
          </view>
        </view>

        <view v-if="conversations.length === 0" class="empty-state">
          <view class="empty-icon">✉️</view>
          <text>还没有私信，去撩一下别人吧~</text>
        </view>
      </view>
    </scroll-view>

  </view>
</template>

<script setup>
import { ref, computed } from 'vue';
import { onShow } from '@dcloudio/uni-app';

const currentTab = ref(0);
const notifications = ref([]);
const conversations = ref([]);

// 计算未读通知数 (可选)
const unreadNotiCount = computed(() => {
  return notifications.value.filter(n => !n.isRead).length;
});

// ⭐ 核心修复：onShow 每次页面显示都刷新数据
onShow(() => {
  const token = uni.getStorageSync('token');
  if (token) {
    fetchNotifications();
    fetchConversations();
  } else {
    // 未登录时清空，或者跳转登录
    notifications.value = [];
    conversations.value = [];
  }
});

const fetchNotifications = () => {
  uni.request({
    url: 'http://localhost:8080/message/notifications',
    header: { 'Authorization': uni.getStorageSync('token') },
    success: (res) => {
      // ⭐ BUG修复核心：兼容 {code:200, data:[]} 和 直接 [] 两种格式
      if (res.statusCode === 200) {
        const raw = res.data;
        // 如果 raw.data 存在且是数组，说明是包装过的对象；否则直接用 raw
        const list = (raw && raw.data && Array.isArray(raw.data)) ? raw.data : raw;
        // 再次确认是数组，防止赋值错误导致页面空白
        notifications.value = Array.isArray(list) ? list : [];
      }
    },
    fail: (e) => console.error('通知获取失败', e)
  });
};

const fetchConversations = () => {
  uni.request({
    url: 'http://localhost:8080/message/conversations',
    header: { 'Authorization': uni.getStorageSync('token') },
    success: (res) => {
      // ⭐ BUG修复核心：同上
      if (res.statusCode === 200) {
        const raw = res.data;
        const list = (raw && raw.data && Array.isArray(raw.data)) ? raw.data : raw;
        conversations.value = Array.isArray(list) ? list : [];
      }
    },
    fail: (e) => console.error('私信获取失败', e)
  });
};

const handleNotiClick = (item) => {
  // 1. 本地立即标为已读 (优化体验)
  item.isRead = true; 
  
  // 2. 发送请求
  uni.request({
    url: `http://localhost:8080/message/notification/read/${item.id}`,
    method: 'POST',
    header: { 'Authorization': uni.getStorageSync('token') }
  });
  
  // 3. 跳转
  if (item.relationId) {
    uni.navigateTo({ url: `/pages/post-detail/post-detail?id=${item.relationId}` });
  }
};

const goToChat = (chat) => {
  const safeAvatar = encodeURIComponent(chat.targetAvatar || '');
  uni.navigateTo({
    url: `/pages/chat/chat?targetId=${chat.targetUserId}&name=${chat.targetName}&avatar=${safeAvatar}`
  });
};

// 智能时间格式化
const formatTime = (t) => {
  if (!t) return '';
  const date = new Date(t);
  const now = new Date();
  const isToday = date.getDate() === now.getDate() && date.getMonth() === now.getMonth();
  
  // 如果是今天，显示具体时间；否则显示日期
  if (isToday) {
    return t.substring(11, 16); // 14:30
  } else {
    return t.substring(5, 10); // 05-20
  }
};
</script>

<style>
:root {
  --primary: #6366f1;
  --bg-page: #f8fafc;
  --text-main: #1e293b;
  --text-sub: #64748b;
  --card-bg: #ffffff;
}

.page-container {
  display: flex; flex-direction: column; height: 100vh;
  background-color: var(--bg-page);
}

/* --- 1. 顶部 Tab --- */
.nav-header {
  padding: 12px 20px;
  background: var(--bg-page);
  /* 粘性定位，确保滚动时还在 */
  position: sticky; top: 0; z-index: 10;
}

.tab-pill-box {
  background: #e2e8f0;
  border-radius: 20px;
  height: 40px;
  display: flex;
  position: relative;
  padding: 4px;
  box-sizing: border-box;
}

/* 滑动的白色背景块 */
.active-bg {
  position: absolute; top: 4px; left: 4px; bottom: 4px;
  width: calc(50% - 4px);
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.tab-item {
  flex: 1;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; color: #64748b; font-weight: 600;
  position: relative; z-index: 1;
  transition: color 0.3s;
}

.tab-item.active { color: var(--text-main); }

.red-dot {
  width: 6px; height: 6px; background: #ef4444; border-radius: 50%;
  position: absolute; top: 10px; right: 25%;
}

/* --- 2. 列表容器 --- */
.scroll-list { flex: 1; height: 0; }
.list-content { padding: 10px 20px 40px; }

/* --- 3. 通知卡片 --- */
.msg-card {
  background: var(--card-bg);
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 12px;
  display: flex; align-items: flex-start;
  box-shadow: 0 2px 10px rgba(0,0,0,0.02);
  position: relative;
  transition: transform 0.1s;
}
.msg-card:active { transform: scale(0.98); background: #f8fafc; }

.icon-box {
  width: 44px; height: 44px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  margin-right: 14px; flex-shrink: 0;
}
.emoji { font-size: 22px; }

/* 不同类型的背景色 */
.type-1 { background: linear-gradient(135deg, #fecaca 0%, #fca5a5 100%); } /* 点赞-红 */
.type-2 { background: linear-gradient(135deg, #bfdbfe 0%, #93c5fd 100%); } /* 评论-蓝 */
.type-3 { background: linear-gradient(135deg, #ddd6fe 0%, #c4b5fd 100%); } /* 系统-紫 */

.content-box { flex: 1; overflow: hidden; }
.top-line { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.title { font-size: 15px; font-weight: 700; color: var(--text-main); }
.time { font-size: 11px; color: #94a3b8; }
.desc { font-size: 13px; color: var(--text-sub); line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

.unread-dot {
  position: absolute; top: 16px; right: 16px;
  width: 8px; height: 8px; background: #ef4444; border-radius: 50%;
  box-shadow: 0 0 0 2px #fff;
}

/* --- 4. 私信卡片 --- */
.chat-card {
  background: var(--card-bg);
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 12px;
  display: flex; align-items: center;
  box-shadow: 0 2px 10px rgba(0,0,0,0.02);
}
.chat-card:active { background: #f8fafc; }

.avatar-wrap { position: relative; margin-right: 14px; }
.avatar { width: 48px; height: 48px; border-radius: 50%; background: #eee; border: 1px solid #f1f5f9; }
.online-dot {
  position: absolute; bottom: 2px; right: 2px;
  width: 10px; height: 10px; background: #22c55e;
  border: 2px solid #fff; border-radius: 50%;
}

.name { font-size: 16px; font-weight: 700; color: var(--text-main); }
.last-msg {
  font-size: 14px; color: var(--text-sub);
  margin-top: 4px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  max-width: 220px;
}

/* --- 5. 空状态 --- */
.empty-state {
  display: flex; flex-direction: column; align-items: center;
  padding-top: 100px; opacity: 0.6;
}
.empty-icon { font-size: 48px; margin-bottom: 16px; filter: grayscale(1); }
.empty-state text { font-size: 14px; color: #94a3b8; }

/* PC 端适配 (分栏显示) */
@media screen and (min-width: 800px) {
  .page-container {
    width: 100%; max-width: 800px;
    margin: 0 auto;
    border-left: 1px solid #e2e8f0;
    border-right: 1px solid #e2e8f0;
  }
}
</style>