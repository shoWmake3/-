<template>
  <view class="container">
    <view class="nav-bar">
      <text class="close-btn" @click="goBack">取消</text>
      <button class="publish-btn" @click="handlePublish" :disabled="uploading">发布</button>
    </view>

    <view class="content-box">
      <view class="media-grid">
        <view class="media-item" v-for="(url, index) in mediaList" :key="index">
          <video v-if="isVideo(url)" :src="url" class="media-content" :controls="false"></video>
          <image v-else :src="url" mode="aspectFill" class="media-content" @click="previewImage(index)"></image>
          <view class="delete-tag" @click.stop="removeMedia(index)">×</view>
        </view>
        <view class="add-btn" @click="handleSelectMedia" v-if="mediaList.length < 9">
          <text class="plus">+</text>
        </view>
      </view>

      <input class="title-input" v-model="form.title" placeholder="填写标题会有更多赞哦~" />
      <textarea class="content-input" v-model="form.content" placeholder="分享你的留日生活、避坑指南..." maxlength="1000"></textarea>

      <view class="option-item">
        <view class="left">
          <text class="icon">📍</text>
          <text class="label">所在位置</text>
        </view>
        <view class="right" style="flex: 1; display: flex; justify-content: flex-end; align-items: center;">
          <input v-model="form.locationName" placeholder="点击图标选择 或 手动输入"
            style="text-align: right; font-size: 14px; color: #333; flex: 1; margin-right: 10px;" />
          <view @click="chooseLocation" style="padding: 5px;">
            <text class="arrow" style="font-size: 18px; color: #007aff;">🗺️</text>
          </view>
        </view>
      </view>

    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue';

const mediaList = ref([]);
const uploading = ref(false);

const form = reactive({
  title: '',
  content: '',
  // ⭐ LBS 字段
  locationName: '',
  latitude: null,
  longitude: null
});

// 1. 选择媒体 (图片/视频)
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

// 2. 上传文件到 MinIO
// 2. 上传文件到 MinIO
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
        // 简单判断：如果以 http 开头，说明直接返回了地址
        if (rawData.startsWith('http')) {
          mediaList.value.push(rawData);
        } else {
          // 否则尝试解析 JSON
          try {
            const data = JSON.parse(rawData);
            if (data.code === 200 || data.data) {
              const url = data.data || data;
              mediaList.value.push(url);
            }
          } catch (e) {
            console.error('解析失败', e);
            // 如果解析失败，但它又不以 http 开头，可能是相对路径或者错误信息
            // 如果你的图片服务器返回相对路径 (如 /bucket/xxx.jpg)，也可以在这里追加处理
            if (rawData.includes('/')) {
              // 容错处理
              mediaList.value.push(rawData);
            }
          }
        }
        // ⭐⭐ 核心修复结束 ⭐⭐
      },
      complete: () => {
        successCount++;
        if (successCount === paths.length) uploading.value = false;
      }
    });
  });
};

const removeMedia = (index) => mediaList.value.splice(index, 1);
const isVideo = (url) => url.toLowerCase().endsWith('.mp4') || url.toLowerCase().endsWith('.mov');

// 3. ⭐ LBS 位置选择 (标准版：使用真实地图)
// 打开 OSM 地图选点
const chooseLocation = () => {
  // 监听选点结果
  uni.$once('location-selected', (data) => {
    console.log('选点返回：', data);
    form.locationName = data.name;
    form.latitude = data.latitude;
    form.longitude = data.longitude;
  });

  // 跳转到自制地图页
  uni.navigateTo({
    url: '/pages/publish/osm-map'
  });
};

// 4. 发布帖子
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
      // ⭐ 传给后端 LBS 信息
      locationName: form.locationName,
      latitude: form.latitude,
      longitude: form.longitude
    },
    success: (res) => {
      uni.showToast({ title: '发布成功' });
      setTimeout(() => {
        uni.navigateBack(); // 返回上一页
        // 触发首页刷新 (可选)
        uni.$emit('refreshHome');
      }, 1000);
    },
    fail: () => uni.showToast({ title: '发布失败', icon: 'none' })
  });
};

const goBack = () => uni.navigateBack();
</script>

<style>
.container {
  padding: 44px 20px 20px;
  background: #fff;
  min-height: 100vh;
}

.nav-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.close-btn {
  font-size: 16px;
  color: #333;
}

.publish-btn {
  background: #ff2442;
  color: #fff;
  font-size: 14px;
  border-radius: 20px;
  padding: 0 20px;
  height: 32px;
  line-height: 32px;
  margin: 0;
}

.publish-btn[disabled] {
  opacity: 0.5;
}

.media-grid {
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.media-item,
.add-btn {
  width: 31%;
  padding-bottom: 31%;
  position: relative;
  margin-right: 2%;
  margin-bottom: 10px;
  border-radius: 8px;
  overflow: hidden;
}

.media-item:nth-child(3n) {
  margin-right: 0;
}

.add-btn {
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.add-btn .plus {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 40px;
  color: #ccc;
}

.media-content {
  position: absolute;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.delete-tag {
  position: absolute;
  top: 5px;
  right: 5px;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  text-align: center;
  line-height: 18px;
  font-size: 14px;
  z-index: 10;
}

.title-input {
  font-size: 18px;
  font-weight: bold;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
  margin-bottom: 10px;
}

.content-input {
  width: 100%;
  height: 150px;
  font-size: 15px;
  line-height: 1.6;
}

/* ⭐ 选项列表样式 */
.option-list {
  margin-top: 20px;
  border-top: 1px solid #f5f5f5;
  padding-top: 10px;
}

.option-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid #f5f5f5;
}

.left {
  display: flex;
  align-items: center;
}

.icon {
  font-size: 18px;
  margin-right: 10px;
}

.label {
  font-size: 15px;
  color: #333;
}

.right {
  display: flex;
  align-items: center;
}

.value {
  font-size: 14px;
  color: #999;
  margin-right: 5px;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.value.highlight {
  color: #007aff;
  font-weight: bold;
}

.value.placeholder {
  color: #ccc;
}

.arrow {
  font-size: 14px;
  color: #ccc;
}
</style>