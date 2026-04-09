import request from '@/utils/request'

export const roomApi = {
  // 获取当前酒店的所有房间
  getByHotel(hotelId) {
    return request.get('/room/hotel', { params: { hotelId } })
  },

  // 根据状态筛选指定类型的房间（0:空闲, 1:入住, 2:维修 等）
  getByTS(data) {
    return request.post('/room/typeAndStatus', data)
  },

  // 根据房型获取房间
  getByType(typeId) {
    return request.get(`/room/type/${typeId}`)
  },

  // 创建房间
  create(room) {
    return request.post('/room/create', room)
  },

  // 修改状态
  updateStatus(roomId, status) {
    return request.put(`/room/${roomId}/status`, null, { params: { status } })
  },

  //修改房间信息
  update(data){
    return request.put('/room/updateInfo', data)
  },

  // 删除房间
  deleteRoom(roomId) {
    return request.delete(`/room/delete`, { params: { roomId } })
  }
}

export default roomApi