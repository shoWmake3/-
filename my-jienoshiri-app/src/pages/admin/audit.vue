<template>
  <view class="container">
    <view class="tabs">
      <view class="tab" :class="{ active: currentTab === 0 }" @click="currentTab = 0">帖子审核</view>
      <view class="tab" :class="{ active: currentTab === 1 }" @click="currentTab = 1">Wiki审核</view>
      <view class="tab" :class="{ active: currentTab === 2 }" @click="currentTab = 2">用户认证</view>
    </view>

    <scroll-view scroll-y class="list-area">
      
      <view v-if="currentTab === 0">
        <view class="audit-item" v-for="(item, index) in postList" :key="index">
          <view class="item-content">
            <text class="item-title">{{ item.title || '无标题' }}</text>
            <text class="item-desc">{{ item.content }}</text>
            
            <view class="media-row" v-if="item.mediaUrls && item.mediaUrls !== '[]'">
               <block v-for="(url, imgIndex) in getMedia(item.mediaUrls)" :key="imgIndex">
                   <video v-if="isVideo(url)" :src="url" class="media-thumb"></video>
                   <image v-else :src="url" mode="aspectFill" class="media-thumb" @click.stop="preview(url, item.mediaUrls)"></image>
               </block>
            </view>

            <text class="item-info">作者ID: {{ item.userId }} · {{ formatTime(item.createTime) }}</text>
          </view>
          <view class="btn-row">
            <button class="btn pass" @click="handlePost(item.id, true)">通过</button>
            <button class="btn reject" @click="handlePost(item.id, false)">拒绝</button>
            
            <button class="btn ban" @click="handleBan(item.id)">⚡ 封号</button>
          </view>
        </view>
        <view v-if="postList.length === 0" class="empty">暂无待审核帖子</view>
      </view>

      <view v-if="currentTab === 1">
        <view class="audit-item" v-for="(item, index) in wikiList" :key="index">
          <view class="item-content">
            <text class="item-title">词条：{{ item.title }}</text>
            <text class="item-desc">分类：{{ item.category }}</text>
            <text class="item-desc">摘要：{{ item.summary }}</text>
          </view>
          <view class="btn-row">
            <button class="btn pass" @click="handleWiki(item.id, true)">通过</button>
            <button class="btn reject" @click="handleWiki(item.id, false)">拒绝</button>
          </view>
        </view>
        <view v-if="wikiList.length === 0" class="empty">暂无待审核词条</view>
      </view>

      <view v-if="currentTab === 2">
        <view class="audit-item" v-for="(item, index) in userList" :key="index">
          <view class="item-content">
            <view class="user-header">
                <image :src="item.avatar || '/static/logo.png'" class="avatar-small"></image>
                <text class="item-title">{{ item.nickname }} ({{ item.username }})</text>
            </view>
            <text class="item-desc">申请身份：<text style="color:#007aff;font-weight:bold">{{ getIdentityName(item.identityType) }}</text></text>
            
            <text class="item-desc" style="margin-top:10px;">身份证明材料：</text>
            <view class="media-row" v-if="item.identityProof">
               <image :src="item.identityProof" mode="aspectFit" class="proof-img" 
                      @click.stop="previewSingle(item.identityProof)"></image>
            </view>
            <text v-else class="item-info" style="color:red">未上传材料 (异常数据)</text>
            
            <text class="item-info">注册时间: {{ formatTime(item.createTime) }}</text>
          </view>
          <view class="btn-row">
            <button class="btn pass" @click="handleUser(item.id, true)">通过认证</button>
            <button class="btn reject" @click="handleUser(item.id, false)">驳回申请</button>
          </view>
        </view>
        <view v-if="userList.length === 0" class="empty">暂无待认证用户</view>
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
const userList = ref([]);

onShow(() => {
  fetchPosts();
  fetchWikis();
  fetchUsers();
});

// 获取数据
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

const fetchUsers = () => {
  uni.request({
    url: 'http://localhost:8080/admin/audit/users',
    header: { 'Authorization': uni.getStorageSync('token') },
    success: (res) => { userList.value = res.data; }
  });
};

// 操作逻辑
const handlePost = (id, pass) => {
  auditRequest('http://localhost:8080/admin/audit/post', { id, pass, reason: pass ? '' : '内容违规' }, fetchPosts);
};

const handleWiki = (id, pass) => {
  auditRequest('http://localhost:8080/admin/audit/wiki', { id, pass, reason: pass ? '' : '不符合收录标准' }, fetchWikis);
};

