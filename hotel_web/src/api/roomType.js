import request from '@/utils/request'

export const roomTypeApi = {
  // 获取当前酒店的所有房型
  getAll(hotelId) {
    return request.get(`/roomType/hotel`, { params: { hotelId } })
  },

  //获取有余量的房型
  getAvailable(data) {
    return request.post(`/roomType/available`, data )
  },

  // 创建新房型
  create(roomType) {
    return request.post('/roomType/create', roomType)
  },

  // 修改房型信息
  update(data) {
    return request.put('/roomType/update', data)
  },

  // 删除房型
  deleteType(id) {
    return request.delete(`/roomType/${id}`)
  }
}

export default roomTypeApi