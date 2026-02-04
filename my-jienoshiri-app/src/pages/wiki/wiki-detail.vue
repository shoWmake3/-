<template>
  <view class="page-container">
    
    <div class="ambient-bg">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
      <div class="noise-overlay"></div>
    </div>

    <view class="nav-bar">
      <view class="back-btn" @click="goBack">
        <text class="arrow">‹</text>
        <text>返回</text>
      </view>
      <text class="nav-title">词条详情</text>
      <view class="placeholder"></view>
    </view>

    <scroll-view scroll-y class="scroll-body" show-scrollbar="false">
      <view class="article-card">
        
        <view class="header-section">
          <view class="category-tag"># {{ wiki.category || '综合知识' }}</view>
          <text class="article-title">{{ wiki.title }}</text>
          <view class="meta-row">
            <text class="meta-item">创建时间：{{ formatTime(wiki.createTime) }}</text>
            <text class="meta-item">👁️ {{ wiki.viewCount || 0 }} 阅读</text>
          </view>
        </view>

        <view class="divider"></view>

        <view class="summary-box" v-if="wiki.summary">
          <text class="summary-label">摘要</text>
          <text class="summary-content">{{ wiki.summary }}</text>
        </view>

        <view class="content-section">
          <text class="article-content" user-select>{{ wiki.content }}</text>
        </view>

        <view class="copyright-notice">
          <text>本文内容由「皆の知り」社区沉淀所得</text>
          <text>知识点由 {{ wiki.creatorId || '社区用户' }} 贡献</text>
        </view>

      </view>
      
      <view style="height: 40px;"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';

const wiki = ref({});

onMounted(() => {
  const data = uni.getStorageSync('currentWiki');
  if (data) {
    wiki.value = data;
    // 自动上报浏览量 (可选)
    reportView(data.id);
  }
});

const goBack = () => uni.navigateBack();

const formatTime = (t) => t ? t.split('T')[0] : '最近';

const reportView = (id) => {
  uni.request({
    url: `http://localhost:8080/wiki/view?id=${id}`,
    method: 'POST'
  });
};
</script>

<style>
:root {
  --primary: #6366f1;
  --text-main: #1e293b;
  --text-sub: #64748b;
}

.page-container {
  height: 100vh; display: flex; flex-direction: column;
  background: #f8fafc; position: relative; overflow: hidden;
}

/* 1. 动态背景 */
.ambient-bg { position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 0; pointer-events: none; }
.noise-overlay { position: absolute; top: 0; left: 0; width: 100%; height: 100%; opacity: 0.03; background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E"); }
.blob { position: absolute; filter: blur(80px); opacity: 0.6; animation: float 10s infinite alternate; }
.blob-1 { top: -10%; left: -10%; width: 60vw; height: 60vw; background: #c4b5fd; }
.blob-2 { bottom: -10%; right: -10%; width: 70vw; height: 70vw; background: #a5f3fc; animation-delay: -2s; }
.blob-3 { top: 40%; left: 30%; width: 50vw; height: 50vw; background: #fecaca; opacity: 0.4; animation-delay: -4s; }
@keyframes float { 0% { transform: translate(0, 0); } 100% { transform: translate(30px, 40px); } }

/* 2. 导航栏 */
.nav-bar {
  position: relative; z-index: 100;
  display: flex; justify-content: space-between; align-items: center;
  padding: 50px 20px 15px;
  background: rgba(255,255,255,0.7); backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(255,255,255,0.5);
}
.back-btn { display: flex; align-items: center; font-size: 15px; color: var(--primary); font-weight: 600; }
.back-btn .arrow { font-size: 24px; margin-right: 2px; margin-top: -2px; }
.nav-title { font-size: 16px; font-weight: 800; color: var(--text-main); }
.placeholder { width: 60px; }

/* 3. 滚动区域 */
.scroll-body { flex: 1; height: 0; position: relative; z-index: 1; }

.article-card {
  margin: 20px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 10px 40px -10px rgba(0,0,0,0.05);
  padding: 30px 24px;
  animation: slideUp 0.6s cubic-bezier(0.2, 0.8, 0.2, 1);
}
@keyframes slideUp { from { opacity: 0; transform: translateY(30px); } to { opacity: 1; transform: translateY(0); } }

/* 头部 */
.header-section { margin-bottom: 24px; }
.category-tag {
  display: inline-block; padding: 4px 10px; background: rgba(99, 102, 241, 0.1);
  color: var(--primary); font-size: 12px; font-weight: 700; border-radius: 8px; margin-bottom: 12px;
}
.article-title { font-size: 26px; font-weight: 800; color: var(--text-main); line-height: 1.3; display: block; margin-bottom: 12px; }
.meta-row { display: flex; gap: 15px; }
.meta-item { font-size: 12px; color: #94a3b8; }

.divider { height: 1px; background: linear-gradient(90deg, rgba(0,0,0,0.05), transparent); margin-bottom: 24px; }

/* 摘要 */
.summary-box {
  background: #f8fafc; border-left: 4px solid var(--primary);
  padding: 16px; border-radius: 4px 12px 12px 4px; margin-bottom: 30px;
}
.summary-label { font-size: 11px; font-weight: 800; color: var(--primary); text-transform: uppercase; letter-spacing: 1px; display: block; margin-bottom: 4px; }
.summary-content { font-size: 14px; color: #475569; line-height: 1.6; font-style: italic; }

/* 正文 */
.content-section { margin-bottom: 40px; }
.article-content {
  font-size: 17px; color: #334155; line-height: 2;
  letter-spacing: 0.02em; white-space: pre-wrap; word-break: break-all;
}

/* 版权 */
.copyright-notice {
  border-top: 1px dashed #e2e8f0; padding-top: 20px; text-align: center;
}
.copyright-notice text { display: block; font-size: 11px; color: #cbd5e1; margin-bottom: 4px; }

/* PC 适配 */
@media screen and (min-width: 800px) {
  .nav-bar { padding-top: 30px; padding-bottom: 30px; }
  .article-card { width: 720px; margin: 40px auto; padding: 60px 80px; }
  .article-title { font-size: 36px; }
}
</style>