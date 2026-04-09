import request from '@/utils/request'

// 酒店相关接口封装
export const hotelApi = {
  // 获取所有酒店列表 (P1 欢迎页加载)
  getAll() {
    return request.get('/hotel/all')
  },

  // 根据名称模糊查询酒店 (P1 搜索)
  getByName(name) {
    return request.get(`/hotel/search`,name)
  },

  // 注册新酒店 (P2 提交表单)
  register(data) {
    return request.post('/hotel/register', data)
  },

  // 获取酒店详情 (P9)
  getById(id) {
    return request.get(`/hotel/${id}`)
  },

  // 更新酒店信息 (P9 编辑酒店信息)
  update(hotel) {
    return request.put('/hotel/update', hotel)
  },

  // 获取酒店标签列表 
  getTags(hotelId) {
    return request.get(`/hotel/${hotelId}/tags`)
  },

  // 添加新标签
  addTag(tag) {
    return request.post('/hotel/tag/add', tag)
  },

  // 更新标签
  updateTag(tag) {
    return request.put('/hotel/tag/update', tag)
  }
}

export default hotelApi