import { roomTypeApi } from '../../api/roomType.js';
import { reservationApi } from '../../api/reservation.js';
import { guestApi } from '../../api/guest.js';
import Toast from '@vant/weapp/toast/toast';

/*

// 日期格式化工具：转为 yyyy-MM-dd
const formatDate = (date) => {
  const d = new Date(date);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};

Page({
  data: {
    hotelId: null,
    
    // 日期相关
    showCalendar: false,
    checkInDate: '',
    checkOutDate: '',
    dateString: '请选择入住和退房日期',
    days: 0,
    
    // 房型相关
    availableRoomTypes: [],
    selectedRoomTypeId: null,
    selectedRoomPrice: 0,
    
    // 表单与价格计算
    guestName: '',
    guestPhone: '',
    totalPrice: 0
  },

  onLoad(options) {
    if (options.hotelId) {
      this.setData({ hotelId: Number(options.hotelId) });
    }
    
    this.fetchCurrentGuestInfo();
  },

  fetchCurrentGuestInfo() {
    const phone = wx.getStorageSync('userPhone');
    
    if (!phone) {
      console.log('本地无手机号缓存，跳过信息回填');
      return;
    }

    guestApi.getGuestByPhone(phone)
      .then(res => {
        if (res) {
          this.setData({
            guestName: res.name || '',
            guestPhone: res.phone || ''
          });
        }
      })
      .catch(err => {
        console.error('获取回填用户信息失败:', err);
      });
  },

  onDisplayCalendar() {
    this.setData({ showCalendar: true });
  },
  onCloseCalendar() {
    this.setData({ showCalendar: false });
  },
  onConfirmCalendar(event) {
    const [start, end] = event.detail;
    const checkInDate = formatDate(start);
    const checkOutDate = formatDate(end);
    
    const days = Math.round((end - start) / (1000 * 60 * 60 * 24));

    this.setData({
      showCalendar: false,
      checkInDate,
      checkOutDate,
      dateString: `${checkInDate} 至 ${checkOutDate}`,
      days
    });

    this.fetchAvailableRooms();
  },

  fetchAvailableRooms() {
    const { hotelId, checkInDate, checkOutDate } = this.data;
    if (!checkInDate || !checkOutDate) return;

    Toast.loading({ message: '查询房型中...', forbidClick: true });

    const requestData = {
      hotelId,
      checkInDate: `${checkInDate} 14:00:00`, 
      checkOutDate: `${checkOutDate} 12:00:00`
    };

    roomTypeApi.getAvailableRoomTypes(requestData)
      .then(res => {
        Toast.clear();
        this.setData({ 
          availableRoomTypes: res || [],
          selectedRoomTypeId: null,
          selectedRoomPrice: 0,
          totalPrice: 0
        });
      })
      .catch(() => Toast.clear());
  },

  // 选择房型卡片
  selectRoomType(e) {
    const roomType = e.currentTarget.dataset.item;
    const totalPrice = roomType.price * this.data.days;
    
    this.setData({
      selectedRoomTypeId: roomType.id,
      selectedRoomPrice: roomType.price,
      totalPrice: totalPrice
    });
  },

  onNameChange(e) { this.setData({ guestName: e.detail }); },
  onPhoneChange(e) { this.setData({ guestPhone: e.detail }); },

  onSubmitOrder() {
    const { hotelId, checkInDate, checkOutDate, selectedRoomTypeId, guestName, guestPhone, totalPrice } = this.data;

    if (!checkInDate) return Toast('请选择日期');
    if (!selectedRoomTypeId) return Toast('请选择房型');
    if (!guestName || !guestPhone) return Toast('请完善入住人信息');

    Toast.loading({ message: '提交中...', forbidClick: true });

    const reservationData = {
      hotelId,
      roomTypeId: selectedRoomTypeId,
      checkInDate: `${checkInDate} 14:00:00`,
      checkOutDate: `${checkOutDate} 12:00:00`,
      guestName, 
      guestPhone,
      totalPrice,
      status: 0 
    };

    reservationApi.createReservation(reservationData)
    .then(res => {
      const orderId = res.id || res; 

      if (res) {
        Toast.success('订单已提交');
        setTimeout(() => {
          wx.navigateTo({
            url: `/pages/payment/payment?orderId=${orderId}&amount=${totalPrice}`
          });
        }, 1000);
      }
    });
  }
});
*/

const formatDate = date => {
  const d = new Date(date);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};

