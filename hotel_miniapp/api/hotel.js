import request from '../utils/request.js';

export const hotelApi = {
  // 获取所有酒店列表
  getAllHotels: () => request.get('/hotel/all'),
  
  // 根据名称查询酒店
  searchHotelByName: (name) => request.get('/hotel/search', { name }),
  
  // 根据城市查询酒店
  getHotelByCity: (city) => request.get(`/hotel/city/${city}`),
  
  // 根据价格区间查询
  getHotelByPrice: (minPrice, maxPrice) => request.get('/hotel/price', { minPrice, maxPrice }),
  
  // 获取酒店所有标签
  getHotelTags: (hotelId) => request.get(`/hotel/${hotelId}/tags`)
};