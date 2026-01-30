<template>
  <div class="outer-wrapper">
    <div class="container">
      <div class="editor-box">
        <input class="title-input" v-model="form.title" placeholder="填写标题会有更多人看哦~" />
        <textarea class="content-input" v-model="form.content" placeholder="分享你在日本的新鲜事..." maxlength="1000"></textarea>
        
        <div class="image-uploader">
          <div class="img-item" v-for="(url, index) in imageList" :key="index">
             
             <video 
                v-if="isVideo(url)" 
                :src="url" 
                class="preview-img"
                :controls="false" 
                :show-center-play-btn="false"
             ></video>
             
             <image 
                v-else 
                :src="url" 
                mode="aspectFill" 
                class="preview-img"
             ></image>

             <div class="del-btn" @click="removeImage(index)">x</div>
             <div class="video-tag" v-if="isVideo(url)">▶</div>
          </div>
          
          <div class="upload-btn" @click="chooseMedia" v-if="imageList.length < 9">
            <text class="plus">+</text>
          </div>
        </div>
      </div>

      <div class="option-list">
        <div class="option-item">
          <text># 添加话题</text>
          <picker mode="selector" :range="categories" range-key="label" @change="onCategoryChange">
             <text class="picker-val">{{ categoryMap[form.category] || '选择分类' }} ></text>
          </picker>
        </div>
        <div class="option-item">
          <text>📍 添加地点</text>
          <text class="picker-val">{{ locationName || '点击获取' }} ></text>
        </div>
      </div>

      <div class="submit-btn" @click="submitPost">发布笔记</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';

const form = reactive({
  title: '',
  content: '',
  category: 'life'
});

const imageList = ref([]); 

const categories = [
  { label: '生活', value: 'life' },
  { label: '学习', value: 'study' },
  { label: '工作', value: 'work' }
];
const categoryMap = { life: '生活', study: '学习', work: '工作' };
const locationName = ref('');
const locationCoords = ref({ lat: null, lng: null });

uni.getLocation({
  type: 'wgs84',
  success: (res) => {
    locationCoords.value = { lat: res.latitude, lng: res.longitude };
    locationName.value = '当前位置'; 
  }
});

const onCategoryChange = (e) => {
  const index = e.detail.value;
  form.category = categories[index].value;
};

// 辅助函数：判断是不是视频
const isVideo = (url) => {
  if (!url) return false;
  const lowerUrl = url.toLowerCase();
  return lowerUrl.endsWith('.mp4') || lowerUrl.endsWith('.mov') || lowerUrl.endsWith('.avi');
};

// ⭐ 核心修复：用兼容性最好的 ActionSheet 替代 chooseMedia
const chooseMedia = () => {
  uni.showActionSheet({
    itemList: ['选择图片', '选择视频'], // 弹出的菜单选项
    success: (res) => {
      if (res.tapIndex === 0) {
        // --- 用户选了图片 ---
        uni.chooseImage({
          count: 9 - imageList.value.length,
          sourceType: ['album', 'camera'],
          success: (imgRes) => {
            // 图片返回的是数组 tempFilePaths
            imgRes.tempFilePaths.forEach(path => uploadToMinIO(path));
          }
        });
      } else if (res.tapIndex === 1) {
        // --- 用户选了视频 ---
        uni.chooseVideo({
          sourceType: ['album', 'camera'],
          compressed: true,
          maxDuration: 60, // 限制60秒
          success: (videoRes) => {
            // 视频返回的是单个对象，路径在 tempFilePath
            const path = videoRes.tempFilePath;
            uploadToMinIO(path);
          }
        });
      }
    },
    fail: (res) => {
      console.log(res.errMsg);
    }
  });
};

const uploadToMinIO = (filePath) => {
  if (filePath.size > 200 * 1024 * 1024) {
    return uni.showToast({ title: '视频不能超过200MB', icon: 'none' });
}
  uni.showLoading({ title: '上传中...' });
  // 真机调试请把 localhost 改为电脑 IP
  uni.uploadFile({
    url: 'http://localhost:8080/oss/upload',
    filePath: filePath,
    name: 'file',
    success: (res) => {
      uni.hideLoading();
      if (res.statusCode === 200) {
        imageList.value.push(res.data);
      } else {
        uni.showToast({ title: '上传失败', icon: 'none' });
      }
    },
    fail: () => {
      uni.hideLoading();
      uni.showToast({ title: '网络错误', icon: 'none' });
    }
  });
};

const removeImage = (index) => {
  imageList.value.splice(index, 1);
};

const submitPost = () => {
  if (!form.title || !form.content) return uni.showToast({ title: '写点什么吧', icon: 'none' });
  
  const token = uni.getStorageSync('token');
  const postData = {
    ...form,
    latitude: locationCoords.value.lat,
    longitude: locationCoords.value.lng,
    locationName: locationName.value,
    mediaUrls: JSON.stringify(imageList.value) 
  };

  uni.request({
    url: 'http://localhost:8080/post/publish',
    method: 'POST',
    header: { 'Authorization': token },
    data: postData,
    success: (res) => {
      if (res.statusCode === 200) {
        uni.showToast({ title: '发布成功', icon: 'success' });
        setTimeout(() => {
          uni.switchTab({ url: '/pages/index/index' });
        }, 1000);
      }
    }
  });
};
</script>

<style>
/* 保持刚才优化的 PC 端样式 */
.outer-wrapper { background-color: #f5f5f5; min-height: 100vh; display: flex; justify-content: center; }

.editor-box { margin-bottom: 20px; }
.title-input { font-size: 18px; font-weight: bold; border-bottom: 1px solid #eee; padding: 10px 0; margin-bottom: 10px; }
.content-input { width: 100%; height: 150px; font-size: 15px; line-height: 1.5; }

.image-uploader { display: flex; flex-wrap: wrap; margin-top: 10px; }
.img-item { width: 31%; height: 100px; margin-right: 2%; margin-bottom: 10px; position: relative; border-radius: 8px; overflow: hidden; background: #000; }
.img-item:nth-child(3n) { margin-right: 0; }
.preview-img { width: 100%; height: 100%; background: #f0f0f0; }

.del-btn { position: absolute; top: 0; right: 0; background: rgba(0,0,0,0.5); color: #fff; width: 20px; height: 20px; text-align: center; line-height: 18px; font-size: 12px; border-bottom-left-radius: 8px; cursor: pointer; z-index: 10; }
.video-tag { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); color: #fff; font-size: 20px; text-shadow: 0 1px 3px rgba(0,0,0,0.5); pointer-events: none; }

.upload-btn { width: 31%; height: 100px; background: #f8f8f8; border-radius: 8px; display: flex; align-items: center; justify-content: center; border: 1px dashed #ddd; cursor: pointer; }
.plus { font-size: 40px; color: #ccc; font-weight: 200; }

.option-list { border-top: 1px solid #eee; border-bottom: 1px solid #eee; padding: 10px 0; }
.option-item { display: flex; justify-content: space-between; align-items: center; padding: 15px 0; font-size: 14px; color: #333; cursor: pointer; }
.picker-val { color: #999; }
.submit-btn { margin-top: 30px; background: #ff2442; color: #fff; text-align: center; padding: 12px; border-radius: 25px; font-weight: bold; font-size: 16px; box-shadow: 0 4px 10px rgba(255, 36, 66, 0.3); cursor: pointer; }
.submit-btn:active { opacity: 0.9; }
</style>