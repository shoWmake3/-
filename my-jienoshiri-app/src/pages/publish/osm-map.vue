<template>
  <view class="page">
    
    <view id="map" class="map-box" :prop="trigger" :change:prop="osm.init"></view>

    <view class="top-toast">
      <text class="toast-icon">📍</text>
      <text class="toast-text">拖动或点击地图选择位置</text>
    </view>

    <view class="glass-panel">
      <view class="panel-header">
        <view class="drag-bar"></view>
      </view>
      
      <view class="info-row">
        <view class="icon-box">
          <text class="loc-icon">🗺️</text>
        </view>
        <view class="text-col">
          <text class="label">当前选中位置</text>
          <text class="address" :class="{ 'placeholder': !address }">
            {{ address || '暂未选择，请点击地图' }}
          </text>
          <text class="coords" v-if="lat">
            {{ lat.toFixed(4) }}, {{ lng.toFixed(4) }}
          </text>
        </view>
      </view>

      <button class="confirm-btn" @click="handleConfirm" :disabled="!lat" 
        hover-class="btn-hover">
        确认选择
      </button>
    </view>

  </view>
</template>

<script>
export default {
  data() {
    return {
      trigger: 0, 
      address: '',
      lat: null,
      lng: null
    }
  },
  onLoad() {
    setTimeout(() => { this.trigger++ }, 200);
  },
  methods: {
    updateLocation(data) {
      this.address = data.address;
      this.lat = data.lat;
      this.lng = data.lng;
    },
    handleConfirm() {
      if (!this.lat) return;
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
      
      const link = document.createElement('link');
      link.rel = 'stylesheet';
      link.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
      document.head.appendChild(link);

      const script = document.createElement('script');
      script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
      script.onload = () => {
        this.createMap(ownerInstance);
      };
      document.head.appendChild(script);
    },
    
    createMap(ownerInstance) {
      this.map = L.map('map', {
          zoomControl: false, // 隐藏默认缩放控件，为了美观
          attributionControl: false // 隐藏版权信息 (可选，注意合规)
      }).setView([35.6895, 139.6917], 13);

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap'
      }).addTo(this.map);

      // 自定义图标 (可选，这里用默认的)
      
      this.map.on('click', (e) => {
        this.handleMapClick(e.latlng, ownerInstance);
      });
      
      // 获取并显示用户当前位置
      if (navigator.geolocation) {
          navigator.geolocation.getCurrentPosition((pos) => {
              const { latitude, longitude } = pos.coords;
              this.map.setView([latitude, longitude], 15);
              
              // 添加当前位置标记（蓝色圆点样式）
              const currentLocationIcon = L.divIcon({
                  className: 'current-location-marker',
                  html: '<div style="background-color: #4285f4; width: 16px; height: 16px; border-radius: 50%; border: 3px solid white; box-shadow: 0 2px 6px rgba(0,0,0,0.3);"></div>',
                  iconSize: [20, 20],
                  iconAnchor: [10, 10]
              });
              
              L.marker([latitude, longitude], { icon: currentLocationIcon })
                .addTo(this.map)
                .bindPopup('📍 我的当前位置')
                .openPopup();
          }, (err) => {
              console.log('无法获取当前位置:', err);
          });
      }
    },

    handleMapClick(latlng, ownerInstance) {
      const { lat, lng } = latlng;
      this.currentLat = lat;
      this.currentLng = lng;

      if (this.marker) {
        this.marker.setLatLng(latlng);
      } else {
        this.marker = L.marker(latlng).addTo(this.map);
      }
      
      // 动画平移到点击位置
      this.map.panTo(latlng);

      // 先显示坐标作为临时地址
      ownerInstance.callMethod('updateLocation', {
        address: '正在获取地址...',
        lat: lat,
        lng: lng
      });

      // 使用多个逆解析服务（带降级处理）
      this.reverseGeocode(lat, lng, ownerInstance);
    },
    
    async reverseGeocode(lat, lng, ownerInstance) {
      // 备用服务列表
      const services = [
        // 主服务：Nominatim (OpenStreetMap 官方)
        () => this.fetchWithTimeout(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&accept-language=zh-CN,zh;q=0.9,en;q=0.8`, {
          headers: { 'User-Agent': 'MyJienoshiriApp/1.0' }
        }, 5000),
        
        // 备用服务 1：使用更简单的格式
        () => this.fetchWithTimeout(`https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lng}`, {
          headers: { 'User-Agent': 'MyJienoshiriApp/1.0' }
        }, 5000),
        
        // 备用服务 2：使用 OpenCage (需要 API key，这里仅作示例，实际使用时可替换)
        // 如果没有 API key，可以注释掉或使用其他免费服务
      ];

      for (let i = 0; i < services.length; i++) {
        try {
          const data = await services[i]();
          if (data && data.display_name) {
            ownerInstance.callMethod('updateLocation', {
              address: data.display_name,
              lat: lat,
              lng: lng
            });
            return; // 成功则退出
          }
        } catch (err) {
          console.log(`[OSM] 逆解析服务 ${i+1} 失败:`, err.message);
          // 继续尝试下一个服务
        }
      }

      // 所有服务都失败，降级显示坐标
      ownerInstance.callMethod('updateLocation', {
        address: '📍 ' + lat.toFixed(4) + ', ' + lng.toFixed(4),
        lat: lat,
        lng: lng
      });
    },
    
    fetchWithTimeout(url, options, timeoutMs) {
      return Promise.race([
        fetch(url, options).then(res => {
          if (!res.ok) throw new Error(`HTTP ${res.status}`);
          return res.json();
        }),
        new Promise((_, reject) => 
          setTimeout(() => reject(new Error('Timeout')), timeoutMs)
        )
      ]);
    }
  }
}
</script>

<style>
:root {
  --primary: #6366f1;
}

.page { 
  display: flex; flex-direction: column; height: 100vh; width: 100vw; 
  position: relative; overflow: hidden;
}

/* 1. 地图全屏 */
.map-box { 
  width: 100%; height: 100%; 
  background: #e2e8f0; /* 加载前的底色 */
}

/* 2. 顶部提示条 */
.top-toast {
  position: absolute; top: 50px; left: 50%; transform: translateX(-50%);
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  padding: 8px 16px; border-radius: 20px;
  display: flex; align-items: center; gap: 6px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.1);
  z-index: 999;
  animation: slideDown 0.5s ease-out;
}
@keyframes slideDown { from { transform: translate(-50%, -20px); opacity: 0; } to { transform: translate(-50%, 0); opacity: 1; } }

.toast-icon { font-size: 14px; }
.toast-text { font-size: 13px; color: #475569; font-weight: 500; }

/* 3. 底部悬浮面板 */
.glass-panel {
  position: absolute; bottom: 30px; left: 20px; right: 20px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px); -webkit-backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 20px;
  box-shadow: 0 10px 40px -10px rgba(0,0,0,0.15);
  border: 1px solid rgba(255, 255, 255, 0.8);
  z-index: 1000;
  animation: slideUp 0.5s ease-out;
}
@keyframes slideUp { from { transform: translateY(100px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }

.panel-header { display: flex; justify-content: center; margin-bottom: 15px; }
.drag-bar { width: 40px; height: 4px; background: #cbd5e1; border-radius: 2px; }

.info-row { display: flex; align-items: flex-start; gap: 12px; margin-bottom: 20px; }

.icon-box {
  width: 40px; height: 40px; background: rgba(99, 102, 241, 0.1);
  border-radius: 12px; display: flex; align-items: center; justify-content: center;
}
.loc-icon { font-size: 20px; }

.text-col { flex: 1; overflow: hidden; }
.label { font-size: 12px; color: #94a3b8; margin-bottom: 4px; display: block; }
.address { 
  font-size: 16px; font-weight: 700; color: #1e293b; 
  line-height: 1.4; display: block; 
  /* 多行截断 */
  display: -webkit-box; 
  -webkit-line-clamp: 2; 
  line-clamp: 2;
  -webkit-box-orient: vertical; 
  overflow: hidden;
}
.address.placeholder { color: #cbd5e1; font-weight: 400; }
.coords { font-size: 11px; color: #6366f1; margin-top: 4px; display: block; font-family: monospace; }

/* 按钮 */
.confirm-btn {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff; font-size: 15px; font-weight: 600;
  border-radius: 16px; height: 44px; line-height: 44px;
  box-shadow: 0 4px 15px rgba(99, 102, 241, 0.3);
  transition: all 0.2s;
}
.btn-hover { transform: scale(0.98); opacity: 0.9; }
.confirm-btn[disabled] { background: #cbd5e1; box-shadow: none; color: #64748b; opacity: 0.6; }

/* PC 适配 */
@media screen and (min-width: 800px) {
  .glass-panel { width: 400px; left: 50%; transform: translateX(-50%); bottom: 40px; }
  /* 复写动画，防止 transform 冲突 */
  @keyframes slideUpPC { from { transform: translate(-50%, 100px); opacity: 0; } to { transform: translate(-50%, 0); opacity: 1; } }
  .glass-panel { animation-name: slideUpPC; }
}

/* 当前位置标记样式 */
.current-location-marker {
  background: transparent;
}
.current-location-marker div {
  transition: all 0.3s ease;
}
.current-location-marker div:hover {
  transform: scale(1.2);
}
</style>