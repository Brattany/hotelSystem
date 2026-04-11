import request from '@/utils/request'

export const guestApi = {
  // 根据手机号获取顾客信息
  getByPhone(phone) {
    return request.get(`/guest/phone`, { params: { phone } })
  },
  // 创建新顾客
  create(data) {
    return request.post('/guest/create', data)
  }
}