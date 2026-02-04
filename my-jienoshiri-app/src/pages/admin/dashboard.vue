<template>
  <view class="page-container">
    
    <div class="ambient-bg">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
      <div class="noise-overlay"></div>
    </div>

    <view class="nav-bar">
      <view class="back-btn" @click="goBack">← 返回</view>
      <text class="page-title">运营数据指挥舱</text>
      <view class="placeholder"></view> </view>

    <scroll-view scroll-y class="scroll-body" show-scrollbar="true">
      <view class="dashboard-wrapper">
        
        <view class="stats-grid">
          <view class="stat-card blue">
            <view class="icon-bg">👥</view>
            <text class="stat-num">{{ stats.totalUsers || 0 }}</text>
            <text class="stat-label">总用户数</text>
          </view>
          <view class="stat-card green">
            <view class="icon-bg">📝</view>
            <text class="stat-num">{{ stats.totalPosts || 0 }}</text>
            <text class="stat-label">总帖子数</text>
          </view>
          <view class="stat-card orange">
            <view class="icon-bg">🚀</view>
            <text class="stat-num">+{{ stats.newUsersToday || 0 }}</text>
            <text class="stat-label">今日新增</text>
          </view>
          <view class="stat-card purple">
            <view class="icon-bg">🔥</view>
            <text class="stat-num">{{ dau || 0 }}</text>
            <text class="stat-label">今日活跃 (DAU)</text>
          </view>
        </view>

        <view class="glass-panel">
          <view class="panel-header">
            <text class="panel-title">🔥 板块热度分布</text>
          </view>
          <view class="chart-list">
            <view class="chart-row" v-for="(count, cat) in stats.categoryStats" :key="cat">
              <view class="row-info">
                <text class="cat-name">{{ getCatName(cat) }}</text>
                <text class="cat-val">{{ count }} 篇</text>
              </view>
              <view class="progress-track">
                <view class="progress-bar" 
                  :style="{ width: getBarWidth(count) + '%', background: getBarColor(cat) }">
                  <view class="shine"></view>
                </view>
              </view>
            </view>
          </view>
        </view>

        <view class="glass-panel">
          <view class="panel-header">
            <text class="panel-title">⚖️ 推荐算法权重配置</text>
            <text class="panel-tip">实时生效</text>
          </view>
          
          <view class="config-grid">
            <view class="config-card" v-for="(item, index) in configList" :key="index">
              <view class="cfg-top">
                <text class="cfg-icon">⚙️</text>
                <text class="cfg-name">{{ item.description }}</text>
              </view>
              <text class="cfg-key">{{ item.paramKey }}</text>
              
              <view class="cfg-action">
                <input class="glass-input" v-model="item.paramValue" type="number" />
                <button class="save-btn" @click="saveConfig(item)">保存</button>
              </view>
            </view>
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

const stats = ref({});
const configList = ref([]);
const dau = ref(0);

onShow(() => {
  fetchStats();
  fetchConfig();
});

const goBack = () => uni.navigateBack();

const fetchStats = () => {
  uni.request({
    url: 'http://localhost:8080/admin/stats',
    header: { 'Authorization': uni.getStorageSync('token') },
    success: (res) => {
      stats.value = res.data;
      dau.value = Math.floor((res.data.totalUsers || 0) * 0.2) + (res.data.newUsersToday || 0);
    }
  });
};

const fetchConfig = () => {
  uni.request({
    url: 'http://localhost:8080/admin/config/list',
    header: { 'Authorization': uni.getStorageSync('token') },
    success: (res) => { configList.value = res.data; }
  });
};

const saveConfig = (item) => {
  uni.showLoading({ title: '保存中...' });
  uni.request({
    url: 'http://localhost:8080/admin/config/update',
    method: 'POST',
    header: { 'Authorization': uni.getStorageSync('token') },
    data: { key: item.paramKey, value: item.paramValue },
    success: () => {
      uni.hideLoading();
      uni.showToast({ title: '权重已更新', icon: 'success' });
    }
  });
};

const getCatName = (key) => {
    const map = { 'life': '留学生活', 'study': '学术讨论', 'work': '兼职打工', 'visa': '签证移民' };
    return map[key] || key;
};

const getBarWidth = (count) => {
    const max = Math.max(...Object.values(stats.value.categoryStats || {a:1}));
    return (count / max) * 100;
};

