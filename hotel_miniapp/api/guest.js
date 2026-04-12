import request from '../utils/request.js';

export const guestApi = {
  // 微信 code 静默登录 (用于换取 JWT Token)
  wxLogin: (code) => request.get('/guest/wxLogin', { code }),
  
  // 客户主动登录 (手机号/验证码等)
  login: (data) => request.post('/guest/login', data),
  
  // 发送验证码
  sendCode: (phone) => request.get('/guest/code', { phone }),
  
  // 客户注册
  register: (data) => request.post('/guest/register', data),
  
  // 获取客户信息 (根据ID / 手机号 / openId)
  getGuestById: (id) => request.get(`/guest/${id}`),
  getGuestByPhone: (phone) => request.get('/guest/phone', { phone }),
  getGuestByOpenId: (openId) => request.get(`/guest/openId/${openId}`),
  
  // 更新客户信息
  updateGuestInfo: (data) => request.put('/guest/update', data),
  
  // 注销/删除客户
  deleteGuest: (id) => request.delete(`/guest/${id}`)
};