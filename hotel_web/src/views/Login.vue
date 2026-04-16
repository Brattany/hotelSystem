<template>
  <div class="register-container">
    <div class="header">
      <span class="page-title">用户登录</span>
      <h2 class="logo-text">Aventale</h2>
    </div>

    <el-form :model="form" label-position="top" class="register-form">
      <el-row :gutter="40" justify="center">
        <el-col :span="12">
          <el-form-item label="电话号码 (phone)">
            <el-input v-model="form.phone" placeholder="请输入电话号码" />
          </el-form-item>
          
          <el-form-item label="密码 (Password)">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
          </el-form-item>

          <div class="link-group">
            <el-link type="primary" @click="$router.push('/login-code')">短信验证码登录</el-link>
            <el-link type="info" @click="$router.push('/user-register')">没有账号？点击注册</el-link>
          </div>

          <div class="footer-actions">
            <el-button type="primary" size="large" @click="handleLogin" class="reg-btn">登录</el-button>
          </div>
        </el-col>
      </el-row>
    </el-form>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import userApi from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const savedHotelId = localStorage.getItem('hotelId')

const form = reactive({
  phone: '',
  password: '',
  hotelId: savedHotelId ? Number(savedHotelId) : null
})

const handleLogin = async () => {
  if (!form.phone || !form.password) {
    ElMessage.warning('请填写完整登录信息')
    return
  }
  try {
    //获取token
    const loginRes = await userApi.loginWithPassword(form)
    const token = loginRes.data

    localStorage.setItem('token', token) // 存储 token 以便后续请求使用

    //获取用户信息
    const infoRes = await userApi.getUserInfo(form.phone, form.hotelId)
    if(infoRes.data) {
      userStore.setUser(
        infoRes.data.userId,
        infoRes.data.username,
        token
      )
    localStorage.setItem('userId', infoRes.data.userId)
    localStorage.setItem('userName', infoRes.data.username)

    ElMessage.success(`登录成功, ${userStore.userName}！`)
    router.push('/home')
  } else {
      ElMessage.error('用户信息同步失败，请联系系统管理员')
    }
}
  catch (error) {
    console.error('登录失败', error)
  }
}
</script>

<style scoped>
@import "@/assets/user/style.css"; 
.link-group {
  margin-top: -10px;
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
}
</style>