<template>
  <div class="common-layout">
    
<header class="header-bar">
      
<div class="header-left">
        <div class="logout-section" @click="handleLogout">
          <el-icon class="logout-icon"><SwitchButton /></el-icon>
          <span class="hint">点击此图标退出系统</span>
        </div>
      </div>

      
<div class="header-right">
        <div class="logo">Aventale</div>
      </div>
    </header>

    <div class="body-container">
      
<aside class="side-menu">
        <div 
          v-for="item in menuItems" 
          :key="item.path"
          :class="['menu-item', { active: activeMenu === item.path }]"
          @click="navigateTo(item.path)"
        >
          {{ item.name }}
        </div>
        
        <div class="side-footer">
          <span class="hint">Hotel Mgmt Sys v1.0</span>
        </div>
      </aside>
      
      
<main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { SwitchButton } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const activeMenu = ref(route.path)

const menuItems = [
  { name: '首页', path: '/home/dashboard' },
  { name: '订单管理', path: '/home/order-manage' },
  { name: '房间/房型管理', path: '/home/room-manage' },
  { name: '关于酒店', path: '/home/hotel-info' },
  { name: '知识库管理', path: '/home/knowledge-manage' },
  { name: '修改个人资料', path: '/home/user-info-manage' }
]

// 监听路由同步菜单高亮
watch(() => route.path, (newPath) => { activeMenu.value = newPath })

const navigateTo = (path) => { router.push(path) }

// 退出登录
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出 Aventale 管理系统吗？', '提示', {
    confirmButtonText: '确定退出',
    cancelButtonText: '取消',
    type: 'warning',
    // 适配你的紫色按钮风格
    confirmButtonClass: 'reg-btn-mini' 
  }).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    router.push('/login')
    ElMessage.success('已安全退出账号')
  })
}
</script>

<style scoped>
/* 容器布局 */
.common-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 顶部栏 */
.header-bar {
  display: flex;
  justify-content: space-between; /* 关键布局 */
  align-items: center;
  padding: 0 50px; /* 两侧内边距 */
  height: 90px;
  border-bottom: 2px solid #67c23a; /* 你的业务绿下划线 */
  background-color: #fff;
  z-index: 100;
}

/* 右上角 Logo：适配图片中的风格 */
.logo {
  color: #67c23a; /* 同样的绿色 */
  font-size: 36px; /* 稍大一点更显大气 */
  font-weight: bold;
  font-style: italic; /* 斜体 */
  /* Cursive / Playball 等草书/斜体字体，需自行确保引入 */
  font-family: 'Playball', 'Cursive', 'Signika', serif; 
  letter-spacing: 1px;
}

/* 左侧退出区域 */
.logout-section {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-icon {
  font-size: 22px;
  color: #a855f7; /* 你的操作紫 */
}

.logout-section:hover .logout-icon {
  color: #c084fc; /* 悬浮时淡紫 */
}

.hint {
  font-size: 12px;
  color: #999;
}

/* 主体容器 */
.body-container {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* 侧边栏 */
.side-menu {
  width: 260px;
  border-right: 2px solid #67c23a; /* 统一绿色边框 */
  display: flex;
  flex-direction: column;
  padding: 30px 0;
  background-color: #fcfdfc;
}

.menu-item {
  padding: 18px 40px;
  font-size: 15px;
  color: #555;
  cursor: pointer;
  transition: all 0.2s;
  border-bottom: 1px solid #f0f0f0;
  margin: 0 20px;
}

.menu-item:hover {
  color: #a855f7;
  padding-left: 45px; /* 悬浮时左侧微调，增加动感 */
}

/* 激活状态：底部紫色实线 */
.menu-item.active {
  color: #a855f7;
  font-weight: bold;
  border-bottom: 2px solid #a855f7; 
}

.side-footer {
  margin-top: auto;
  padding: 20px;
  text-align: center;
}

/* 内容区 */
.main-content {
  flex: 1;
  padding: 0;
  overflow-y: auto;
  background-color: #fdfdfd;
}

/* 适配你风格的 MessageBox 紫色按钮 */
:deep(.reg-btn-mini) {
  background: transparent !important;
  border: 1px solid #a855f7 !important;
  color: #a855f7 !important;
}
:deep(.reg-btn-mini:hover) {
  background: #a855f7 !important;
  color: white !important;
}
</style>
