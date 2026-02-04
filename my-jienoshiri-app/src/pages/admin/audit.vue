<template>
  <view class="page-container">
    
    <view class="nav-header">
      <view class="tab-pill-box">
        <view class="active-bg" :style="{ transform: `translateX(${currentTab * 100}%)` }"></view>
        
        <view class="tab-item" :class="{ active: currentTab === 0 }" @click="currentTab = 0">帖子审核</view>
        <view class="tab-item" :class="{ active: currentTab === 1 }" @click="currentTab = 1">Wiki收录</view>
        <view class="tab-item" :class="{ active: currentTab === 2 }" @click="currentTab = 2">用户认证</view>
      </view>
    </view>

    <scroll-view scroll-y class="list-area" show-scrollbar="true">
      <view class="content-wrapper">
        
        <view v-if="currentTab === 0">
          <view class="audit-card" v-for="(item, index) in postList" :key="index">
            <view class="card-header">
              <text class="user-id">用户ID: {{ item.userId }}</text>
              <text class="time-tag">{{ formatTime(item.createTime) }}</text>
            </view>
            
            <view class="card-body">
              <text class="content-title" v-if="item.title">{{ item.title }}</text>
              <text class="content-text">{{ item.content }}</text>
              
              <view class="media-grid" v-if="item.mediaUrls && item.mediaUrls !== '[]'">
                <block v-for="(url, imgIndex) in getMedia(item.mediaUrls)" :key="imgIndex">
                   <video v-if="isVideo(url)" :src="url" class="media-thumb"></video>
                   <image v-else :src="url" mode="aspectFill" class="media-thumb" @click.stop="preview(url, item.mediaUrls)"></image>
                </block>
              </view>
            </view>

            <view class="action-bar">
              <view class="left-actions">
                <view class="btn ban-btn" @click="handleBan(item.id)">
                  <text>⚡ 封禁账号</text>
                </view>
              </view>
              <view class="right-actions">
                <view class="btn reject-btn" @click="handlePost(item.id, false)">
                  <text>✕ 驳回</text>
                </view>
                <view class="btn pass-btn" @click="handlePost(item.id, true)">
                  <text>✓ 通过</text>
                </view>
              </view>
            </view>
          </view>
          <view v-if="postList.length === 0" class="empty-state">
            <text class="empty-icon">☕</text>
            <text>暂无待审核帖子，喝杯咖啡吧</text>
          </view>
        </view>

        <view v-if="currentTab === 1">
          <view class="audit-card wiki-style" v-for="(item, index) in wikiList" :key="index">
            <view class="wiki-tag">Wiki 申请</view>
            <text class="wiki-title">{{ item.title }}</text>
            
            <view class="wiki-meta">
              <text class="meta-label">分类：</text>
              <text class="meta-val">{{ item.category }}</text>
            </view>
            <view class="wiki-meta">
              <text class="meta-label">摘要：</text>
              <text class="meta-val">{{ item.summary }}</text>
            </view>

            <view class="action-bar simple">
              <view class="btn reject-btn" @click="handleWiki(item.id, false)">拒绝收录</view>
              <view class="btn pass-btn" @click="handleWiki(item.id, true)">确认收录</view>
            </view>
          </view>
          <view v-if="wikiList.length === 0" class="empty-state">
            <text>暂无 Wiki 申请</text>
          </view>
        </view>

        <view v-if="currentTab === 2">
          <view class="audit-card user-style" v-for="(item, index) in userList" :key="index">
            <view class="user-info-row">
              <image :src="item.avatar || '/static/logo.png'" class="avatar"></image>
              <view class="info-col">
                <text class="nickname">{{ item.nickname }}</text>
                <text class="username">@{{ item.username }}</text>
                <view class="apply-tag">申请：{{ getIdentityName(item.identityType) }}</view>
              </view>
            </view>
            
            <view class="proof-section">
              <text class="proof-label">身份证明材料</text>
              <view class="proof-img-box" v-if="item.identityProof">
                <image :src="item.identityProof" mode="aspectFit" class="proof-img" 
                       @click.stop="previewSingle(item.identityProof)"></image>
              </view>
              <view class="error-box" v-else>⚠️ 未上传材料</view>
            </view>

            <view class="action-bar simple">
              <view class="btn reject-btn" @click="handleUser(item.id, false)">驳回申请</view>
              <view class="btn pass-btn" @click="handleUser(item.id, true)">通过认证</view>
            </view>
          </view>
          <view v-if="userList.length === 0" class="empty-state">
            <text>暂无待认证用户</text>
          </view>
        </view>

      </view>
      <view style="height: 60px;"></view>
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

