import request from '@/utils/request'

// 酒店相关接口封装
export const hotelApi = {
  // 获取所有酒店列表
  getAll() {
    return request.get('/hotel/all')
  },

  // 根据名称模糊查询酒店
  getByName(name) {
    return request.get('/hotel/search', { params: { name } })
  },

  // 注册新酒店
  register(data) {
    return request.post('/hotel/register', data)
  },

  // 获取酒店详情
  getById(id) {
    return request.get(`/hotel/${id}`)
  },

  // 更新酒店信息
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
  },

  //删除标签
  deleteTag(tagId) {
    return request.delete(`/hotel/tagDel`,{params:{tagId}})
  }
}

export default hotelApi
