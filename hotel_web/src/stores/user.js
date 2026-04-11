import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore(
  'user',
  () => {
    const userId = ref(null)   
    const userName = ref('')
    const token = ref('')

    function setUser(id, name, userToken) {
      userId.value = id
      userName.value = name
      token.value = userToken
      localStorage.setItem('token', userToken)
    }

    // 退出登录时清理全部数据
    function logout() {
      userId.value = null
      userName.value = ''
      token.value = ''
      localStorage.clear()
    }

    return { userId, userName, token, setUser, logout }
  },
  {
    persist: {
      key: 'aventale-user-storage',
      storage: localStorage,
      // 关键：将 userId 加入持久化路径，确保刷新页面不丢失
      paths: ['userId', 'userName', 'token']
    }
  }
)