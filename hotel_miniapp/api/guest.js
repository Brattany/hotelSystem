import request from '../utils/request.js';

export const guestApi = {
  wxLogin: (code) => request.get('/guest/wxLogin', { code }),
  
  login: (data) => request.post('/guest/login', data),
  
  sendCode: (phone) => request.get('/guest/code', { phone }),
  
  register: (data) => request.post('/guest/register', data),
  
  getGuestById: (id) => request.get(`/guest/${id}`),
  getGuestByPhone: (phone) => request.get('/guest/phone', { phone }),
  getGuestByOpenId: (openId) => request.get(`/guest/openId/${openId}`),
  
  updateGuestInfo: (data) => request.put('/guest/update', data),
  
  deleteGuest: (id) => request.delete(`/guest/${id}`)
};