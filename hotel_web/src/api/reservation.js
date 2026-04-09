import request from '@/utils/request'

export const reservationApi = {
  getByHotelId(hotelId) {
    return request.get('/reservation/getByHotelId', {
      params: { hotelId }
    })
  },

  getByPhone(phone) {
    return request.get(`/reservation/guest/phone`, {
      params: { phone }
    })
  },

  getByStatus(hotelId, status) {
    return request.get('/reservation/status', {
      params: { hotelId, status }
    })
  },

  getRoomType(id) {
    return request.get('/reservation/type', {
      params: { id }
    })
  },

  create(data) {
    return request.post('/reservation/newReservation', data)
  },

  updateStatus(id, status) {
    return request.put('/reservation/upstateStatus', null, {
      params: { id, status }
    })
  },

  delete(id) {
    return request.delete(`/reservation/${id}`)
  }
}

export default reservationApi