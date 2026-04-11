<template>
  <div class="profile-container">
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <span>个人中心</span>
        </div>
      </template>

      <!-- 基础资料 -->
      <div class="info-section">
        <h3>基础资料</h3>
        <el-form label-width="100px" style="max-width: 460px">
          <el-form-item label="当前昵称">
            <el-input v-model="userInfo.username" placeholder="请输入新昵称">
              <template #append>
                <el-button @click="handleUpdateName">修改</el-button>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="绑定手机">
            <el-input v-model="userInfo.phone" placeholder="请输入新手机号">
              <template #append>
                <el-button @click="handleUpdatePhone">更换</el-button>
              </template>
            </el-input>
          </el-form-item>
        </el-form>
      </div>

      <el-divider />

      <!-- 修改密码 -->
      <div class="info-section">
        <h3>安全设置</h3>
        <el-form 
          :model="pwdForm" 
          label-width="100px" 
          style="max-width: 460px"
          ref="pwdFormRef"
        >
          <el-form-item label="原密码" prop="oldPass">
            <el-input v-model="pwdForm.oldPass" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码" prop="newPass">
            <el-input v-model="pwdForm.newPass" type="password" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="danger" @click="handleUpdatePassword">确认修改密码</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="logout-area">
        <el-button type="info" plain @click="doLogout">退出登录</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { userApi } from '@/api/user'
import { onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()

// 从 localStorage获取用户Id和用户名，初始化用户信息
const userId = localStorage.getItem('userId')
const userName = localStorage.getItem('userName')
const storedUser = JSON.parse(localStorage.getItem('userInfo')) || {}

const oldUserInfo = ref({ data: {} })

onMounted(async () => {
  const res = await userApi.getInfoById(userId)
  oldUserInfo.value = res

  userInfo.phone = res.data ? res.data.phone : ''
})

const userInfo = reactive({
  username: userName || '未知用户',
  phone: oldUserInfo.value.data ? oldUserInfo.value.data.phone : ''
})

const pwdForm = reactive({
  oldPass: '',
  newPass: ''
})

// 修改昵称
const handleUpdateName = async () => {
  if (!userInfo.username) return ElMessage.warning('昵称不能为空')
  await userApi.updateName(userId, userInfo.username)
  ElMessage.success('昵称修改成功')
  // 更新本地存储
  storedUser.username = userInfo.username
  localStorage.setItem('userInfo', JSON.stringify(storedUser))
}

// 修改手机号
const handleUpdatePhone = async () => {
  if (!/^1[3-9]\d{9}$/.test(userInfo.phone)) {
    return ElMessage.warning('请输入正确的手机号')
  }
  await userApi.updatePhone(userId, userInfo.phone)
  ElMessage.success('手机号已更新')
}

// 修改密码
const handleUpdatePassword = async () => {
  
  if (!pwdForm.oldPass || !pwdForm.newPass) {
    return ElMessage.warning('请填写完整密码信息')
  }

  if(pwdForm.oldPass === pwdForm.newPass) {
    return ElMessage.warning('新密码不能与原密码相同')
  }
  
  try {
    const res = await userApi.updatePassword({userId:userId, oldPass:pwdForm.oldPass, newPass:pwdForm.newPass})
    if (res.data) {
      ElMessage.success('密码修改成功，请重新登录')
      userApi.logout()
      router.push('/login')
    } else {
      ElMessage.error('原密码错误')
    }
  } catch (err) {
    // 自动被拦截器处理错误提示
  }
}

const doLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示')
    .then(() => {
      return userApi.logout()
    })
    .then(res => {
      ElMessage.success(res.data || '退出成功')
      localStorage.removeItem('token')
      router.push('/login')
    })
    .catch(() => {
      ;
    })
}
</script>

<style scoped>
.profile-container {
  padding: 40px;
  display: flex;
  justify-content: center;
}
.profile-card {
  width: 100%;
  max-width: 800px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}
.info-section {
  padding: 10px 0;
}
.info-section h3 {
  margin-bottom: 25px;
  color: #333;
  border-left: 4px solid #4a7c44;
  padding-left: 15px;
}
.logout-area {
  margin-top: 40px;
  text-align: center;
}
</style>