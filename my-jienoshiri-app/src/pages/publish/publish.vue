<template>
  <view class="page-container">
    
    <div class="ambient-bg">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
      <div class="noise-overlay"></div>
    </div>

    <view class="nav-bar">
      <button class="publish-btn" @click="handlePublish" :disabled="uploading" :class="{ 'loading': uploading }">
        {{ uploading ? '上传中...' : '发布' }}
      </button>
    </view>

    <scroll-view scroll-y class="scroll-body" show-scrollbar="true">
      <view class="glass-card">
        
        <view class="media-grid">
          <view class="media-item" v-for="(url, index) in mediaList" :key="index">
            <video v-if="isVideo(url)" :src="url" class="media-content" :controls="false"></video>
            <image v-else :src="url" mode="aspectFill" class="media-content" @click="previewImage(index)"></image>
            <view class="delete-btn" @click.stop="removeMedia(index)">×</view>
          </view>
          
          <view class="add-btn" @click="handleSelectMedia" v-if="mediaList.length < 9">
            <view class="plus-icon">+</view>
            <text class="add-text">图片/视频</text>
          </view>
        </view>

        <view class="input-section">
          <input class="title-input" v-model="form.title" placeholder="填写标题会有更多赞哦~" 
            placeholder-style="color:#94a3b8; font-weight:400" />
          
          <view class="divider"></view>
          
          <textarea class="content-input" v-model="form.content" placeholder="分享你的留日生活、避坑指南..." 
            maxlength="1000" placeholder-style="color:#94a3b8; font-weight:400"></textarea>
        </view>

        <view class="location-card" @click="chooseLocation">
          <view class="loc-left">
            <view class="loc-icon-box">📍</view>
            <text class="loc-label">所在位置</text>
          </view>
          
          <view class="loc-right">
            <text class="loc-val" :class="{ 'has-val': form.locationName }">
              {{ form.locationName || '点击选择位置' }}
            </text>
            <text class="arrow">›</text>
          </view>
        </view>

      </view>
      
      <view style="height: 40px;"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue';

const mediaList = ref([]);
const uploading = ref(false);

const form = reactive({
  title: '',
  content: '',
  locationName: '',
  latitude: null,
  longitude: null
});

const handleSelectMedia = () => {
  uni.showActionSheet({
    itemList: ['相册/拍摄 (图片)', '相册/拍摄 (视频)'],
    success: (res) => {
      if (res.tapIndex === 0) chooseImage();
      else chooseVideo();
    }
  });
};

const chooseImage = () => {
  uni.chooseImage({
    count: 9 - mediaList.value.length,
    success: (res) => { uploadFiles(res.tempFilePaths, 'image'); }
  });
};

const chooseVideo = () => {
  uni.chooseVideo({
    sourceType: ['camera', 'album'],
    success: (res) => { uploadFiles([res.tempFilePath], 'video'); }
  });
};

const uploadFiles = (paths, type) => {
  uploading.value = true;
  let successCount = 0;
  paths.forEach(path => {
    uni.uploadFile({
      url: 'http://localhost:8080/oss/upload',
      filePath: path,
      name: 'file',
      success: (uploadRes) => {
        const rawData = uploadRes.data;
        if (rawData.startsWith('http')) {
          mediaList.value.push(rawData);
        } else {
          try {
            const data = JSON.parse(rawData);
            if (data.code === 200 || data.data) {
              mediaList.value.push(data.data || data);
            }
          } catch (e) {
            if (rawData.includes('/')) mediaList.value.push(rawData);
          }
        }
      },
      complete: () => {
        successCount++;
        if (successCount === paths.length) uploading.value = false;
      }
    });
  });
};

const removeMedia = (index) => mediaList.value.splice(index, 1);
const isVideo = (url) => url && (url.toLowerCase().endsWith('.mp4') || url.toLowerCase().endsWith('.mov'));

const chooseLocation = () => {
  uni.$once('location-selected', (data) => {
    form.locationName = data.name;
    form.latitude = data.latitude;
    form.longitude = data.longitude;
  });
  uni.navigateTo({ url: '/pages/publish/osm-map' });
};

const handlePublish = () => {
  if (!form.content && mediaList.value.length === 0) {
    return uni.showToast({ title: '写点什么吧~', icon: 'none' });
  }
  const token = uni.getStorageSync('token');
  uni.request({
    url: 'http://localhost:8080/post/publish',
    method: 'POST',
    header: { 'Authorization': token },
    data: {
      title: form.title,
      content: form.content,
      mediaUrls: JSON.stringify(mediaList.value),
      locationName: form.locationName,
      latitude: form.latitude,
      longitude: form.longitude
    },
    success: (res) => {
      uni.showToast({ title: '发布成功' });
      setTimeout(() => {
        uni.navigateBack();
        uni.$emit('refreshHome');
      }, 1000);
    },
    fail: () => uni.showToast({ title: '发布失败', icon: 'none' })
  });
};

const goBack = () => uni.navigateBack();
const previewImage = (index) => {
    uni.previewImage({ current: index, urls: mediaList.value.filter(u => !isVideo(u)) })
}
</script>

<style>
:root {
  --primary: #6366f1;
  --bg-page: #f8fafc;
  --text-main: #1e293b;
  --glass-border: rgba(255, 255, 255, 0.6);
}

/* =================================================================
   📱 手机端样式 (默认)
   ================================================================= */
.page-container {
  height: 100vh;
  display: flex; flex-direction: column;
  position: relative; overflow: hidden;
}

