import request from '@/utils/request'

export const checkOutApi = {
  getAll() {
    return request.get('/checkOut/all')
  },

  create(roomNumber) {
    return request.post('/checkOut/create', null, {
      params: { roomNumber }
    })
  }
}

export default checkOutApi