const handleUser = (id, pass) => {
  if (!id) return uni.showToast({ title: 'ID错误', icon: 'none' });
  uni.showModal({
      title: pass ? '确认通过?' : '确认驳回?',
      content: pass ? '用户将获得认证标识' : '请输入理由:',
      editable: !pass, placeholderText: '驳回理由',
      success: (res) => {
          if (res.confirm) {
              const reason = res.content || '材料不符合要求';
              auditRequest('http://localhost:8080/admin/audit/user', { id, pass, reason }, fetchUsers);
          }
      }
  });
};

// ⭐ 封号逻辑
const handleBan = (postId) => {
  uni.showModal({
    title: '⚠️ 严重警告',
    content: '确定要删除此帖并【永久封禁】该用户吗？此操作不可恢复！',
    confirmColor: '#dd524d',
    success: (res) => {
      if (res.confirm) {
        uni.showLoading({ title: '执行中...' });
        uni.request({
          url: 'http://localhost:8080/admin/ban',
          method: 'POST',
          header: { 
             'Authorization': uni.getStorageSync('token'),
             'Content-Type': 'application/x-www-form-urlencoded'
          },
          data: { postId: postId, reason: '发布严重违规内容' },
          success: (res) => {
            uni.hideLoading();
            if (res.statusCode === 200) {
              uni.showToast({ title: '已封杀', icon: 'success' });
              fetchPosts(); // 刷新列表
            } else {
              uni.showToast({ title: '操作失败', icon: 'none' });
            }
          }
        });
      }
    }
  });
};

const auditRequest = (url, data, callback) => {
    uni.showLoading({ title: '提交中' });
    uni.request({
        url: url,
        method: 'POST',
        header: { 
            'Authorization': uni.getStorageSync('token'),
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        data: data,
        success: (res) => {
            uni.hideLoading();
            if(res.statusCode === 200) {
                uni.showToast({ title: '操作成功' });
                if(callback) callback();
            } else {
                uni.showToast({ title: '操作失败', icon: 'none' });
            }
        },
        fail: () => { uni.hideLoading(); }
    });
}

// 辅助函数
const getIdentityName = (type) => { 
    const map = { 'student': '留学生', 'agent': '中介', 'worker': '打工人', 'tourist': '游客' }; 
    return map[type] || type; 
};
const getMedia = (jsonStr) => {
    try { return JSON.parse(jsonStr) || []; } catch (e) { return jsonStr ? [jsonStr] : []; }
};
const isVideo = (url) => url && (url.endsWith('.mp4') || url.endsWith('.mov'));
const preview = (current, jsonStr) => {
    const urls = getMedia(jsonStr).filter(u => !isVideo(u));
    uni.previewImage({ current, urls });
};
const previewSingle = (url) => { uni.previewImage({ current: url, urls: [url] }); };
const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 16) : '';
</script>

<style>
.container { background: #f5f5f5; min-height: 100vh; }
.tabs { display: flex; background: #fff; padding: 10px; border-bottom: 1px solid #eee; position: sticky; top: 0; z-index: 10; }
.tab { flex: 1; text-align: center; padding: 10px; font-weight: bold; color: #999; }
.tab.active { color: #007aff; border-bottom: 2px solid #007aff; }

.list-area { padding: 10px; }
.audit-item { background: #fff; padding: 15px; border-radius: 8px; margin-bottom: 10px; }
.item-title { font-size: 16px; font-weight: bold; margin-bottom: 5px; display: block; }
.item-desc { font-size: 14px; color: #333; margin-bottom: 8px; display: block; line-height: 1.5; }
.item-info { font-size: 12px; color: #999; margin-top: 5px; display: block; }

.user-header { display: flex; align-items: center; margin-bottom: 10px; border-bottom: 1px solid #eee; padding-bottom: 10px;}
.avatar-small { width: 30px; height: 30px; border-radius: 50%; margin-right: 10px; background: #eee; }
.proof-img { width: 100%; height: 200px; background: #000; border-radius: 4px; margin-top: 5px; }

.media-row { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 10px; }
.media-thumb { width: 90px; height: 90px; border-radius: 6px; background: #eee; object-fit: cover; }

.btn-row { display: flex; justify-content: flex-end; margin-top: 10px; gap: 10px; border-top: 1px solid #f0f0f0; padding-top: 10px; }
.btn { font-size: 12px; padding: 0 15px; height: 30px; line-height: 30px; margin: 0; }
.pass { background: #4cd964; color: #fff; }
.reject { background: #dd524d; color: #fff; }
/* ⭐ 封号按钮样式：黑色背景 */
.ban { background: #333; color: #fff; font-weight: bold; }

.empty { text-align: center; color: #999; margin-top: 50px; }
</style>