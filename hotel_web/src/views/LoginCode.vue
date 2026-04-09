<template>
  <div class="register-container">
    <div class="header">
      <span class="page-title">验证码登录</span>
      <h2 class="logo-text">Aventale</h2>
    </div>

    <el-form :model="form" label-position="top" class="register-form">
      <el-row :gutter="40" justify="center">
        <el-col :span="12">
          <h3>快捷登录：</h3>
          <el-form-item label="电话号码 (Phone)">
            <el-input v-model="form.phone" placeholder="请输入绑定的手机号" />
          </el-form-item>
          
          <el-form-item label="验证码 (Code)">
            <div style="display: flex; gap: 10px;">
              <el-input v-model="form.code" placeholder="6位验证码" />
              <el-button @click="sendCode" class="code-btn">获取验证码</el-button>
            </div>
          </el-form-item>

          <div class="link-group">
            <el-link type="primary" @click="$router.push('/login')">返回密码登录</el-link>
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
import { useUserStore } from '@/stores/user'
import userApi from '@/api/user'

const router = useRouter()
const userStore = useUserStore()

const form = reactive({ phone: '', code: '' })

const sendCode = async () => {
  if (!form.phone) return ElMessage.warning('请输入手机号')
  try {
    await userApi.sendCode(form.phone)
    ElMessage.success('验证码已发送')
  } catch (err) {}
}

//登录并存储用户信息
const handleLogin = async () => {
  if(!form.phone || !form.code) {
    ElMessage.warning('请填写完整登录信息')
    return
  }

  try {
    const loginRes = await userApi.loginWithCode(form)
    const token = loginRes.data

    localStorage.setItem('token', token) // 存储 token 以便后续请求使用

    const infoRes = await userApi.getUserInfo(form.phone)
    if(infoRes.data) {
      userStore.setUser(
        infoRes.data.userId,
        infoRes.data.username,
        token
      )
    localStorage.setItem('userId', infoRes.data.userId) // 存储用户ID
    localStorage.setItem('userName', infoRes.data.username) // 存储用户名
    
    ElMessage.success(`登录成功, ${userStore.userName}！`)
    router.push('/home')
  } else {
      ElMessage.error('用户信息同步失败，请联系系统管理员')
    }
  } catch (err) {
    console.error('登录失败', err)
  }
}
</script>

<style scoped>
@import "@/assets/user/style.css";
.code-btn {
  border: 1px solid #1a237e;
  color: #1a237e;
}
</style>