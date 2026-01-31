<template>
  <div class="container">
    <div class="header">
      <h1 class="title">创建账号</h1>
      <p class="subtitle">加入中日信息消除平台</p>
    </div>

    <div class="form-box">
      <input class="input" v-model="form.username" type="text" placeholder="设置用户名" />
      <input class="input" v-model="form.password" type="password" placeholder="设置密码" />
      <input class="input" v-model="form.email" type="text" placeholder="电子邮箱" />
      <input class="input" v-model="form.nickname" type="text" placeholder="你的昵称" />

      <div class="label">选择身份:</div>
      <radio-group @change="onIdentityChange" class="radio-group">
        <label class="radio-item">
          <radio value="student" checked="true" />留学生
        </label>
        <label class="radio-item">
          <radio value="tourist" />游客
        </label>
        <label class="radio-item">
          <radio value="worker" />在日工作
        </label>
        <label class="radio-item">
          <radio value="agent" />中介/商户
        </label>
      </radio-group>

      <button class="btn-submit" @click="handleRegister">注 册</button>

      <div class="link-area">
        <text class="link" @click="goBack">返回登录</text>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue';

const form = reactive({
  username: '',
  password: '',
  email: '',
  nickname: '',
  identityType: 'student' // 默认值
});

const onIdentityChange = (e) => {
  form.identityType = e.detail.value;
};

const handleRegister = () => {
  if (!form.username || !form.password || !form.email) {
    uni.showToast({ title: '请填写必要信息', icon: 'none' });
    return;
  }

  uni.request({
    url: 'http://localhost:8080/auth/register',
    method: 'POST',
    data: form,
    success: (res) => {
      if (res.statusCode === 200) {
        uni.showToast({ title: '注册成功！', icon: 'success' });
        setTimeout(() => {
          uni.navigateBack(); // 返回登录页
        }, 1500);
      } else {
        uni.showToast({ title: '注册失败', icon: 'none' });
      }
    },
    fail: () => {
      uni.showToast({ title: '网络错误', icon: 'none' });
    }
  });
};

const goBack = () => uni.navigateBack();
</script>

<style>
/* 复用 Login 的样式，增加一点 Radio 样式 */
.container {
  padding: 40px 20px;
  background-color: #fff;
  min-height: 100vh;
}

.header {
  margin-bottom: 30px;
  margin-top: 40px;
}

.title {
  font-size: 30px;
  font-weight: bold;
  margin-bottom: 10px;
}

.subtitle {
  color: #999;
}

.input {
  width: 100%;
  height: 50px;
  background: #f5f5f5;
  border-radius: 8px;
  margin-bottom: 15px;
  padding: 0 15px;
  box-sizing: border-box;
}

.label {
  margin: 10px 0 5px;
  color: #666;
  font-size: 14px;
}

.radio-group {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}

.radio-item {
  font-size: 14px;
}

.btn-submit {
  width: 100%;
  height: 50px;
  background: #4cd964;
  color: #fff;
  /* 注册按钮绿色 */
  border-radius: 25px;
  line-height: 50px;
  font-size: 18px;
  margin-top: 10px;
}

.link-area {
  margin-top: 20px;
  text-align: center;
  color: #007aff;
}
</style>