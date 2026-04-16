import request from '@/utils/request'

// 用户相关接口封装
export const userApi = {
  // 用户登录 (P3 电话号码+密码登录)
  loginWithPassword(data) {
    return request.post('/user/login/password', data)
  },
  
  //用户登录（P4 电话+验证码登录）
  loginWithCode(data) {
    return request.post('/user/login/code', data)
  },
  
  //用户注册（P5 注册页提交表单）
  register(data) {
    return request.post('/user/register', data)
  },

  //用户登出
  logout() {
    return request.post('/user/logout')
  },

  //发送验证码
  sendCode(phone) {
    return request.get('/user/code',{ params: { phone } })  
  },

  // 修改昵称
  updateName(userId, newName) {
    return request.put('/user/name', null, { params: { userId, newName } })
  },

  // 修改手机号
  updatePhone(userId,newPhone) {
    return request.put('/user/phone', null, { params: { userId, newPhone } })
  },

  // 修改密码
  updatePassword(data) {
    return request.put('/user/password', data)
  },

  // 获取用户信息
  getUserInfo(phone,hotelId) {
    return request.get('/user/info', { params: { phone , hotelId } })
  },

  getInfoById(userId) {
    return request.get('/user/infoById', { params: { userId } })  
  }
}
  
  export default userApi