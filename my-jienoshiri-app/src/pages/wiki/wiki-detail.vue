<template>
  <view class="page-container">
    
    <div class="ambient-bg">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
      <div class="noise-overlay"></div>
    </div>

    <view class="nav-bar-transparent">
      <view class="back-btn-float" @click="goBack">
        <text class="arrow">‹</text>
        <text class="back-text">返回</text>
      </view>
      </view>

    <scroll-view scroll-y class="scroll-body" show-scrollbar="false">
      
      <view class="nav-placeholder"></view>

      <view class="article-wrapper">
        
        <view class="header-section">
          <view class="category-pill"># {{ wiki.category || '综合知识' }}</view>
          <text class="article-title">{{ wiki.title }}</text>
          <view class="meta-row">
            <text class="meta-item">{{ formatTime(wiki.createTime) }}</text>
            <text class="meta-divider">·</text>
            <text class="meta-item">{{ wiki.viewCount || 0 }} 次阅读</text>
          </view>
        </view>

        <view class="summary-glass" v-if="wiki.summary">
          <text class="summary-icon">💡</text>
          <text class="summary-text">{{ wiki.summary }}</text>
        </view>

        <view class="content-body">
          <text class="article-text" user-select>{{ wiki.content }}</text>
        </view>

        <view class="footer-info">
          <text>内容由社区用户 {{ wiki.creatorId || '匿名' }} 贡献</text>
          <text class="copyright">© Jienoshiri Wiki</text>
        </view>

      </view>
      
      <view style="height: 60px;"></view>
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
    reportView(data.id);
  }
});

const goBack = () => uni.navigateBack();

const formatTime = (t) => t ? t.split('T')[0] : '刚刚';

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
  --text-body: #334155;
}

.page-container {
  height: 100vh; display: flex; flex-direction: column;
  background: #f8fafc; position: relative; overflow: hidden;
}

/* --- 1. 背景层 --- */
.ambient-bg { position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 0; pointer-events: none; }
.noise-overlay { position: absolute; top: 0; left: 0; width: 100%; height: 100%; opacity: 0.04; background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E"); }
.blob { position: absolute; filter: blur(90px); opacity: 0.6; animation: float 10s infinite alternate; }
.blob-1 { top: -20%; left: -20%; width: 70vw; height: 70vw; background: #c4b5fd; }
.blob-2 { bottom: -10%; right: -10%; width: 80vw; height: 80vw; background: #a5f3fc; animation-delay: -3s; }
.blob-3 { top: 30%; left: 40%; width: 50vw; height: 50vw; background: #fecaca; opacity: 0.4; animation-delay: -5s; }
@keyframes float { 0% { transform: translate(0, 0); } 100% { transform: translate(30px, 40px); } }

/* --- 2. 导航栏 (透明) --- */
.nav-bar-transparent {
  position: fixed; top: 0; left: 0; width: 100%; z-index: 100;
  /* 不设背景色，完全透明 */
  height: 88px; /* 包含状态栏的高度 */
  pointer-events: none; /* 让导航栏空白处不挡点击 */
}

/* 返回按钮 (悬浮左上角) */
.back-btn-float {
  position: absolute; 
  left: 20px; 
  top: 50px; /* 适配大部分刘海屏位置 */
  
  display: flex; align-items: center; gap: 4px;
  padding: 8px 16px 8px 10px;
  background: rgba(255, 255, 255, 0.6); /* 半透明胶囊背景 */
  backdrop-filter: blur(10px);
  border-radius: 30px;
  border: 1px solid rgba(255,255,255,0.8);
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  pointer-events: auto; /* 按钮可点 */
  transition: transform 0.2s;
  z-index: 101;
}
.back-btn-float:active { transform: scale(0.95); background: rgba(255,255,255,0.8); }

.arrow { font-size: 20px; color: var(--text-main); margin-top: -2px; }
.back-text { font-size: 14px; font-weight: 600; color: var(--text-main); }

/* --- 3. 内容区 --- */
.scroll-body { flex: 1; height: 0; position: relative; z-index: 1; }
.nav-placeholder { height: 100px; } /* 把内容顶下去，露出头部 */

.article-wrapper {
  padding: 0 24px;
  /* 关键：去掉卡片背景，融合页面 */
  background: transparent; 
  animation: fadeIn 0.8s ease-out;
}
@keyframes fadeIn { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }

/* 头部 */
.header-section { margin-bottom: 30px; }
.category-pill {
  display: inline-block; font-size: 12px; font-weight: 700; color: var(--primary);
  background: rgba(99, 102, 241, 0.1); padding: 4px 10px; border-radius: 8px;
  margin-bottom: 16px; letter-spacing: 0.5px;
}
.article-title {
  font-size: 32px; font-weight: 900; color: var(--text-main);
  line-height: 1.3; letter-spacing: -0.5px; margin-bottom: 12px; display: block;
}
.meta-row { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #64748b; font-family: monospace; }
.meta-divider { opacity: 0.5; }

/* 摘要 (唯一保留背景的区域，做成玻璃条) */
.summary-glass {
  display: flex; gap: 12px; align-items: flex-start;
  background: rgba(255, 255, 255, 0.4); /* 极淡 */
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.5);
  padding: 16px; border-radius: 16px;
  margin-bottom: 40px;
}
.summary-icon { font-size: 18px; margin-top: 2px; }
.summary-text { font-size: 15px; color: #475569; line-height: 1.6; font-style: italic; }

/* 正文 */
.article-text {
  font-size: 17px; color: var(--text-body); line-height: 2.0;
  letter-spacing: 0.02em; white-space: pre-wrap; text-align: justify;
}

/* 底部 */
.footer-info {
  margin-top: 60px; padding-top: 30px; border-top: 1px dashed rgba(0,0,0,0.1);
  text-align: center; opacity: 0.6;
}
.footer-info text { display: block; font-size: 12px; color: #64748b; margin-bottom: 6px; }
.copyright { font-weight: 600; letter-spacing: 1px; }

/* PC 适配 */
@media screen and (min-width: 800px) {
  .article-wrapper {
    width: 680px; margin: 0 auto;
    /* PC端文字如果太散，可以稍微加一点点背景聚焦，或者保持通透 */
    /* background: rgba(255,255,255,0.3); padding: 60px; border-radius: 30px; */
  }
  .article-title { font-size: 42px; }
  
  /* PC端返回键微调 */
  .back-btn-float { left: 40px; top: 40px; cursor: pointer; }
}
</style>