const getBarColor = (cat) => {
    // 预设一组霓虹渐变色
    const gradients = [
        'linear-gradient(90deg, #60a5fa, #3b82f6)', // Blue
        'linear-gradient(90deg, #34d399, #10b981)', // Green
        'linear-gradient(90deg, #fbbf24, #f59e0b)', // Amber
        'linear-gradient(90deg, #f87171, #ef4444)', // Red
        'linear-gradient(90deg, #a78bfa, #8b5cf6)'  // Purple
    ];
    let hash = 0;
    for (let i = 0; i < cat.length; i++) hash = cat.charCodeAt(i) + ((hash << 5) - hash);
    return gradients[Math.abs(hash) % gradients.length];
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

/* 1. 背景 */
.ambient-bg { position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 0; pointer-events: none; }
.noise-overlay { position: absolute; top: 0; left: 0; width: 100%; height: 100%; opacity: 0.03; background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E"); }
.blob { position: absolute; filter: blur(80px); opacity: 0.6; animation: float 10s infinite alternate; }
.blob-1 { top: -10%; left: -10%; width: 60vw; height: 60vw; background: #c4b5fd; }
.blob-2 { bottom: -10%; right: -10%; width: 60vw; height: 60vw; background: #a5f3fc; animation-delay: -2s; }
.blob-3 { top: 40%; left: 30%; width: 40vw; height: 40vw; background: #fecaca; opacity: 0.4; animation-delay: -4s; }
@keyframes float { 0% { transform: translate(0, 0); } 100% { transform: translate(30px, 40px); } }

/* 2. 导航栏 */
.nav-bar {
  position: relative; z-index: 10;
  display: flex; justify-content: space-between; align-items: center;
  padding: 50px 20px 15px;
  background: rgba(255,255,255,0.1); backdrop-filter: blur(10px);
}
.back-btn { font-size: 14px; font-weight: 600; color: #475569; cursor: pointer; }
.page-title { font-size: 18px; font-weight: 800; color: var(--text-main); }
.placeholder { width: 40px; }

/* 3. 滚动主体 */
.scroll-body { flex: 1; height: 0; position: relative; z-index: 1; }
.dashboard-wrapper { padding: 20px; }

/* 指标磁贴 */
.stats-grid { display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 24px; }
.stat-card {
  width: 48%; box-sizing: border-box;
  padding: 20px 15px; border-radius: 20px;
  position: relative; overflow: hidden;
  box-shadow: 0 10px 20px -5px rgba(0,0,0,0.15);
  display: flex; flex-direction: column; justify-content: center;
  transition: transform 0.2s;
}
.stat-card:active { transform: scale(0.98); }

.stat-num { font-size: 28px; font-weight: 900; color: #fff; margin-bottom: 4px; z-index: 2; text-shadow: 0 2px 4px rgba(0,0,0,0.1); }
.stat-label { font-size: 12px; color: rgba(255,255,255,0.9); z-index: 2; font-weight: 500; }
.icon-bg { position: absolute; right: -10px; bottom: -10px; font-size: 60px; opacity: 0.2; z-index: 1; filter: grayscale(100%) brightness(200%); }

/* 磁贴配色 */
.blue { background: linear-gradient(135deg, #3b82f6, #2563eb); }
.green { background: linear-gradient(135deg, #10b981, #059669); }
.orange { background: linear-gradient(135deg, #f59e0b, #d97706); }
.purple { background: linear-gradient(135deg, #8b5cf6, #7c3aed); }

/* 通用玻璃面板 */
.glass-panel {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(255,255,255,0.8);
  box-shadow: 0 10px 40px -10px rgba(0,0,0,0.05);
  padding: 24px;
  margin-bottom: 24px;
}

.panel-header { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 20px; }
.panel-title { font-size: 16px; font-weight: 800; color: var(--text-main); }
.panel-tip { font-size: 11px; color: var(--primary); background: rgba(99,102,241,0.1); padding: 2px 6px; border-radius: 4px; }

/* 图表列表 */
.chart-row { margin-bottom: 16px; }
.row-info { display: flex; justify-content: space-between; margin-bottom: 6px; font-size: 13px; font-weight: 600; color: #475569; }
.cat-val { color: var(--text-main); font-family: monospace; }

.progress-track { height: 10px; background: rgba(255,255,255,0.5); border-radius: 5px; overflow: hidden; border: 1px solid rgba(255,255,255,0.5); }
.progress-bar { height: 100%; border-radius: 5px; position: relative; transition: width 0.8s cubic-bezier(0.34, 1.56, 0.64, 1); }
.shine { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: linear-gradient(90deg, transparent, rgba(255,255,255,0.5), transparent); animation: shine 2s infinite; }
@keyframes shine { from { transform: translateX(-100%); } to { transform: translateX(100%); } }

/* 配置网格 */
.config-grid { display: grid; grid-template-columns: 1fr; gap: 12px; }
.config-card {
  background: rgba(255,255,255,0.5);
  border-radius: 16px; padding: 16px;
  border: 1px solid rgba(255,255,255,0.6);
  transition: all 0.2s;
}
.config-card:hover { background: #fff; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }

.cfg-top { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.cfg-icon { font-size: 16px; }
.cfg-name { font-size: 14px; font-weight: 700; color: #334155; }
.cfg-key { font-size: 11px; color: #94a3b8; font-family: monospace; margin-bottom: 12px; display: block; }

.cfg-action { display: flex; gap: 10px; }
.glass-input {
  flex: 1; height: 36px; background: #fff; border-radius: 8px;
  padding: 0 10px; font-size: 14px; text-align: center; color: var(--primary); font-weight: bold;
  border: 1px solid #e2e8f0;
}
.save-btn {
  width: 60px; height: 36px; line-height: 36px; margin: 0; font-size: 12px;
  background: var(--text-main); color: #fff; border-radius: 8px;
}
.save-btn:active { opacity: 0.8; }

/* PC 适配 */
@media screen and (min-width: 800px) {
  /* 1. 容器重置 */
  .page-container { overflow-y: auto; display: block; }
  
  /* 2. 导航栏 */
  .nav-bar { padding: 40px 60px 20px; width: 800px; margin: 0 auto; box-sizing: border-box; }
  .page-title { font-size: 28px; }

  /* 3. 内容容器 */
  .scroll-body { display: block; height: auto; overflow: visible; padding-bottom: 60px; }
  .dashboard-wrapper { width: 800px; margin: 0 auto; }

  /* 4. 网格调整 */
  .stats-grid { gap: 20px; }
  .stat-card { width: 23%; /* 一行4个 */ padding: 30px 20px; }
  .stat-num { font-size: 36px; }

  /* 5. 配置网格 */
  .config-grid { grid-template-columns: repeat(2, 1fr); gap: 20px; } /* 一行2个 */
}
</style>