import request from '@/utils/request'

export const checkInApi = {
  getByReservationId(reservationId) {
    return request.get('/checkIn/reservation', {
      params: { reservationId }
    })
  },

  create(data) {
    return request.post('/checkIn/create', data)
  }
}

export default checkInApi