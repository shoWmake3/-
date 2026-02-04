<template>
  <view class="page-container">

    <div class="ambient-bg">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
      <div class="noise-overlay"></div>
    </div>

    <scroll-view scroll-y class="chat-list" :scroll-top="scrollTop" scroll-with-animation :show-scrollbar="false">

      <view class="chat-wrapper">
        <view class="msg-row" v-for="(msg, index) in messages" :key="index" :class="{ 'self': msg.isSelf }">

          <view class="avatar-box left" v-if="!msg.isSelf">
            <image class="avatar" :src="targetAvatar || '/static/logo.png'" mode="aspectFill"></image>
          </view>

          <view class="avatar-box right" v-if="msg.isSelf">
            <image class="avatar" :src="myAvatar || '/static/logo.png'" mode="aspectFill"></image>
          </view>

          <view class="bubble-box">
            <view class="bubble">
              <text class="msg-text">{{ msg.content }}</text>
            </view>
          </view>
        </view>
      </view>

      <view style="height: 100px;"></view>
    </scroll-view>

    <view class="glass-input-bar">
      <view class="input-inner">
        <input class="chat-input" v-model="content" placeholder="发消息..." confirm-type="send" @confirm="send"
          cursor-spacing="20" placeholder-style="color: #94a3b8;" />

        <view class="send-btn" @click="send" :class="{ 'active': content.length > 0 }">
          <text class="send-icon">↑</text>
        </view>
      </view>
    </view>

  </view>
</template>

<script setup>
import { ref, nextTick } from 'vue';
import { onLoad, onUnload } from '@dcloudio/uni-app';

const targetId = ref(null);
const targetName = ref('');
const targetAvatar = ref('');
const myAvatar = ref('');
const messages = ref([]);
const content = ref('');
const scrollTop = ref(0);
let timer = null;

onLoad((options) => {
  targetId.value = options.targetId;
  targetName.value = options.name || '聊天';
  uni.setNavigationBarTitle({ title: targetName.value });

  if (options.avatar) {
    targetAvatar.value = decodeURIComponent(options.avatar);
  }

  const me = uni.getStorageSync('user');
  if (me) myAvatar.value = me.avatar;

  fetchHistory();
  timer = setInterval(fetchHistory, 3000);
});

onUnload(() => {
  if (timer) clearInterval(timer);
});

const fetchHistory = () => {
  const token = uni.getStorageSync('token');
  if (!token) return;

  uni.request({
    url: `http://localhost:8080/message/chat?targetUserId=${targetId.value}`,
    header: { 'Authorization': token },
    success: (res) => {
      if (res.statusCode === 200) {
        const user = uni.getStorageSync('user');
        const myId = user ? user.id : 0;

        // 只有当消息数量增加时才滚动，避免轮询时屏幕乱跳
        const shouldScroll = res.data.length > messages.value.length;

        messages.value = res.data.map(m => ({
          ...m,
          isSelf: m.senderId == myId
        }));

        if (shouldScroll) scrollToBottom();
      }
    }
  });
};

const send = () => {
  if (!content.value.trim()) return;
  const token = uni.getStorageSync('token');

  uni.request({
    url: 'http://localhost:8080/message/send',
    method: 'POST',
    header: { 'Authorization': token },
    data: { receiverId: targetId.value, content: content.value },
    success: () => {
      content.value = '';
      fetchHistory(); // 立即刷新
      scrollToBottom();
    }
  });
};

const scrollToBottom = () => {
  nextTick(() => {
    // 设一个足够大的值确保滚到底
    scrollTop.value = messages.value.length * 1000 + Math.random();
  });
};
</script>

<style>
:root {
  --primary: #6366f1;
  --bg-page: #f8fafc;
  --bubble-self: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  --bubble-other: rgba(255, 255, 255, 0.8);
}

.page-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  background: var(--bg-page);
}

/* 1. 动态背景 */
.ambient-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
}

.noise-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0.03;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E");
}

.blob {
  position: absolute;
  filter: blur(80px);
  opacity: 0.6;
  animation: float 10s infinite alternate;
}

.blob-1 {
  top: -10%;
  left: -10%;
  width: 60vw;
  height: 60vw;
  background: #c4b5fd;
}

.blob-2 {
  bottom: -10%;
  right: -10%;
  width: 70vw;
  height: 70vw;
  background: #a5f3fc;
  animation-delay: -2s;
}

.blob-3 {
  top: 40%;
  left: 30%;
  width: 50vw;
  height: 50vw;
  background: #fecaca;
  opacity: 0.4;
  animation-delay: -4s;
}

@keyframes float {
  0% {
    transform: translate(0, 0);
  }

  100% {
    transform: translate(30px, 40px);
  }
}

/* 2. 消息列表 */
.chat-list {
  flex: 1;
  height: 0;
  position: relative;
  z-index: 1;
}

.chat-wrapper {
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.msg-row {
  display: flex;
  margin-bottom: 24px;
  align-items: flex-end;
  animation: slideUp 0.3s ease-out;
}

.msg-row.self {
  flex-direction: row-reverse;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 头像 */
.avatar-box {
  margin-bottom: 4px;
}

.avatar-box.left {
  margin-right: 12px;
}

.avatar-box.right {
  margin-left: 12px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
  border: 2px solid rgba(255, 255, 255, 0.8);
}

/* 气泡 */
.bubble-box {
  max-width: 70%;
  display: flex;
  flex-direction: column;
}

.bubble {
  padding: 12px 16px;
  border-radius: 20px;
  font-size: 15px;
  line-height: 1.6;
  position: relative;
  word-break: break-all;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
  transition: all 0.2s;
}

/* 对方气泡 */
.msg-row:not(.self) .bubble {
  background: var(--bubble-other);
  backdrop-filter: blur(10px);
  color: #334155;
  border-bottom-left-radius: 4px;
  /* 尖角 */
}

/* 自己气泡 */
.msg-row.self .bubble {
  background: var(--bubble-self);
  color: #ffffff;
  border-bottom-right-radius: 4px;
  /* 尖角 */
  box-shadow: 0 8px 20px rgba(99, 102, 241, 0.3);
}

.msg-text {
  display: inline-block;
}

/* 3. 悬浮输入栏 (PC/Mobile 通用胶囊) */
.glass-input-bar {
  position: fixed;
  bottom: 30px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  z-index: 100;
  pointer-events: none;
  /* 让两侧空白处可点击穿透 */
}

.input-inner {
  width: 90%;
  max-width: 600px;
  height: 56px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 30px;
  box-shadow: 0 20px 40px -10px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  padding: 0 6px 0 20px;
  box-sizing: border-box;
  pointer-events: auto;
  /* 恢复点击 */
  transition: transform 0.2s;
}

.chat-input {
  flex: 1;
  height: 100%;
  font-size: 15px;
  color: #1e293b;
}

.send-btn {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #cbd5e1;
  /* 禁用灰 */
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 10px;
  transition: all 0.3s;
}

.send-btn.active {
  background: var(--text-main);
  /* 激活黑/深蓝 */
  transform: scale(1.05);
}

.send-icon {
  color: #fff;
  font-weight: 800;
  font-size: 18px;
}

/* PC 端适配 */
@media screen and (min-width: 800px) {
  .page-container {
    align-items: center;
    /* 居中内容 */
  }

  .chat-list {
    width: 100%;
    max-width: 700px;
    /* 限制宽度，防止气泡太散 */
  }

  /* 输入栏微调 */
  .glass-input-bar {
    bottom: 40px;
  }

  .input-inner {
    max-width: 680px;
  }
}
</style>