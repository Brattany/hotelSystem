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

  //发送验证码
  sendCode(phone) {
    return request.get('/user/code',{ params: { phone } })  
  },

  // 修改昵称
  updateName(newName) {
    return request.put('/user/name', null, { params: { newName } })
  },

  // 修改手机号
  updatePhone(newPhone) {
    return request.put('/user/phone', null, { params: { newPhone } })
  },

  // 修改密码 (对应后端 PasswordChangeRequest)
  updatePassword(oldPass, newPass) {
    return request.put('/user/password', { oldPass, newPass })
  },

  // 获取用户信息
  getUserInfo(phone) {
    return request.get('/user/info', { params: { phone } })
  }
}
  
  export default userApi