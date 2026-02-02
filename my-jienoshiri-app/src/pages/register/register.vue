<template>
  <view class="container">
    <image src="/static/logo.png" class="logo"></image>
    <view class="title">加入我们</view>

    <view class="form-area">
      <input class="input" v-model="form.username" placeholder="请输入账号" />
      <input class="input" v-model="form.password" type="password" placeholder="请输入密码" />
      <input class="input" v-model="form.nickname" placeholder="请输入昵称" />
      
      <view class="label">选择身份：</view>
      <radio-group @change="onIdentityChange" class="radio-group">
        <label class="radio-item" v-for="item in identities" :key="item.value">
          <radio :value="item.value" :checked="form.identityType === item.value" color="#007aff" />
          <text>{{ item.name }}</text>
        </label>
      </radio-group>

      <view class="upload-area" v-if="needProof">
        <view class="label">上传身份证明 (学生证/工作证)：</view>
        <view class="upload-box" @click="chooseImage">
          <image v-if="form.identityProof" :src="form.identityProof" mode="aspectFill" class="preview-img"></image>
          <view v-else class="placeholder">
            <text class="plus">+</text>
            <text class="tip">点击上传</text>
          </view>
        </view>
        <text class="sub-tip">管理员将在后台审核您的材料</text>
      </view>

      <button class="btn" @click="handleRegister" :loading="loading">立即注册</button>
      <view class="link" @click="goToLogin">已有账号？去登录</view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue';

const form = ref({
  username: '',
  password: '',
  nickname: '',
  identityType: 'student',
  identityProof: '' // 存图片URL
});

const loading = ref(false);

const identities = [
  { name: '留学生', value: 'student' },
  { name: '中介', value: 'agent' },
  { name: '打工人', value: 'worker' },
  { name: '游客', value: 'tourist' }
];

// 计算属性：是否需要上传证明
const needProof = computed(() => {
  return form.value.identityType === 'student' || form.value.identityType === 'agent';
});

const onIdentityChange = (e) => {
  form.value.identityType = e.detail.value;
};

const goToLogin = () => {
  uni.navigateBack();
};

// ⭐ 上传图片逻辑
const chooseImage = () => {
  uni.chooseImage({
    count: 1,
    success: (res) => {
      const filePath = res.tempFilePaths[0];
      uni.showLoading({ title: '上传中...' });
      
      uni.uploadFile({
        url: 'http://localhost:8080/oss/upload',
        filePath: filePath,
        name: 'file',
        success: (uploadRes) => {
          uni.hideLoading();
          // 处理后端返回结果 (兼容纯字符串和JSON)
          let url = uploadRes.data;
          try {
             const json = JSON.parse(url);
             if(json.data) url = json.data;
          } catch(e) {}
          
          if (url.startsWith('http')) {
             form.value.identityProof = url;
          } else {
             uni.showToast({ title: '上传失败', icon: 'none' });
          }
        },
        fail: () => {
          uni.hideLoading();
          uni.showToast({ title: '网络错误', icon: 'none' });
        }
      });
    }
  });
};

const handleRegister = () => {
  if (!form.value.username || !form.value.password) return uni.showToast({ title: '请填写完整', icon: 'none' });
  
  // 校验：如果需要证明但没传
  if (needProof.value && !form.value.identityProof) {
      return uni.showToast({ title: '请上传身份证明材料', icon: 'none' });
  }

  loading.value = true;
  uni.request({
    url: 'http://localhost:8080/user/register',
    method: 'POST',
    data: form.value,
    success: (res) => {
      loading.value = false;
      if (res.statusCode === 200) {
        uni.showToast({ title: '注册成功' });
        setTimeout(() => {
          uni.navigateBack();
        }, 1500);
      } else {
        uni.showToast({ title: res.data || '注册失败', icon: 'none' });
      }
    },
    fail: () => {
      loading.value = false;
      uni.showToast({ title: '网络错误', icon: 'none' });
    }
  });
};
</script>

<style>
.container { padding: 40px 30px; display: flex; flex-direction: column; align-items: center; background: #fff; min-height: 100vh; }
.logo { width: 80px; height: 80px; margin-bottom: 20px; border-radius: 10px; }
.title { font-size: 24px; font-weight: bold; margin-bottom: 40px; color: #333; }
.form-area { width: 100%; }
.input { width: 100%; height: 50px; background: #f5f5f5; border-radius: 25px; padding: 0 20px; margin-bottom: 15px; font-size: 14px; box-sizing: border-box; }
.label { font-size: 14px; color: #666; margin: 10px 0 10px 5px; }
.radio-group { display: flex; flex-wrap: wrap; justify-content: space-between; margin-bottom: 20px; }
.radio-item { display: flex; align-items: center; width: 48%; margin-bottom: 10px; font-size: 14px; }

/* 上传样式 */
.upload-area { margin-bottom: 20px; }
.upload-box { width: 100px; height: 100px; background: #f0f0f0; border-radius: 8px; display: flex; align-items: center; justify-content: center; overflow: hidden; border: 1px dashed #ccc; }
.preview-img { width: 100%; height: 100%; }
.placeholder { display: flex; flex-direction: column; align-items: center; }
.plus { font-size: 30px; color: #999; line-height: 30px; }
.tip { font-size: 12px; color: #999; }
.sub-tip { font-size: 12px; color: #ff9800; margin-top: 5px; display: block; }

.btn { width: 100%; height: 50px; background: #007aff; color: #fff; border-radius: 25px; font-size: 16px; margin-top: 20px; display: flex; align-items: center; justify-content: center; }
.link { text-align: center; font-size: 14px; color: #007aff; margin-top: 20px; text-decoration: underline; }
</style>