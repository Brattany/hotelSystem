<template>
  <div class="welcome-container">
    <h1 class="system-title">Aventale 酒店信息化管理系统</h1>
    
    <div class="search-section">
      <el-select
        v-model="selectedHotelId"
        filterable
        remote
        :remote-method="handleSearch"
        placeholder="请选择您的酒店..."
        size="large"
        style="width: 400px"
        @change="handleHotelChange"
      >
        <el-option
          v-for="item in hotelList"
          :key="item.hotelId"
          :label="`${item.hotelName}  ${item.province}·${item.city}·${item.district}·${item.address}`"
          :value="item.hotelId"
        />
      </el-select>

      <div class="register-hint">
        未找到您的酒店？
        <el-link type="primary" @click="router.push('/register')">去注册</el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { useHotelStore } from '@/stores/hotel'

const hotelStore = useHotelStore()
const router = useRouter()
const selectedHotelId = ref('')
const hotelList = ref([])
let timer = null // 防抖定时器

// 获取全部酒店
const fetchHotels = async () => {
  try {
    const res = await axios.get('http://localhost:8080/hotel/all')
    if (res.data.code === 200) {
      hotelList.value = res.data.data || []
    }
  } catch (error) {
    console.error('获取酒店列表失败', error)
  }
}

// 核心：带防抖的远程搜索
const handleSearch = (query) => {
  if (timer) clearTimeout(timer)

  timer = setTimeout(async () => {
    try {
      if (!query) {
        await fetchHotels()
        return
      }

      const res = await axios.get(`http://localhost:8080/hotel/search?name=${query}`)
      console.log(res.data)
      if (res.data.code === 200) {
        hotelList.value = res.data.data || []
      } else {
        hotelList.value = []
      }
    } catch (error) {
      console.error('搜索失败', error)
      hotelList.value = []
    }
  }, 300) // 防抖300ms
}

// 选择酒店
const handleHotelChange = (id) => {
  console.log('准备存储的ID:', id)
  //从列表中找到当前选中的那个对象
  const selectedHotel = hotelList.value.find(hotel => hotel.hotelId === id)

  if(selectedHotel){
    console.log('选中的酒店:',selectedHotel.hotelName)
    hotelStore.setHotel(selectedHotel.hotelId, selectedHotel.hotelName) // 存入内存和 localStorage

    localStorage.setItem('hotelName', selectedHotel.hotelName)
    localStorage.setItem('hotelId', selectedHotel.hotelId)
  }
  
  console.log('存储成功，当前 localStorage:', localStorage.getItem('hotelId'), localStorage.getItem('hotelName'))
  router.push('/login')
}


onMounted(() => {
  fetchHotels()
})
</script>

<style scoped>
.welcome-container {
  height: 100vh;
  background-color: #e8f9f8; 
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.system-title {
  color: #67c23a; /* 绿色字体 */
  font-size: 3rem;
  margin-bottom: 2rem;
  font-family: "Zhi Mang Xing", cursive;
}
.register-hint {
  margin-top: 15px;
  font-size: 14px;
}
</style>