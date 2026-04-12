import request from '../utils/request.js';

export const roomTypeApi = {
  // 查询有空房间的房型
  getAvailableRoomTypes: (data) => request.post('/roomType/available', data),
  
  // 根据类型、状态以及时间范围查询具体房间
  getRoomsByTypeAndStatus: (data) => request.post('/room/typeAndStatus', data),
  
  // 根据状态获取房间
  filterRooms: (data) => request.post('/room/filter', data)
};