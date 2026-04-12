import { hotelApi } from '../../api/hotel.js';

Page({
  data: {
    searchValue: '',
    activeTab: 0,
    tabPages: [
      '/pages/index/index',
      '/pages/hotel/hotel',
      '/pages/profile/profile'
    ]
  },

  onSearchChange(e) {
    this.setData({ searchValue: e.detail });
  },

  // 搜索
  onSearch() {
    const name = this.data.searchValue;
    if (!name) return;

    wx.reLaunch({
      url: `/pages/hotel/hotel?name=${encodeURIComponent(name)}`
    });
  },

  navigateTo(e) {
    const url = e.currentTarget.dataset.url;
    
    if (this.data.tabPages.includes(url)) {
      wx.switchTab({ url }); 
    } else {
      wx.navigateTo({ url }); 
    }
  },

  onTabChange(event) {
    const index = event.detail;
    this.setData({ activeTab: index });
    
    const urls = this.data.tabPages;
    wx.reLaunch({ url: urls[index] }); 
  }
});