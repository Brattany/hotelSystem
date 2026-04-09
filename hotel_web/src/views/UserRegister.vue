<template>
  <div class="register-container">
    <div class="header">
      <span class="page-title">用户注册</span>
      <h2 class="logo-text">Aventale</h2>
    </div>

    <el-form :model="form" label-position="top" class="register-form">
      <el-row :gutter="40">
        <!-- 左半部分 -->
        <el-col :span="12">
          <h3>基础信息：</h3>
          <el-form-item label="用户名 (Username)">
            <el-input v-model="form.userName" placeholder="长度至少为2个字符" />
          </el-form-item>
          
          <el-form-item label="设置密码 (Password)">
            <el-input v-model="form.password" type="password" placeholder="请输入密码(6-20个字符,同时包含数字和字母)" show-password />
          </el-form-item>

          <el-form-item label="担任职务 (Role)">
            <el-select v-model="form.role" placeholder="请选择您的职务" style="width: 100%">
              <el-option label="酒店管理员" value="ADMIN" />
              <el-option label="前台职员" value="STAFF" />
            </el-select>
          </el-form-item>
          
          <div class="footer-actions" style="margin-top: 60px;">
            <el-button type="primary" size="large" @click="submitRegister" class="reg-btn">立即注册</el-button>
          </div>
        </el-col>

        <!-- 右半部分 -->
        <el-col :span="12">
          <h3>安全验证：</h3>
          <el-form-item label="联系电话 (Phone)">
            <el-input v-model="form.phone" placeholder="请输入手机号" />
          </el-form-item>

          <el-form-item label="短信验证码 (Verify Code)">
            <div style="display: flex; gap: 10px;">
              <el-input v-model="form.code" placeholder="输入验证码" />
              <el-button @click="sendCode" plain>发送验证码</el-button>
            </div>
          </el-form-item>

          <el-alert
            title="注意事项"
            type="info"
            description="注册成功后，请使用设置的用户名或绑定的手机号进行登录。"
            show-icon
            :closable="false"
            style="margin-top: 20px"
          />
        </el-col>
      </el-row>
    </el-form>
  </div>
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import userApi from '@/api/user'
import { useHotelStore } from '@/stores/hotel'

const hotelStore = useHotelStore()
const router = useRouter()
const form = reactive({
  hotelId: '',
  userName: '',
  password: '',
  role: '',
  phone: '',
  code: ''
})

onMounted(() => {
  const id = hotelStore.hotelId || localStorage.getItem('hotelId')
  
  if (id) {
    form.hotelId = id
    console.log('注册页获取到酒店ID:', id)
  } else {
      ElMessage.error('未识别到酒店，请重新选择')
      router.push('/')
    }
})

const sendCode = async () => {
  if (!form.phone) return ElMessage.warning('请先填写手机号')
  await userApi.sendCode(form.phone)
  ElMessage.success('发送成功')
}

const submitRegister = async () => {
  console.log("最终提交的参数:", JSON.stringify(form))

  if (!form.userName || !form.password || !form.role) {
    ElMessage.error('请填写完整的注册信息')
    return
  }
  try {
    await userApi.register(form)
    ElMessage.success('注册成功！')
    router.push('/login')
  } catch (error) {}
}
</script>

<style scoped>
@import "@/assets/user/style.css";
</style>