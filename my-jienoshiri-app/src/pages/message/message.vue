<template>
  <view class="container">
    <view class="tab-bar">
      <view class="tab-item" :class="{ active: currentTab === 0 }" @click="currentTab = 0">通知</view>
      <view class="tab-item" :class="{ active: currentTab === 1 }" @click="currentTab = 1">私信</view>
    </view>

    <scroll-view scroll-y class="list-area" v-if="currentTab === 0">
      <view class="msg-item" v-for="(item, index) in notifications" :key="index" @click="handleNotiClick(item)">
        <view class="icon-box">
          <text v-if="item.type === 1">❤️</text>
          <text v-else-if="item.type === 2">💬</text>
          <text v-else>🔔</text>
        </view>
        <view class="msg-body">
          <view class="msg-header">
            <text class="msg-title">{{ item.title }}</text>
            <text class="msg-time">{{ formatTime(item.createTime) }}</text>
          </view>
          <text class="msg-content">{{ item.content }}</text>
        </view>
        <view class="unread-dot" v-if="!item.isRead"></view>
      </view>
      <view v-if="notifications.length === 0" class="empty-tip">暂无通知</view>
    </scroll-view>

    <scroll-view scroll-y class="list-area" v-if="currentTab === 1">
      <view class="chat-item" v-for="(chat, index) in conversations" :key="index" @click="goToChat(chat)">
        <image class="avatar" :src="chat.targetAvatar || '/static/logo.png'" mode="aspectFill"></image>
        <view class="chat-body">
          <view class="chat-header">
            <text class="chat-name">{{ chat.targetName }}</text>
            <text class="chat-time">{{ formatTime(chat.latestTime) }}</text>
          </view>
          <text class="chat-content">{{ chat.latestContent }}</text>
        </view>
      </view>
      <view v-if="conversations.length === 0" class="empty-tip">暂无私信，去个人主页找人聊天吧~</view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';

const currentTab = ref(0);
const notifications = ref([]);
const conversations = ref([]);

onShow(() => {
  if (!uni.getStorageSync('token')) return;
  fetchNotifications();
  fetchConversations();
});

// 获取通知
const fetchNotifications = () => {
  uni.request({
    url: 'http://localhost:8080/message/notifications',
    header: { 'Authorization': uni.getStorageSync('token') },
    success: (res) => { notifications.value = res.data; }
  });
};

// 获取私信会话
const fetchConversations = () => {
  uni.request({
    url: 'http://localhost:8080/message/conversations',
    header: { 'Authorization': uni.getStorageSync('token') },
    success: (res) => { conversations.value = res.data; }
  });
};

// 点击通知跳转
const handleNotiClick = (item) => {
  // 标记已读
  uni.request({
    url: `http://localhost:8080/message/notification/read/${item.id}`,
    method: 'POST',
    header: { 'Authorization': uni.getStorageSync('token') }
  });
  
  // 跳转到关联帖子
  if (item.relationId) {
    uni.navigateTo({ url: `/pages/post-detail/post-detail?id=${item.relationId}` });
  }
};

// 点击私信跳转
const goToChat = (chat) => {
  // 同样进行编码传递
  const safeAvatar = encodeURIComponent(chat.targetAvatar || '');
  uni.navigateTo({
    url: `/pages/chat/chat?targetId=${chat.targetUserId}&name=${chat.targetName}&avatar=${safeAvatar}`
  });
};

const formatTime = (t) => t ? t.replace('T', ' ').substring(5, 16) : '';
</script>

<style>
.container { background-color: #fff; height: 100vh; display: flex; flex-direction: column; }
.tab-bar { display: flex; border-bottom: 1px solid #f5f5f5; height: 44px; align-items: center; }
.tab-item { flex: 1; text-align: center; font-size: 15px; color: #666; height: 44px; line-height: 44px; position: relative; }
.tab-item.active { color: #333; font-weight: bold; font-size: 16px; }
.tab-item.active::after { content: ''; position: absolute; bottom: 0; left: 50%; transform: translateX(-50%); width: 20px; height: 3px; background: #ff2442; border-radius: 2px; }

.list-area { flex: 1; height: 0; background: #fff; }

/* 通知样式 */
.msg-item { display: flex; padding: 15px; border-bottom: 1px solid #f9f9f9; align-items: flex-start; }
.icon-box { width: 40px; height: 40px; background: #f0f8ff; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 20px; margin-right: 12px; flex-shrink: 0; }
.msg-body { flex: 1; display: flex; flex-direction: column; }
.msg-header { display: flex; justify-content: space-between; margin-bottom: 4px; }
.msg-title { font-size: 15px; font-weight: bold; color: #333; }
.msg-time { font-size: 12px; color: #ccc; }
.msg-content { font-size: 13px; color: #666; line-height: 1.4; }
.unread-dot { width: 8px; height: 8px; background: #ff2442; border-radius: 50%; margin-left: 5px; margin-top: 5px; }

/* 私信样式 */
.chat-item { display: flex; padding: 15px; border-bottom: 1px solid #f9f9f9; align-items: center; }
.avatar { width: 45px; height: 45px; border-radius: 50%; margin-right: 12px; border: 1px solid #eee; }
.chat-body { flex: 1; }
.chat-header { display: flex; justify-content: space-between; margin-bottom: 4px; }
.chat-name { font-size: 15px; font-weight: bold; color: #333; }
.chat-time { font-size: 12px; color: #ccc; }
.chat-content { font-size: 13px; color: #999; display: block; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; max-width: 200px; }
.empty-tip { text-align: center; color: #ccc; padding-top: 100px; font-size: 14px; }
</style>