const handleBan = (postId) => {
  uni.showModal({
    title: '⚠️ 严重警告',
    content: '确定要删除此帖并【永久封禁】该用户吗？此操作不可恢复！',
    confirmColor: '#ef4444',
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
              fetchPosts();
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
:root {
  --primary: #6366f1;
  --success: #10b981;
  --error: #ef4444;
  --bg-page: #f1f5f9;
  --text-main: #1e293b;
}

.page-container {
  height: 100vh;
  display: flex; flex-direction: column;
  background: var(--bg-page);
}

/* 1. 顶部 Tab */
.nav-header {
  padding: 12px 20px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  position: sticky; top: 0; z-index: 10;
}

.tab-pill-box {
  background: #f1f5f9;
  border-radius: 12px;
  height: 40px;
  display: flex;
  position: relative;
  padding: 4px;
}

.active-bg {
  position: absolute; top: 4px; left: 4px; bottom: 4px;
  width: calc(33.33% - 4px);
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.05);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.tab-item {
  flex: 1;
  text-align: center; line-height: 32px;
  font-size: 13px; color: #64748b; font-weight: 600;
  position: relative; z-index: 1;
  transition: color 0.3s;
}
.tab-item.active { color: var(--primary); }

/* 2. 列表区域 */
.list-area { flex: 1; height: 0; }
.content-wrapper { padding: 20px; }

/* 通用审核卡片 */
.audit-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);
  animation: slideUp 0.3s ease-out;
}
@keyframes slideUp { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

/* A. 帖子样式 */
.card-header { display: flex; justify-content: space-between; margin-bottom: 12px; font-size: 12px; color: #94a3b8; }
.user-id { font-family: monospace; }

.content-title { font-size: 16px; font-weight: 700; color: var(--text-main); margin-bottom: 8px; display: block; }
.content-text { font-size: 14px; color: #475569; line-height: 1.6; margin-bottom: 12px; display: block; }

.media-grid { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 16px; }
.media-thumb { width: 80px; height: 80px; border-radius: 8px; background: #f1f5f9; object-fit: cover; }

/* B. Wiki 样式 */
.audit-card.wiki-style { border-left: 4px solid var(--primary); }
.wiki-tag { display: inline-block; font-size: 10px; color: #fff; background: var(--primary); padding: 2px 6px; border-radius: 4px; margin-bottom: 8px; }
.wiki-title { font-size: 18px; font-weight: 800; color: var(--text-main); margin-bottom: 12px; display: block; }
.wiki-meta { display: flex; margin-bottom: 6px; font-size: 13px; }
.meta-label { color: #94a3b8; width: 50px; }
.meta-val { color: #334155; flex: 1; }

/* C. 用户样式 */
.audit-card.user-style { border-left: 4px solid #f59e0b; }
.user-info-row { display: flex; align-items: center; margin-bottom: 16px; }
.avatar { width: 48px; height: 48px; border-radius: 50%; background: #eee; margin-right: 12px; }
.info-col { flex: 1; }
.nickname { font-size: 16px; font-weight: 700; color: var(--text-main); display: block; }
.username { font-size: 12px; color: #94a3b8; }
.apply-tag { font-size: 11px; color: #f59e0b; background: rgba(245, 158, 11, 0.1); padding: 2px 6px; border-radius: 4px; display: inline-block; margin-top: 4px; }

.proof-section { background: #f8fafc; padding: 12px; border-radius: 8px; margin-bottom: 16px; }
.proof-label { font-size: 12px; color: #64748b; margin-bottom: 8px; display: block; }
.proof-img { width: 100%; height: 180px; background: #e2e8f0; border-radius: 4px; }
.error-box { color: var(--error); font-size: 12px; text-align: center; padding: 20px; background: #fee2e2; border-radius: 4px; }

/* 操作栏 */
.action-bar { display: flex; justify-content: space-between; align-items: center; border-top: 1px dashed #e2e8f0; padding-top: 16px; margin-top: 8px; }
.action-bar.simple { justify-content: flex-end; gap: 12px; }

.right-actions { display: flex; gap: 12px; }

.btn {
  padding: 8px 16px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s;
  display: flex; align-items: center; justify-content: center;
}
.btn:active { transform: scale(0.95); }

.pass-btn { background: var(--success); color: #0c0202; box-shadow: 0 4px 10px rgba(16, 185, 129, 0.2); }
.reject-btn { background: #fff; border: 1px solid #cbd5e1; color: #64748b; }
.ban-btn { background: #1e293b; color: #fff; } /* 黑色封禁按钮 */

/* 空状态 */
.empty-state { text-align: center; padding-top: 60px; opacity: 0.6; display: flex; flex-direction: column; align-items: center; }
.empty-icon { font-size: 40px; margin-bottom: 10px; }

/* PC 适配 */
@media screen and (min-width: 800px) {
  .page-container { overflow-y: auto; display: block; }
  .list-area { display: block; height: auto; padding-bottom: 60px; }
  
  .nav-header { width: 600px; margin: 0 auto; background: transparent; box-shadow: none; padding-top: 40px; }
  .content-wrapper { width: 700px; margin: 0 auto; }
  
  .audit-card { margin-bottom: 24px; transition: transform 0.2s; }
  .audit-card:hover { transform: translateY(-2px); box-shadow: 0 10px 20px rgba(0,0,0,0.08); }
}
</style>