<template>
  <view class="chat-container">
    <scroll-view scroll-y class="msg-list" :scroll-top="scrollTop" scroll-with-animation>
      <view class="msg-row" v-for="(msg, index) in messages" :key="index" :class="{ 'self': msg.isSelf }">
        
        <image v-if="!msg.isSelf" class="avatar" :src="targetAvatar || '/static/logo.png'" mode="aspectFill"></image>
        
        <view class="bubble">
          <text>{{ msg.content }}</text>
        </view>
        
        <image v-if="msg.isSelf" class="avatar" :src="myAvatar || '/static/logo.png'" mode="aspectFill"></image>
      </view>
      <view style="height: 20px;"></view>
    </scroll-view>

    <view class="input-area">
      <input class="input-field" v-model="content" placeholder="发消息..." confirm-type="send" @confirm="send" />
      <view class="send-btn" @click="send">发送</view>
    </view>
  </view>
</template>

<script setup>
import { ref, nextTick } from 'vue';
import { onLoad, onUnload } from '@dcloudio/uni-app';

const targetId = ref(null);
const targetName = ref('');
const targetAvatar = ref(''); // ⭐ 新增：存储对方头像
const myAvatar = ref('');
const messages = ref([]);
const content = ref('');
const scrollTop = ref(0);
let timer = null;

onLoad((options) => {
  targetId.value = options.targetId;
  targetName.value = options.name || '聊天';
  uni.setNavigationBarTitle({ title: targetName.value });
  
  // ⭐ 核心：接收并解码头像参数
  if (options.avatar) {
    targetAvatar.value = decodeURIComponent(options.avatar);
  }

  // 获取我自己的头像
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
  uni.request({
    url: `http://localhost:8080/message/chat?targetUserId=${targetId.value}`,
    header: { 'Authorization': token },
    success: (res) => {
      if (res.statusCode === 200) {
        // 简单判断发送者是否为自己
        const user = uni.getStorageSync('user');
        const myId = user ? user.id : 0;
        
        messages.value = res.data.map(m => ({
          ...m,
          isSelf: m.senderId == myId
        }));
        scrollToBottom();
      }
    }
  });
};

const send = () => {
  if (!content.value) return;
  const token = uni.getStorageSync('token');
  
  uni.request({
    url: 'http://localhost:8080/message/send',
    method: 'POST',
    header: { 'Authorization': token },
    data: { receiverId: targetId.value, content: content.value },
    success: () => {
      content.value = '';
      fetchHistory();
    }
  });
};

const scrollToBottom = () => {
  nextTick(() => {
    scrollTop.value = 9999999;
  });
};
</script>

<style>
.chat-container { display: flex; flex-direction: column; height: 100vh; background: #f2f2f2; }
.msg-list { flex: 1; height: 0; padding: 15px; box-sizing: border-box; }
.msg-row { display: flex; margin-bottom: 15px; align-items: flex-start; }
.msg-row.self { justify-content: flex-end; }
.avatar { width: 40px; height: 40px; border-radius: 4px; margin: 0 10px; flex-shrink: 0; background-color: #eee; }
.bubble { max-width: 70%; padding: 10px 14px; border-radius: 8px; font-size: 15px; line-height: 1.5; position: relative; word-break: break-all; }
.msg-row .bubble { background: #fff; color: #333; border-top-left-radius: 0; }
.msg-row.self .bubble { background: #95ec69; color: #000; border-top-right-radius: 0; }
.input-area { background: #f7f7f7; padding: 10px; border-top: 1px solid #ddd; display: flex; align-items: center; padding-bottom: 30px; }
.input-field { flex: 1; background: #fff; height: 40px; border-radius: 4px; padding: 0 10px; font-size: 15px; margin-right: 10px; }
.send-btn { background: #07c160; color: #fff; padding: 0 15px; height: 40px; line-height: 40px; border-radius: 4px; font-size: 14px; }
</style>