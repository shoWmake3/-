<template>
  <div class="container">
    <div class="header">
      <h1 class="title">欢迎回来</h1>
      <p class="subtitle">登录你的皆の知り账号</p>
    </div>

    <div class="form-box">
      <input class="input" v-model="form.username" type="text" placeholder="请输入用户名" />
      <input class="input" v-model="form.password" type="password" placeholder="请输入密码" />
      
      <button class="btn-submit" @click="handleLogin">登 录</button>
      
      <div class="link-area">
        <text>还没有账号？</text>
        <text class="link" @click="goToRegister">去注册</text>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue';

const form = reactive({
  username: '',
  password: ''
});

const handleLogin = () => {
  if (!form.username || !form.password) {
    uni.showToast({ title: '请填写完整', icon: 'none' });
    return;
  }

  uni.showLoading({ title: '登录中...' }); // 加个加载动画体验更好

  // 1. 发起登录请求
  uni.request({
    url: 'http://localhost:8080/auth/login',
    method: 'POST',
    data: form,
    success: (res) => {
      // 检查状态码 (后端可能返回字符串token，也可能返回JSON，取决于您的实现)
      // 假设 res.data 直接就是字符串 token
      if (res.statusCode === 200 && res.data) {
        
        const token = res.data;
        
        // ⭐ 第一步：存 Token
        uni.setStorageSync('token', token);

        // ⭐ 第二步：紧接着获取用户信息 (这是关键修改！)
        uni.request({
          url: 'http://localhost:8080/auth/me', // 调用我们刚才加的接口
          method: 'GET',
          header: { 'Authorization': token },    // 带上刚拿到的 token
          success: (userRes) => {
            uni.hideLoading();
            
            if (userRes.statusCode === 200 && userRes.data) {
              // ⭐ 第三步：存用户信息 (mine.vue 就是靠这个显示的)
              uni.setStorageSync('user', userRes.data);

              uni.showToast({ title: '登录成功', icon: 'success' });

              // 4. 跳转首页
              setTimeout(() => {
                uni.reLaunch({ url: '/pages/index/index' });
              }, 1500);
            } else {
              uni.showToast({ title: '获取用户信息失败', icon: 'none' });
            }
          },
          fail: () => {
            uni.hideLoading();
            uni.showToast({ title: '网络异常', icon: 'none' });
          }
        });

      } else {
        uni.hideLoading();
        // 登录失败处理
        // 如果后端返回的是对象 {code: 500, message: '...'} 这里的判断要灵活调整
        const msg = (typeof res.data === 'object' && res.data.message) ? res.data.message : '账号或密码错误';
        uni.showToast({ title: '登录失败: ' + msg, icon: 'none' });
      }
    },
    fail: (err) => {
      uni.hideLoading();
      console.error(err);
      uni.showToast({ title: '连接服务器失败', icon: 'none' });
    }
  });
};

const goToRegister = () => {
  uni.navigateTo({ url: '/pages/register/register' });
};
</script>

<style>
.container { padding: 40px 20px; background-color: #fff; height: 100vh; }
.header { margin-bottom: 40px; margin-top: 60px; }
.title { font-size: 32px; font-weight: bold; color: #333; margin-bottom: 10px; }
.subtitle { font-size: 16px; color: #999; }
.input {
  width: 100%; height: 50px; background: #f5f5f5; border-radius: 8px;
  margin-bottom: 20px; padding: 0 15px; box-sizing: border-box; font-size: 16px;
}
.btn-submit {
  width: 100%; height: 50px; background: #007aff; color: #fff;
  border-radius: 25px; font-size: 18px; line-height: 50px; margin-top: 20px;
}
.link-area { margin-top: 20px; text-align: center; font-size: 14px; color: #666; }
.link { color: #007aff; margin-left: 5px; }
</style>