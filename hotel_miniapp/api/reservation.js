/*
/*
import request from '../utils/request.js';

export const reservationApi = {
  // 新建订单
  createReservation: (data) => request.post('/reservation/newReservation', data),
  
  // 根据手机号获取客户的所有订单
  getReservationsByPhone: (phone) => request.get('/reservation/guest/all/phone', { phone }),
  
  // 获取某一订单预订的房型
  getReservationType: (id) => request.get('/reservation/type', { id }),
  
  // 更新订单状态 (例如取消订单)
  updateStatus: (id, status) => request.put('/reservation/updateStatus', { id, status }),
  
  // 更新预订房型
  updateRoomType: (id, typeId) => request.put('/reservation/updateRoomType', { id, typeId }),
  
  // 更新预计入住/退房时间
  updateCheckInDate: (id, checkInDate) => request.put('/reservation/updateCheckInDate', { id, checkInDate }),
  updateCheckOutDate: (id, checkOutDate) => request.put('/reservation/updateCheckOutDate', { id, checkOutDate }),
  
  // 删除订单
  deleteReservation: (id) => request.delete(`/reservation/${id}`),

  // 通过订单ID获取入住记录
  getCheckInRecord: (reservationId) => request.get('/checkIn/reservation', { reservationId })
};
*/

import request from '../utils/request.js';

export const reservationApi = {
  createReservation: data => request.post('/reservation/newReservation', data),
  getReservationsByPhone: phone => request.get('/reservation/guest/all/phone', { phone }),
  getReservationType: id => request.get('/reservation/type', { id }),
  updateStatus: (id, status) => request.put(`/reservation/upstateStatus?id=${id}&status=${status}`),
  updateRoomType: (id, typeId) => request.put(`/reservation/upstateRoomType?id=${id}&typeId=${typeId}`),
  updateCheckInDate: (id, checkInDate) => request.put(`/reservation/upstateCheckInDate?id=${id}&checkInDate=${encodeURIComponent(checkInDate)}`),
  updateCheckOutDate: (id, checkOutDate) => request.put(`/reservation/upstateCheckOutDate?id=${id}&checkOutDate=${encodeURIComponent(checkOutDate)}`),
  deleteReservation: id => request.delete(`/reservation/${id}`),
  getCheckInRecord: reservationId => request.get('/checkIn/reservation', { reservationId })
};
*/

import request from '../utils/request.js';

export const reservationApi = {
  createReservation: data => request.post('/reservation/newReservation', data),
  getReservationsByPhone: phone => request.get('/reservation/guest/all/phone', { phone }),
  getReservationType: id => request.get('/reservation/type', { id }),
  updateStatus: (id, status) => request.put(`/reservation/upstateStatus?id=${id}&status=${status}`),
  updateRoomType: (id, typeId) => request.put(`/reservation/upstateRoomType?id=${id}&typeId=${typeId}`),
  updateCheckInDate: (id, checkInDate) => request.put(`/reservation/upstateCheckInDate?id=${id}&checkInDate=${encodeURIComponent(checkInDate)}`),
  updateCheckOutDate: (id, checkOutDate) => request.put(`/reservation/upstateCheckOutDate?id=${id}&checkOutDate=${encodeURIComponent(checkOutDate)}`),
  deleteReservation: id => request.delete(`/reservation/${id}`),
  getCheckInRecord: reservationId => request.get('/checkIn/reservation', { reservationId })
};
