<template>
  <view class="container">
    <view class="search-bar">
      <input class="search-input" v-model="keyword" placeholder="搜索百科知识..." @confirm="doSearch" />
      <button class="search-btn" @click="doSearch">搜索</button>
    </view>

    <view class="category-grid" v-if="!keyword">
      <view class="cat-item" v-for="cat in categories" :key="cat" @click="filterByCat(cat)">
        <text class="cat-icon">📚</text>
        <text class="cat-name">{{ cat }}</text>
      </view>
    </view>

    <view class="wiki-list">
      <view class="section-title">{{ keyword ? '搜索结果' : '最新词条' }}</view>
      <view class="wiki-item" v-for="(item, index) in list" :key="index" @click="goToDetail(item)">
        <view class="wiki-info">
          <text class="wiki-title">{{ item.title }}</text>
          <text class="wiki-summary">{{ item.summary }}</text>
          <view class="wiki-meta">
            <text class="tag">{{ item.category }}</text>
            <text class="views">{{ item.viewCount }} 阅读</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';

const keyword = ref('');
const list = ref([]);
const categories = ['签证办理', '租房攻略', '交通出行', '学术研究', '打工兼职'];

onShow(() => {
  fetchList();
});

const fetchList = () => {
  const url = keyword.value 
    ? `http://localhost:8080/wiki/search?keyword=${keyword.value}`
    : `http://localhost:8080/wiki/list`;
    
  uni.request({
    url: url,
    success: (res) => { list.value = res.data; }
  });
};

const doSearch = () => { fetchList(); };
const filterByCat = (cat) => { keyword.value = cat; fetchList(); };

const goToDetail = (item) => {
  // 这里简单处理，实际可复用 post-detail 或新建 wiki-detail
  uni.showModal({ title: item.title, content: item.content, showCancel: false });
};
</script>

<style>
.container { padding: 15px; background: #fff; min-height: 100vh; }
.search-bar { display: flex; margin-bottom: 20px; }
.search-input { flex: 1; background: #f5f5f5; height: 36px; padding: 0 15px; border-radius: 18px; font-size: 14px; }
.search-btn { font-size: 14px; background: #007aff; color: #fff; height: 36px; line-height: 36px; border-radius: 18px; margin-left: 10px; }

.category-grid { display: flex; flex-wrap: wrap; justify-content: space-between; margin-bottom: 25px; }
.cat-item { width: 48%; background: #f0f8ff; padding: 15px; border-radius: 8px; margin-bottom: 10px; display: flex; align-items: center; box-sizing: border-box; }
.cat-icon { font-size: 20px; margin-right: 10px; }
.cat-name { font-weight: bold; color: #333; }

.section-title { font-size: 16px; font-weight: bold; border-left: 4px solid #007aff; padding-left: 10px; margin-bottom: 15px; }
.wiki-item { padding: 15px 0; border-bottom: 1px solid #eee; }
.wiki-title { font-size: 16px; font-weight: bold; margin-bottom: 5px; display: block; }
.wiki-summary { font-size: 13px; color: #666; line-height: 1.5; margin-bottom: 8px; display: -webkit-box; -webkit-line-clamp: 2; overflow: hidden; }
.wiki-meta { display: flex; justify-content: space-between; font-size: 12px; color: #999; }
.tag { background: #eef6ff; color: #007aff; padding: 2px 6px; border-radius: 4px; }
</style>