Page({
  data: {
    hotelId: null,
    showCalendar: false,
    checkInDate: '',
    checkOutDate: '',
    dateString: 'Select check-in and check-out',
    days: 0,
    availableRoomTypes: [],
    selectedRoomTypeId: null,
    selectedRoomPrice: 0,
    guestName: '',
    guestPhone: '',
    totalPrice: 0
  },

  onLoad(options) {
    const hotelId = Number(options.hotelId);
    if (!hotelId) {
      wx.showToast({ title: 'Missing hotel id', icon: 'none' });
      setTimeout(() => wx.navigateBack(), 500);
      return;
    }

    this.setData({ hotelId });
    this.fetchCurrentGuestInfo();
  },

  fetchCurrentGuestInfo() {
    const phone = wx.getStorageSync('userPhone');
    if (!phone) return;

    guestApi.getGuestByPhone(phone)
      .then(res => {
        if (!res) return;

        this.setData({
          guestName: res.name || '',
          guestPhone: res.phone || phone
        });
      })
      .catch(() => {});
  },

  onDisplayCalendar() {
    this.setData({ showCalendar: true });
  },

  onCloseCalendar() {
    this.setData({ showCalendar: false });
  },

  onConfirmCalendar(event) {
    const [start, end] = event.detail;
    const checkInDate = formatDate(start);
    const checkOutDate = formatDate(end);
    const days = Math.max(1, Math.round((end - start) / (1000 * 60 * 60 * 24)));

    this.setData({
      showCalendar: false,
      checkInDate,
      checkOutDate,
      dateString: `${checkInDate} to ${checkOutDate}`,
      days
    });

    this.fetchAvailableRooms();
  },

  fetchAvailableRooms() {
    const { hotelId, checkInDate, checkOutDate } = this.data;
    if (!hotelId || !checkInDate || !checkOutDate) return;

    Toast.loading({ message: 'Loading rooms...', forbidClick: true });

    roomTypeApi.getAvailableRoomTypes({
      hotelId,
      checkInDate,
      checkOutDate
    })
      .then(res => {
        this.setData({
          availableRoomTypes: Array.isArray(res) ? res : [],
          selectedRoomTypeId: null,
          selectedRoomPrice: 0,
          totalPrice: 0
        });
      })
      .catch(() => {
        this.setData({
          availableRoomTypes: [],
          selectedRoomTypeId: null,
          selectedRoomPrice: 0,
          totalPrice: 0
        });
      })
      .finally(() => {
        Toast.clear();
      });
  },

  selectRoomType(e) {
    const roomType = e.currentTarget.dataset.item || {};
    const roomPrice = Number(roomType.price || 0);

    this.setData({
      selectedRoomTypeId: roomType.typeId || null,
      selectedRoomPrice: roomPrice,
      totalPrice: roomPrice * this.data.days
    });
  },

  onNameChange(e) {
    this.setData({ guestName: e.detail });
  },

  onPhoneChange(e) {
    this.setData({ guestPhone: e.detail });
  },

  onSubmitOrder() {
    const {
      hotelId,
      checkInDate,
      checkOutDate,
      selectedRoomTypeId,
      guestName,
      guestPhone,
      totalPrice
    } = this.data;

    if (!hotelId) return Toast.fail('Missing hotel id');
    if (!checkInDate || !checkOutDate) return Toast.fail('Please select dates');
    if (!selectedRoomTypeId) return Toast.fail('Please select a room type');
    if (!guestName || !guestPhone) return Toast.fail('Please complete guest info');

    Toast.loading({ message: 'Submitting...', forbidClick: true, duration: 0 });

    guestApi.getGuestByPhone(guestPhone)
      .then(guest => {
        if (!guest || !guest.guestId) {
          throw new Error('Guest not found');
        }

        return reservationApi.createReservation({
          hotelId,
          guestId: guest.guestId,
          typeId: selectedRoomTypeId,
          roomCount: 1,
          totalPrice,
          checkInDate,
          checkOutDate
        });
      })
      .then(orderId => {
        Toast.success('Order created');
        setTimeout(() => {
          wx.navigateTo({
            url: `/pages/payment/payment?orderId=${orderId}&amount=${totalPrice}`
          });
        }, 500);
      })
      .catch(err => {
        console.error('submit order error', err);
        Toast.fail('Guest info not available');
      })
      .finally(() => {
        Toast.clear();
      });
  }
});
