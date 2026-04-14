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
    hotelList: [],
    isLoading: true,
    searchQuery: '',
    defaultCover: '../../assets/logo.jpg'
  },

  onLoad(options) {
    const keyword = options.name ? decodeURIComponent(options.name) : '';
    this.setData({ searchQuery: keyword });

    if (keyword) {
      this.fetchHotelsByName(keyword);
      return;
    }

    this.fetchAllHotels();
  },

  fetchAllHotels() {
    this.setData({ isLoading: true });
    hotelApi.getAllHotels()
      .then((res) => {
        this.handleHotelData(extractArray(res));
      })
      .catch(() => {
        this.setData({ hotelList: [], isLoading: false });
      });
  },

  fetchHotelsByName(name) {
    const keyword = (name || '').trim();
    if (!keyword) {
      this.fetchAllHotels();
      return;
    }

    this.setData({ isLoading: true, searchQuery: keyword });
    hotelApi.searchHotelByName(keyword)
      .then((res) => {
        const list = extractArray(res);
        if (list.length > 0) {
          this.handleHotelData(list);
          return;
        }

        this.fetchHotelsByNameFallback(keyword);
      })
      .catch(() => {
        this.fetchHotelsByNameFallback(keyword);
      });
  },

  fetchHotelsByNameFallback(keyword) {
    hotelApi.getAllHotels()
      .then((res) => {
        const list = extractArray(res);
        this.handleHotelData(this.filterHotelsByName(list, keyword));
      })
      .catch(() => {
        this.setData({ hotelList: [], isLoading: false });
      });
  },

  filterHotelsByName(list, keyword) {
    const normalizedKeyword = (keyword || '').trim().toLowerCase();
    if (!normalizedKeyword) {
      return Array.isArray(list) ? list : [];
    }

    return (Array.isArray(list) ? list : []).filter((hotel) => {
      const hotelName = this.getHotelName(hotel).toLowerCase();
      return hotelName.includes(normalizedKeyword);
    });
  },

  handleHotelData(list) {
    const sourceList = Array.isArray(list) ? list : [];
    if (!sourceList.length) {
      this.setData({ hotelList: [], isLoading: false });
      return;
    }

    const tasks = sourceList.map((hotel) => {
      const hotelId = this.getHotelId(hotel);
      if (!hotelId) {
        return Promise.resolve(this.normalizeHotel(hotel));
      }

      return hotelApi.getHotelTags(hotelId)
        .then((tags) => this.normalizeHotel({
          ...hotel,
          tags: Array.isArray(tags) ? tags : []
        }))
        .catch(() => this.normalizeHotel({
          ...hotel,
          tags: Array.isArray(hotel.tags) ? hotel.tags : []
        }));
    });

    Promise.all(tasks)
      .then((completedList) => {
        this.setData({
          hotelList: completedList,
          isLoading: false
        });
      })
      .catch(() => {
        this.setData({ hotelList: sourceList.map((item) => this.normalizeHotel(item)), isLoading: false });
      });
  },

  getHotelId(hotel) {
    return hotel.hotelId || hotel.id || hotel.hotelID || null;
  },

  getHotelName(hotel) {
    return hotel.hotelName || hotel.name || hotel.title || '未命名酒店';
  },

  getHotelCover(hotel) {
    return hotel.coverImage || hotel.cover || hotel.coverUrl || hotel.image || hotel.imageUrl || hotel.img || hotel.imgUrl || hotel.photo || hotel.picture || hotel.hotelImage || hotel.hotelImg || this.data.defaultCover;
  },

  getHotelSummary(hotel, tags) {
    const summary = hotel.introduction || hotel.intro || hotel.description || hotel.hotelDesc || hotel.remark || hotel.summary || '';
    if (summary) {
      return summary;
    }

    if (tags.length) {
      return tags.join(' / ');
    }

    return '舒适住宿，欢迎预订';
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

  goToDetail(e) {
    const hotelId = e.currentTarget.dataset.id;
    if (!hotelId) {
      wx.showToast({ title: '缺少酒店信息', icon: 'none' });
      return;
    }

    const selectedHotel = this.data.hotelList.find((item) => Number(item.hotelId) === Number(hotelId));
    if (selectedHotel) {
      wx.setStorageSync(HOTEL_CACHE_KEY, selectedHotel);
    }

    wx.navigateTo({
      url: `/pages/hotel/detail?hotelId=${hotelId}`
    });
  }
});