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