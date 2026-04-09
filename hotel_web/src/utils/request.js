// utils/request.js
import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 5000
})

request.interceptors.response.use(
  res => {
    const data = res.data
    if (data.code !== 200) {
      ElMessage.error(data.message || '系统异常')
      return Promise.reject(data)
    }
    return data
  },
  err => {
    ElMessage.error('网络异常')
    return Promise.reject(err)
  }
)

export default request