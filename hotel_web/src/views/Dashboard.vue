<template>
  <div class="dashboard-container">
    <div class="welcome-header">
      <h2>亲爱的 {{ userName }}</h2>
      <p class="time-greeting">{{ timeGreeting }}好！</p>
    </div>
    <div class="welcome-content">
      <p class="main-text">
        十分高兴您能成为 
        <span class="hotel-name">{{ hotelName }}</span> 
        的一员。
      </p>
      <p class="sub-text">
        您可以通过点击侧边栏进入相应界面进行相关事项的处理。祝您工作愉快！
      </p>
      <div class="feature-guide">
        <section class="guide-item">
          <p>
            在“<span class="page-link" @click="goTo('/home/order-manage')">订单管理</span>”页面中，
            您可以查询顾客订单信息，并进行入住/退房处理。
          </p>
        </section>
        <section class="guide-item">
          <p>
            在“<span class="page-link" @click="goTo('/home/room-manage')">房间/房型管理</span>”页面中，
            您可以对房间/房型相关信息进行操作，但请确保相关操作已然经过酒店管理层协商一致。
          </p>
        </section>
        <section class="guide-item">
          <p>
            在“<span class="page-link" @click="goTo('/home/hotel-info')">关于酒店</span>”页面中，
            您可以对酒店相关信息进行修改，但请确保相关操作已然经过酒店管理层商讨一致。
          </p>
        </section>
        <section class="guide-item">
          <p>
            在“<span class="page-link" @click="goTo('/home/user-info-manage')">修改个人信息</span>”页面中，
            您可以进行相关信息变更，如电话号码、密码以及职务（需得酒店负责人批准）。
          </p>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useHotelStore } from '@/stores/hotel'
import { storeToRefs } from 'pinia'

const router = useRouter()

//初始化Store
const userStore = useUserStore()
const hotelStore = useHotelStore()

// 使用 storeToRefs 提取变量，确保解构后的数据依然是响应式的
// 这样当我们在 P10 修改了 username 时，首页的欢迎语会自动变化
const { userName } = storeToRefs(userStore)
const { hotelName } = storeToRefs(hotelStore)

// 动态计算时间问候语
const timeGreeting = computed(() => {
  const hour = new Date().getHours()
  if (hour >= 5 && hour < 9) return '早上'
  if (hour >= 9 && hour < 12) return '中午'
  if (hour >= 12 && hour < 18) return '下午'
  return '晚上'
})

const goTo = (path) => {
  router.push(path)
}
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
  line-height: 1.8;
  color: #333;
  animation: fadeIn 0.5s ease-in-out;
}

.welcome-header h2 {
  font-size: 24px;
  color: #4a7c44; /* 延续你的主题绿 */
  margin-bottom: 5px;
}

.time-greeting {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 25px;
}

.welcome-content {
  font-size: 16px;
}

.hotel-name {
  font-weight: bold;
  text-decoration: underline;
  padding: 0 4px;
}

.main-text {
  margin-bottom: 10px;
}

.sub-text {
  margin-bottom: 30px;
  color: #666;
}

.feature-guide {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.guide-item {
  background: #fdfdfd;
  padding: 10px 0;
}

.page-link {
  color: #4a7c44;
  font-weight: bold;
  text-decoration: underline;
  cursor: pointer;
  transition: color 0.3s;
}

.page-link:hover {
  color: #9c27b0; /* 悬浮时变为紫色 */
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>