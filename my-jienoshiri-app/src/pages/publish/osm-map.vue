<template>
  <view class="page">
    <view id="map" class="map-box" :prop="trigger" :change:prop="osm.init"></view>

    <view class="footer">
      <view class="info">
        <text class="label">当前选择：</text>
        <text class="address">{{ address || '请点击地图选择位置' }}</text>
      </view>
      <button class="confirm-btn" @click="handleConfirm" :disabled="!lat">确认选择</button>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      trigger: 0, // 用于触发 renderjs 初始化
      address: '',
      lat: null,
      lng: null
    }
  },
  onLoad() {
    // 触发地图初始化
    setTimeout(() => { this.trigger++ }, 200);
  },
  methods: {
    // 接收 renderjs 传回的选点数据
    updateLocation(data) {
      this.address = data.address;
      this.lat = data.lat;
      this.lng = data.lng;
    },
    // 确认并返回上一页
    handleConfirm() {
      if (!this.lat) return;
      // 发送事件给上一页
      uni.$emit('location-selected', {
        name: this.address,
        latitude: this.lat,
        longitude: this.lng
      });
      uni.navigateBack();
    }
  }
}
</script>

<script module="osm" lang="renderjs">
export default {
  data() {
    return {
      map: null,
      marker: null,
      currentLat: 0,
      currentLng: 0
    }
  },
  methods: {
    init(newValue, oldValue, ownerInstance, instance) {
      if(this.map) return;
      
      // 1. 动态加载 Leaflet CSS
      const link = document.createElement('link');
      link.rel = 'stylesheet';
      link.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
      document.head.appendChild(link);

      // 2. 动态加载 Leaflet JS
      const script = document.createElement('script');
      script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
      script.onload = () => {
        this.createMap(ownerInstance);
      };
      document.head.appendChild(script);
    },
    
    createMap(ownerInstance) {
      // 初始化地图 (默认定位到日本，缩放级别 5)
      // 如果有定位权限，可以先 navigator.geolocation.getCurrentPosition 获取当前位置
      this.map = L.map('map').setView([35.6895, 139.6917], 13); // 东京

      // 加载 OSM 图层 (免费且无 Key)
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors'
      }).addTo(this.map);

      // 绑定点击事件
      this.map.on('click', (e) => {
        this.handleMapClick(e.latlng, ownerInstance);
      });
      
      // 尝试获取浏览器定位
      if (navigator.geolocation) {
          navigator.geolocation.getCurrentPosition((pos) => {
              const { latitude, longitude } = pos.coords;
              this.map.setView([latitude, longitude], 15);
          });
      }
    },

    handleMapClick(latlng, ownerInstance) {
      const { lat, lng } = latlng;
      this.currentLat = lat;
      this.currentLng = lng;

      // 1. 移动或创建标记
      if (this.marker) {
        this.marker.setLatLng(latlng);
      } else {
        this.marker = L.marker(latlng).addTo(this.map);
      }

      // 2. ⭐ 修改：调用自己的后端接口 (解决 CORS 和 403)
      // 注意：这里是在 renderjs 视图层运行，可以直接用 fetch 请求 localhost:8080
      const url = `http://localhost:8080/post/reverse-geo?lat=${lat}&lon=${lng}`;
      
      // 如果你的后端有鉴权 (需要登录)，你可能需要把 token 传进来或者在 SecurityConfig 放行这个接口
      // 这里假设你已经登录或接口已放行
      fetch(url, {
          method: 'GET',
           // 如果需要 Token，得从逻辑层传进来，这里先简单处理
      })
        .then(res => res.text()) // 后端直接返回的是 String (地址名)
        .then(addressName => {
          console.log('后端解析结果:', addressName);
          
          // 3. 传回逻辑层
          ownerInstance.callMethod('updateLocation', {
            address: addressName,
            lat: lat,
            lng: lng
          });
        })
        .catch(err => {
          console.error('解析失败', err);
          ownerInstance.callMethod('updateLocation', {
            address: '未知地点 (' + lat.toFixed(4) + ', ' + lng.toFixed(4) + ')',
            lat: lat,
            lng: lng
          });
        });
    },

    emitConfirm(event, ownerInstance) {
      ownerInstance.callMethod('handleConfirm');
    }
  }
}
</script>

<style>
.page { display: flex; flex-direction: column; height: 100vh; }
.map-box { flex: 1; width: 100%; background: #eee; }
.footer {
  background: #fff;
  padding: 15px;
  border-top: 1px solid #ddd;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.info { display: flex; align-items: center; }
.label { font-weight: bold; width: 80px; }
.address { flex: 1; font-size: 14px; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.confirm-btn { background: #007aff; color: #fff; font-size: 16px; width: 100%; border-radius: 25px; }
.confirm-btn[disabled] { background: #ccc; }
</style>