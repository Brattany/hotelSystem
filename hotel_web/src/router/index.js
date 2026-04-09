import { pa } from 'element-plus/es/locale/index.mjs'
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Welcome',
    component: () => import('../views/Welcome.vue') // P1 欢迎页
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/HotelRegister.vue') // P2 注册页
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue') // P3 登录页
  },
  {
    path:'/login-code',
    name:'LoginCode',
    component: () => import('../views/LoginCode.vue') // P4 验证码登录页
  },
  {
    path:'/user-register',
    name:'UserRegister',
    component: () => import('../views/UserRegister.vue') // P5 注册页
  },
  
  // --- 管理系统核心部分（使用 MainLayout 布局） ---
  {
    path: '/home',
    component: () => import('../views/MainLayout.vue'), // P6 作为外壳
    redirect: '/home/dashboard', // 访问 /home 时默认显示欢迎首页
    children: [
      {
        path: 'dashboard', // 实际地址：/home/dashboard
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue') // 原 P6 的右侧内容部分
      },
      {
        path: 'order-manage', // 实际地址：/home/order-manage
        name: 'OrderManage',
        component: () => import('../views/OrderManage.vue') // P7 订单页
      },
      {
        path: 'room-manage', // 实际地址：/home/room-manage
        name: 'RoomManage',
        component: () => import('../views/RoomManage.vue') // P8 房间页
      },
      {
        path: 'hotel-info', // 实际地址：/home/hotel-info
        name: 'HotelInfo',
        component: () => import('../views/HotelInfo.vue') // P9 酒店页
      },
      {
        path: 'user-info-manage', // 实际地址：/home/user-info-manage
        name: 'UserInfoManage',
        component: () => import('../views/UserInfoManage.vue') // P10 个人信息
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router