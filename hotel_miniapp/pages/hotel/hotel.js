import { hotelApi } from '../../api/hotel.js';

/*

Page({
  data: {
    hotelList: [],
    isLoading: true,
    searchQuery: '' 
  },

  onLoad(options) {
    if (options.name) {
      const decodedName = decodeURIComponent(options.name);
      this.setData({ searchQuery: decodedName });
      this.fetchHotelsByName(decodedName);
    } else {
      this.fetchAllHotels();
    }
  },

  // 获取所有酒店
  fetchAllHotels() {
    this.setData({ isLoading: true });
    hotelApi.getAllHotels()
      .then(res => {
        this.handleHotelData(res); 
      })
      .catch(() => this.setData({ isLoading: false }));
  },

  // 根据名称搜索酒店
  fetchHotelsByName(name) {
    this.setData({ isLoading: true });
    hotelApi.searchHotelByName(name)
      .then(res => {
        const list = Array.isArray(res) ? res : (res ? [res] : []);
        this.handleHotelData(list);
      })
      .catch(() => this.setData({ isLoading: false }));
  },

  handleHotelData(list) {
    if (!list || list.length === 0) {
      this.setData({ hotelList: [], isLoading: false });
      return;
    }

    const tagPromises = list.map(hotel => {
      return hotelApi.getHotelTags(hotel.id).then(tags => {
        hotel.tags = tags || [];
        return hotel;
      }).catch(() => {
        hotel.tags = []; 
        return hotel;
      });
    });

    Promise.all(tagPromises).then(completedList => {
      this.setData({
        hotelList: completedList,
        isLoading: false
      });
    });
  },

  // 跳转到具体房间选择页面
  goToRoom(e) {
    const hotelId = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/reservation/reservation?hotelId=${hotelId}`
    });
  }
});
*/

Page({
  data: {
    hotelList: [],
    isLoading: true,
    searchQuery: ''
  },

  onLoad(options) {
    if (options.name) {
      const decodedName = decodeURIComponent(options.name);
      this.setData({ searchQuery: decodedName });
      this.fetchHotelsByName(decodedName);
      return;
    }

    this.fetchAllHotels();
  },

  fetchAllHotels() {
    this.setData({ isLoading: true });
    hotelApi.getAllHotels()
      .then(res => {
        this.handleHotelData(Array.isArray(res) ? res : []);
      })
      .catch(() => {
        this.setData({ hotelList: [], isLoading: false });
      });
  },

  fetchHotelsByName(name) {
    this.setData({ isLoading: true });
    hotelApi.searchHotelByName(name)
      .then(res => {
        const list = Array.isArray(res) ? res : (res ? [res] : []);
        this.handleHotelData(list);
      })
      .catch(() => {
        this.setData({ hotelList: [], isLoading: false });
      });
  },

  handleHotelData(list) {
    if (!list.length) {
      this.setData({ hotelList: [], isLoading: false });
      return;
    }

    const tasks = list.map(hotel => {
      const hotelId = hotel.hotelId;
      return hotelApi.getHotelTags(hotelId)
        .then(tags => ({
          ...hotel,
          tags: Array.isArray(tags) ? tags : []
        }))
        .catch(() => ({
          ...hotel,
          tags: []
        }));
    });

    Promise.all(tasks).then(completedList => {
      this.setData({
        hotelList: completedList,
        isLoading: false
      });
    });
  },

  goToRoom(e) {
    const hotelId = e.currentTarget.dataset.id;
    if (!hotelId) {
      wx.showToast({ title: 'Missing hotel id', icon: 'none' });
      return;
    }

    wx.navigateTo({
      url: `/pages/reservation/reservation?hotelId=${hotelId}`
    });
  }
});
