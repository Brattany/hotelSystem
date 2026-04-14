import { hotelApi } from '../../api/hotel.js';

const HOTEL_CACHE_KEY = 'selectedHotel';

const extractArray = (res) => {
  if (Array.isArray(res)) return res;
  if (res && Array.isArray(res.data)) return res.data;
  if (res && res.data && Array.isArray(res.data.records)) return res.data.records;
  if (res && Array.isArray(res.records)) return res.records;
  return [];
};

Page({
  data: {
    hotelId: null,
    hotel: null,
    isLoading: true
  },

  onLoad(options) {
    const hotelId = Number(options.hotelId || 0);

    if (!hotelId) {
      wx.showToast({ title: '缺少酒店信息', icon: 'none' });
      setTimeout(() => {
        wx.navigateBack();
      }, 500);
      return;
    }

    this.setData({ hotelId });
    this.loadCachedHotel(hotelId);
    this.loadHotelDetail();
  },

  loadCachedHotel(hotelId) {
    const cachedHotel = wx.getStorageSync(HOTEL_CACHE_KEY);
    if (!cachedHotel) {
      return;
    }

    if (Number(this.getHotelId(cachedHotel)) !== Number(hotelId)) {
      return;
    }

    this.setData({
      hotel: this.normalizeHotel(cachedHotel),
      isLoading: false
    });
  },

  loadHotelDetail() {
    hotelApi.getAllHotels()
      .then((res) => {
        const hotels = extractArray(res);
        const targetHotel = hotels.find((hotel) => Number(this.getHotelId(hotel)) === Number(this.data.hotelId));

        if (!targetHotel) {
          this.setData({ isLoading: false, hotel: null });
          return;
        }

        const hotelId = this.getHotelId(targetHotel);
        return hotelApi.getHotelTags(hotelId)
          .then((tags) => {
            const hotel = this.normalizeHotel({
              ...targetHotel,
              tags: Array.isArray(tags) ? tags : []
            });

            this.setData({ hotel, isLoading: false });
            wx.setStorageSync(HOTEL_CACHE_KEY, hotel);
          })
          .catch(() => {
            const hotel = this.normalizeHotel(targetHotel);
            this.setData({ hotel, isLoading: false });
            wx.setStorageSync(HOTEL_CACHE_KEY, hotel);
          });
      })
      .catch(() => {
        this.setData({ isLoading: false });
      });
  },

  getHotelId(hotel) {
    return hotel.hotelId || hotel.id || hotel.hotelID || null;
  },

  getHotelName(hotel) {
    return hotel.hotelName || hotel.name || hotel.title || '未命名酒店';
  },

  getHotelCover(hotel) {
    return hotel.coverImage || hotel.cover || hotel.coverUrl || hotel.image || hotel.imageUrl || hotel.img || hotel.imgUrl || hotel.photo || hotel.picture || hotel.hotelImage || hotel.hotelImg || '../../assets/logo.jpg';
  },

  getHotelSummary(hotel, tags) {
    const summary = hotel.introduction || hotel.intro || hotel.description || hotel.hotelDesc || hotel.remark || hotel.summary || '';
    if (summary) {
      return summary;
    }

    if (tags.length) {
      return tags.join(' / ');
    }

    return '舒适住客体验，欢迎预订';
  },

  normalizeHotel(hotel) {
    const tags = (Array.isArray(hotel.tags) ? hotel.tags : [])
      .map((tag) => {
        if (typeof tag === 'string') {
          return tag;
        }

        return tag.tag || tag.tagName || tag.name || '';
      })
      .filter(Boolean);

    return {
      ...hotel,
      hotelId: this.getHotelId(hotel),
      hotelName: this.getHotelName(hotel),
      displayAddress: [hotel.city, hotel.address].filter(Boolean).join(' ') || '暂无地址信息',
      coverImage: this.getHotelCover(hotel),
      tags,
      summary: this.getHotelSummary(hotel, tags)
    };
  },

  goToReservation() {
    const hotel = this.data.hotel;
    if (!hotel || !hotel.hotelId) {
      wx.showToast({ title: '缺少酒店信息', icon: 'none' });
      return;
    }

    wx.setStorageSync(HOTEL_CACHE_KEY, hotel);
    wx.navigateTo({
      url: `/pages/reservation/reservation?hotelId=${hotel.hotelId}&hotelName=${encodeURIComponent(hotel.hotelName || '')}`
    });
  }
});