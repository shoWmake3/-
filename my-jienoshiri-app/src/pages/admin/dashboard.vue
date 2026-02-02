<template>
  <view class="container">
    <view class="header">
      <text class="title">📊 运营数据大屏</text>
    </view>

    <view class="card-grid">
      <view class="stat-card blue">
        <text class="num">{{ stats.totalUsers || 0 }}</text>
        <text class="label">总用户数</text>
      </view>
      <view class="stat-card green">
        <text class="num">{{ stats.totalPosts || 0 }}</text>
        <text class="label">总帖子数</text>
      </view>
      <view class="stat-card orange">
        <text class="num">+{{ stats.newUsersToday || 0 }}</text>
        <text class="label">今日新增用户</text>
      </view>
      <view class="stat-card purple">
        <text class="num">{{ dau || 0 }}</text>
        <text class="label">今日活跃 (DAU)</text>
      </view>
    </view>

    <view class="section-box">
      <text class="section-title">板块热度统计</text>
      <view class="chart-box">
        <view class="chart-row" v-for="(count, cat) in stats.categoryStats" :key="cat">
          <text class="chart-label">{{ getCatName(cat) }}</text>
          <view class="bar-bg">
            <view class="bar-fill" :style="{ width: getBarWidth(count) + '%', background: getBarColor(cat) }"></view>
          </view>
          <text class="chart-val">{{ count }}篇</text>
        </view>
      </view>
    </view>

    <view class="section-box">
      <text class="section-title">⚖️ 推荐算法权重配置</text>
      <text class="sub-tip">修改后实时生效，影响首页推荐排序</text>
      
      <view class="config-list">
        <view class="config-item" v-for="(item, index) in configList" :key="index">
          <view class="cfg-info">
            <text class="cfg-key">{{ item.description }}</text>
            <text class="cfg-desc">Key: {{ item.paramKey }}</text>
          </view>
          <view class="cfg-action">
            <input class="cfg-input" v-model="item.paramValue" type="number" />
            <button class="save-btn" @click="saveConfig(item)">保存</button>
          </view>
        </view>
      </view>
    </view>

  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';

const stats = ref({});
const configList = ref([]);
const dau = ref(0); // 模拟一下DAU

onShow(() => {
  fetchStats();
  fetchConfig();
});

const fetchStats = () => {
  uni.request({
    url: 'http://localhost:8080/admin/stats',
    header: { 'Authorization': uni.getStorageSync('token') },
    success: (res) => {
      stats.value = res.data;
      // 简单模拟一下 DAU (通常是总用户的 10%-20%)
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

// 工具函数
const getCatName = (key) => {
    const map = { 'life': '留学生活', 'study': '学术讨论', 'work': '兼职打工', 'visa': '签证移民' };
    return map[key] || key;
};

const getBarWidth = (count) => {
    // 找出最大值作为 100%
    const max = Math.max(...Object.values(stats.value.categoryStats || {a:1}));
    return (count / max) * 100;
};

const getBarColor = (cat) => {
    const colors = ['#007aff', '#4cd964', '#f0ad4e', '#dd524d', '#9c27b0'];
    let hash = 0;
    for (let i = 0; i < cat.length; i++) hash = cat.charCodeAt(i) + ((hash << 5) - hash);
    return colors[Math.abs(hash) % colors.length];
};
</script>

<style>
.container { padding: 20px; background: #f4f6f8; min-height: 100vh; }
.header { margin-bottom: 20px; }
.title { font-size: 24px; font-weight: bold; color: #333; }

/* 1. 指标卡片 */
.card-grid { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 20px; }
.stat-card { width: 48%; background: #fff; padding: 15px; border-radius: 12px; box-sizing: border-box; box-shadow: 0 2px 6px rgba(0,0,0,0.05); }
.num { font-size: 24px; font-weight: bold; display: block; margin-bottom: 5px; }
.label { font-size: 12px; opacity: 0.8; }
.blue { color: #fff; background: linear-gradient(135deg, #007aff, #0056b3); }
.green { color: #fff; background: linear-gradient(135deg, #4cd964, #2ac845); }
.orange { color: #fff; background: linear-gradient(135deg, #f0ad4e, #d98d26); }
.purple { color: #fff; background: linear-gradient(135deg, #9c27b0, #7b1fa2); }

/* 2. 区块通用 */
.section-box { background: #fff; padding: 20px; border-radius: 12px; margin-bottom: 20px; box-shadow: 0 2px 6px rgba(0,0,0,0.03); }
.section-title { font-size: 16px; font-weight: bold; border-left: 4px solid #007aff; padding-left: 10px; margin-bottom: 5px; display: block; }
.sub-tip { font-size: 12px; color: #999; margin-bottom: 15px; display: block; }

/* 3. 条形图 */
.chart-row { display: flex; align-items: center; margin-bottom: 12px; }
.chart-label { width: 70px; font-size: 13px; color: #666; }
.bar-bg { flex: 1; height: 10px; background: #f0f0f0; border-radius: 5px; margin: 0 10px; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 5px; transition: width 0.5s; }
.chart-val { width: 40px; font-size: 12px; color: #333; text-align: right; }

/* 4. 配置列表 */
.config-item { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid #f5f5f5; }
.cfg-info { display: flex; flex-direction: column; }
.cfg-key { font-size: 14px; font-weight: bold; color: #333; }
.cfg-desc { font-size: 12px; color: #999; margin-top: 2px; }
.cfg-action { display: flex; align-items: center; }
.cfg-input { width: 80px; height: 32px; background: #f5f5f5; border-radius: 4px; text-align: center; font-size: 14px; margin-right: 10px; }
.save-btn { font-size: 12px; background: #007aff; color: #fff; height: 32px; line-height: 32px; padding: 0 12px; border-radius: 16px; margin: 0; }
</style>