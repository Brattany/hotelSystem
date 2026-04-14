Page({
  data: {
    searchValue: '',
    switchTabPages: [
      '/pages/index/index',
      '/pages/order/list',
      '/pages/profile/profile'
    ]
  },

  onSearchChange(event) {
    this.setData({
      searchValue: event.detail || ''
    });
  },

  onSearch() {
    const name = this.data.searchValue.trim();
    if (!name) {
      wx.showToast({
        title: '请输入酒店名称关键词',
        icon: 'none'
      });
      return;
    }

    wx.navigateTo({
      url: `/pages/hotel/hotel?name=${encodeURIComponent(name)}`
    });
  },

  navigateTo(event) {
    const { url } = event.currentTarget.dataset;
    if (!url) {
      return;
    }

    if (this.data.switchTabPages.includes(url)) {
      wx.switchTab({ url });
      return;
    }

    wx.navigateTo({ url });
  },

  goToCustomerService() {
    wx.navigateTo({
      url: '/pages/service/chat'
    });
  }
});