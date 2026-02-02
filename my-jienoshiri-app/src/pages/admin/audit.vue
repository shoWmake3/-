<template>
  <view class="container">
    <view class="tabs">
      <view class="tab" :class="{ active: currentTab === 0 }" @click="currentTab = 0">帖子审核</view>
      <view class="tab" :class="{ active: currentTab === 1 }" @click="currentTab = 1">Wiki审核</view>
    </view>

    <scroll-view scroll-y class="list-area">
      
      <view v-if="currentTab === 0">
        <view class="audit-item" v-for="(item, index) in postList" :key="index">
          <view class="item-content">
            <text class="item-title">{{ item.title }}</text>
            <text class="item-desc">{{ item.content }}</text>
            <text class="item-info">作者ID: {{ item.userId }} · {{ formatTime(item.createTime) }}</text>
          </view>
          <view class="btn-row">
            <button class="btn pass" @click="handlePost(item.id, true)">通过</button>
            <button class="btn reject" @click="handlePost(item.id, false)">拒绝</button>
          </view>
        </view>
        <view v-if="postList.length === 0" class="empty">暂无待审核帖子</view>
      </view>

      <view v-if="currentTab === 1">
        <view class="audit-item" v-for="(item, index) in wikiList" :key="index">
          <view class="item-content">
            <text class="item-title">词条：{{ item.title }}</text>
            <text class="item-desc">分类：{{ item.category }}</text>
            <text class="item-desc">{{ item.summary }}</text>
          </view>
          <view class="btn-row">
            <button class="btn pass" @click="handleWiki(item.id, true)">通过</button>
            <button class="btn reject" @click="handleWiki(item.id, false)">拒绝</button>
          </view>
        </view>
        <view v-if="wikiList.length === 0" class="empty">暂无待审核词条</view>
      </view>

    </scroll-view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';

const currentTab = ref(0);
const postList = ref([]);
const wikiList = ref([]);

onShow(() => {
  fetchPosts();
  fetchWikis();
});

const fetchPosts = () => {
  uni.request({
    url: 'http://localhost:8080/admin/audit/posts',
    header: { 'Authorization': uni.getStorageSync('token') },
    success: (res) => { postList.value = res.data; }
  });
};

const fetchWikis = () => {
  uni.request({
    url: 'http://localhost:8080/admin/audit/wikis',
    header: { 'Authorization': uni.getStorageSync('token') },
    success: (res) => { wikiList.value = res.data; }
  });
};

const handlePost = (id, pass) => {
  uni.request({
    url: 'http://localhost:8080/admin/audit/post',
    method: 'POST',
    header: { 
        'Authorization': uni.getStorageSync('token'),
        'Content-Type': 'application/x-www-form-urlencoded'
    },
    data: { id, pass, reason: pass ? '' : '内容违规' },
    success: () => {
        uni.showToast({ title: '操作成功' });
        fetchPosts(); // 刷新列表
    }
  });
};

const handleWiki = (id, pass) => {
  uni.request({
    url: 'http://localhost:8080/admin/audit/wiki',
    method: 'POST',
    header: { 
        'Authorization': uni.getStorageSync('token'),
        'Content-Type': 'application/x-www-form-urlencoded'
    },
    data: { id, pass, reason: pass ? '' : '不符合收录标准' },
    success: () => {
        uni.showToast({ title: '操作成功' });
        fetchWikis(); // 刷新列表
    }
  });
};

const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 16) : '';
</script>

<style>
.container { background: #f5f5f5; min-height: 100vh; }
.tabs { display: flex; background: #fff; padding: 10px; border-bottom: 1px solid #eee; }
.tab { flex: 1; text-align: center; padding: 10px; font-weight: bold; color: #999; }
.tab.active { color: #007aff; border-bottom: 2px solid #007aff; }

.list-area { padding: 10px; }
.audit-item { background: #fff; padding: 15px; border-radius: 8px; margin-bottom: 10px; }
.item-title { font-size: 16px; font-weight: bold; margin-bottom: 5px; display: block; }
.item-desc { font-size: 14px; color: #666; margin-bottom: 8px; display: block; }
.item-info { font-size: 12px; color: #999; }

.btn-row { display: flex; justify-content: flex-end; margin-top: 10px; gap: 10px; }
.btn { font-size: 12px; padding: 0 15px; height: 30px; line-height: 30px; margin: 0; }
.pass { background: #4cd964; color: #fff; }
.reject { background: #dd524d; color: #fff; }
.empty { text-align: center; color: #999; margin-top: 50px; }
</style>