/* 动态背景 */
.ambient-bg {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 0; pointer-events: none; background: #f8fafc;
}
.noise-overlay { position: absolute; top: 0; left: 0; width: 100%; height: 100%; opacity: 0.03; background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E"); }
.blob { position: absolute; filter: blur(80px); opacity: 0.6; animation: float 10s infinite alternate; }
.blob-1 { top: -10%; left: -10%; width: 60vw; height: 60vw; background: #c4b5fd; }
.blob-2 { bottom: -10%; right: -10%; width: 70vw; height: 70vw; background: #a5f3fc; animation-delay: -2s; }
.blob-3 { top: 40%; left: 30%; width: 50vw; height: 50vw; background: #fecaca; opacity: 0.4; animation-delay: -4s; }
@keyframes float { 0% { transform: translate(0, 0); } 100% { transform: translate(30px, 40px); } }

/* 导航栏 */
.nav-bar {
  position: relative; z-index: 100;
  display: flex; justify-content: space-between; align-items: center;
  padding: 50px 20px 15px;
  background: rgba(255,255,255,0.1); backdrop-filter: blur(10px);
}
.close-btn { font-size: 16px; color: #475569; font-weight: 500; cursor: pointer; }
.page-title { font-size: 17px; font-weight: 700; color: var(--text-main); }
.publish-btn {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff; font-size: 13px; font-weight: 600;
  border-radius: 20px; padding: 0 18px; height: 32px; line-height: 32px; margin: 0;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3); transition: all 0.2s;
}
.publish-btn:active { transform: scale(0.95); opacity: 0.9; }
.publish-btn[disabled] { opacity: 0.6; background: #cbd5e1; box-shadow: none; color: #64748b; }

/* 滚动主体 */
.scroll-body { flex: 1; height: 0; position: relative; z-index: 1; padding: 20px; box-sizing: border-box; }

/* 玻璃拟态卡片 (手机端) */
.glass-card {
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(20px); -webkit-backdrop-filter: blur(20px);
  border-radius: 24px; border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 10px 40px -10px rgba(0,0,0,0.05);
  padding: 20px;
  width: 100%; /* 手机端撑满 */
  box-sizing: border-box;
  animation: slideUp 0.5s ease-out;
}
@keyframes slideUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }

/* 媒体网格 */
.media-grid { display: flex; flex-wrap: wrap; margin-bottom: 10px; gap: 10px; }
.media-item, .add-btn { width: 31%; aspect-ratio: 1; border-radius: 16px; overflow: hidden; position: relative; }
.media-content { width: 100%; height: 100%; object-fit: cover; }
.delete-btn { position: absolute; top: 6px; right: 6px; width: 20px; height: 20px; background: rgba(0,0,0,0.4); backdrop-filter: blur(4px); border-radius: 50%; color: #fff; text-align: center; line-height: 18px; font-size: 14px; }
.add-btn { background: #f1f5f9; border: 2px dashed #cbd5e1; box-sizing: border-box; display: flex; flex-direction: column; align-items: center; justify-content: center; transition: all 0.2s; color: #94a3b8; }
.add-btn:active { background: #e2e8f0; border-color: #94a3b8; }
.plus-icon { font-size: 32px; font-weight: 300; margin-bottom: -4px; }
.add-text { font-size: 10px; }

/* 输入区 */
.input-section { margin-top: 10px; }
.title-input { font-size: 18px; font-weight: 700; color: var(--text-main); padding: 12px 0; height: 48px; }
.divider { height: 1px; background: rgba(0,0,0,0.05); margin-bottom: 12px; }
.content-input { width: 100%; height: 160px; font-size: 15px; color: #334155; line-height: 1.6; }

/* LBS 定位卡片 */
.location-card { margin-top: 10px; background: rgba(255,255,255,0.5); border: 1px solid rgba(255,255,255,0.6); border-radius: 16px; padding: 12px 16px; display: flex; justify-content: space-between; align-items: center; transition: background 0.2s; }
.location-card:active { background: rgba(255,255,255,0.8); }
.loc-left { display: flex; align-items: center; gap: 8px; }
.loc-icon-box { width: 28px; height: 28px; background: rgba(99, 102, 241, 0.1); border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 14px; }
.loc-label { font-size: 14px; font-weight: 600; color: #475569; }
.loc-right { display: flex; align-items: center; gap: 6px; flex: 1; justify-content: flex-end; }
.loc-val { font-size: 13px; color: #94a3b8; max-width: 160px; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.loc-val.has-val { color: #6366f1; font-weight: 500; }
.arrow { color: #cbd5e1; font-size: 16px; }

/* =================================================================
   💻 PC 端专属适配 (Media Query)
   ================================================================= */
@media screen and (min-width: 960px) {
  /* 1. 容器重置 */
  .page-container {
    overflow-y: auto; /* PC端页面滚动 */
    display: block;   /* 取消 flex */
  }

  /* 2. 导航栏加宽 */
  .nav-bar { padding: 20px 60px; }

  /* 3. 滚动区域变容器 */
  .scroll-body {
    height: auto;
    overflow: visible;
    width: 100%;
    display: block; /* 取消 flex */
    padding-bottom: 100px;
  }

  /* 4. 核心卡片绝对居中 */
  .glass-card {
    width: 680px;           /* PC 固定宽度 */
    margin: 40px auto 0;    /* 上40px, 左右auto(居中) */
    padding: 40px;
    box-shadow: 0 20px 60px -10px rgba(0,0,0,0.1);
    /* 覆盖手机端的 width: 100% */
    max-width: none;
  }

  /* 5. 图片变大 */
  .media-item, .add-btn {
    width: 140px; height: 140px; aspect-ratio: auto;
  }
}
</style>