import { roomTypeApi } from '../../api/roomType.js';
import { reservationApi } from '../../api/reservation.js';
import { guestApi } from '../../api/guest.js';
import Toast from '@vant/weapp/toast/toast';
import { addDays, diffDays, formatDate, validateStayDates } from '../../utils/date.js';

const HOTEL_CACHE_KEY = 'selectedHotel';

const extractArray = (res) => {
  if (Array.isArray(res)) return res;
  if (res && Array.isArray(res.data)) return res.data;
  if (res && res.data && Array.isArray(res.data.records)) return res.data.records;
  if (res && Array.isArray(res.records)) return res.records;
  return [];
};

const extractGuestId = (res) => {
  if (!res) return null;
  return res.guestId || res.id || (res.data && (res.data.guestId || res.data.id)) || null;
};

const extractOrderId = (res) => {
  if (!res) return null;
  return res.id || res.reservationId || res.orderId || (res.data && (res.data.id || res.data.reservationId || res.data.orderId)) || null;
};

const getBooleanLabel = (value, yesText, noText) => {
  if (typeof value === 'boolean') {
    return value ? yesText : noText;
  }

  return value || '';
};

const normalizeRoomType = (roomType, hotelCoverImage) => {
  const normalizedId = roomType.roomTypeId || roomType.typeId || roomType.id || null;
  const capacity = Number(roomType.capacity || roomType.peopleCount || roomType.maxGuests || 0);
  const availableCount = Number(roomType.availableCount || roomType.stock || roomType.roomCount || 0);
  const price = Number(roomType.price || roomType.roomPrice || roomType.amount || 0);
  const bedType = roomType.bedType || roomType.bedName || roomType.bed || roomType.roomBedType || '';
  const area = roomType.area || roomType.roomArea || roomType.square || roomType.size || roomType.acreage || '';
  const breakfast = getBooleanLabel(roomType.breakfast || roomType.hasBreakfast, '含早餐', '无早餐') || roomType.breakfastDesc || roomType.breakfastInfo || '';
  const cancelRule = roomType.cancelRule || roomType.cancelPolicy || roomType.refundRule || getBooleanLabel(roomType.freeCancel, '支持免费取消', '不可取消');
  const description = roomType.description || roomType.roomDesc || roomType.intro || roomType.remark || '';
  const imageUrl = roomType.image || roomType.imageUrl || roomType.coverImage || roomType.cover || roomType.roomTypeImage || roomType.roomTypeImg || hotelCoverImage || '../../assets/logo.jpg';
  const detailTags = [bedType, area ? `${area}${/㎡|m²|平/.test(String(area)) ? '' : '㎡'}` : '', capacity ? `可住 ${capacity} 人` : '']
    .filter(Boolean);

  return {
    ...roomType,
    roomTypeId: normalizedId,
    typeId: normalizedId,
    typeName: roomType.typeName || roomType.roomTypeName || roomType.name || '房型',
    price,
    capacity,
    availableCount,
    bedType,
    area,
    breakfast,
    cancelRule,
    description,
    imageUrl,
    detailTags,
    statusText: availableCount > 0 ? `剩余 ${availableCount} 间` : '可预订'
  };
};

const normalizeErrorMessage = (error, fallbackMessage) => {
  if (error && typeof error.message === 'string' && error.message.trim()) {
    return error.message;
  }

  return fallbackMessage;
};

const getCalendarRange = (event) => {
  const detail = event && event.detail;
  if (Array.isArray(detail)) {
    return detail;
  }

  if (detail && Array.isArray(detail.value)) {
    return detail.value;
  }

  return [];
};

const createGuestByApi = (guestName, guestPhone) => {
  const payload = { name: guestName, guestName, phone: guestPhone, guestPhone };
  const methodNames = ['createGuest', 'addGuest', 'saveGuest', 'registerGuest', 'create', 'add'];

  for (let i = 0; i < methodNames.length; i += 1) {
    const methodName = methodNames[i];
    if (guestApi && typeof guestApi[methodName] === 'function') {
      return guestApi[methodName](payload);
    }
  }

  return Promise.reject(new Error('当前账号暂无可用的入住人创建接口，请先确认已登录并完善用户信息'));
};

Page({
  data: {
    hotelId: null,
    hotelName: '',
    hotelCoverImage: '../../assets/logo.jpg',
    showCalendar: false,
    checkInDate: '',
    checkOutDate: '',
    dateString: '请选择入住和离店日期',
    days: 1,
    roomLoading: false,
    roomSearchDone: false,
    roomErrorMessage: '',
    availableRoomTypes: [],
    selectedRoomTypeId: null,
    selectedRoomInfo: null,
    roomCount: 1,
    selectedRoomPrice: 0,
    guestName: '',
    guestPhone: '',
    totalPrice: 0
  },

  onLoad(options) {
    const hotelId = Number(options.hotelId || 0);
    const hotelName = options.hotelName ? decodeURIComponent(options.hotelName) : '';

    if (!hotelId) {
      wx.showToast({ title: '缺少酒店信息', icon: 'none' });
      setTimeout(() => {
        wx.navigateBack();
      }, 500);
      return;
    }

    const cachedHotel = wx.getStorageSync(HOTEL_CACHE_KEY) || {};
    const today = formatDate(new Date());
    const tomorrow = formatDate(addDays(new Date(), 1));
    const currentDays = diffDays(today, tomorrow);
    const hotelCoverImage = cachedHotel.coverImage || cachedHotel.cover || cachedHotel.coverUrl || cachedHotel.image || cachedHotel.imageUrl || cachedHotel.img || cachedHotel.imgUrl || '../../assets/logo.jpg';

    this.setData({
      hotelId,
      hotelName: hotelName || cachedHotel.hotelName || cachedHotel.name || '',
      hotelCoverImage,
      checkInDate: today,
      checkOutDate: tomorrow,
      dateString: `${today} 至 ${tomorrow}`,
      days: currentDays,
      roomLoading: true,
      roomSearchDone: false,
      roomErrorMessage: ''
    }, () => {
      this.fetchCurrentGuestInfo();
      this.fetchAvailableRooms();
    });
  },

  fetchCurrentGuestInfo() {
    const phone = wx.getStorageSync('userPhone');
    if (!phone) {
      return;
    }

    guestApi.getGuestByPhone(phone)
      .then((res) => {
        const guest = res && res.data ? res.data : res;
        if (!guest) {
          return;
        }

        this.setData({
          guestName: guest.name || guest.guestName || '',
          guestPhone: guest.phone || guest.guestPhone || phone
        });
      })
      .catch((err) => {
        console.error('获取入住人信息失败', err);
      });
  },

  onDisplayCalendar() {
    this.setData({ showCalendar: true });
  },

  onCloseCalendar() {
    this.setData({ showCalendar: false });
  },

  onConfirmCalendar(event) {
    const dates = getCalendarRange(event);
    const start = dates[0];
    const end = dates[1];

    if (!start) {
      Toast.fail('入住日期不能为空');
      return;
    }

    if (!end) {
      Toast.fail('离店日期不能为空');
      return;
    }

    const checkInDate = formatDate(start);
    const checkOutDate = formatDate(end);
    const validationMessage = validateStayDates(checkInDate, checkOutDate);
    if (validationMessage) {
      Toast.fail(validationMessage);
      return;
    }

    this.setData({
      showCalendar: false,
      checkInDate,
      checkOutDate,
      dateString: `${checkInDate} 至 ${checkOutDate}`,
      days: diffDays(checkInDate, checkOutDate),
      roomLoading: true,
      roomSearchDone: false,
      roomErrorMessage: '',
      availableRoomTypes: [],
      selectedRoomTypeId: null,
      selectedRoomInfo: null,
      roomCount: 1,
      selectedRoomPrice: 0,
      totalPrice: 0
    }, () => {
      this.fetchAvailableRooms();
    });
  },

  fetchAvailableRooms() {
    const { hotelId, checkInDate, checkOutDate, hotelCoverImage } = this.data;
    if (!hotelId || !checkInDate || !checkOutDate) {
      return;
    }

    const validationMessage = validateStayDates(checkInDate, checkOutDate);
    if (validationMessage) {
      this.setData({
        roomLoading: false,
        roomSearchDone: false,
        roomErrorMessage: validationMessage
      });
      return;
    }

    Toast.loading({ message: '查询房型中...', forbidClick: true, duration: 0 });

    roomTypeApi.getAvailableRoomTypes({
      hotelId,
      checkInDate,
      checkOutDate
    })
      .then((res) => {
        const roomList = extractArray(res).map((roomType) => normalizeRoomType(roomType, hotelCoverImage));

        Toast.clear();
        this.setData({
          roomLoading: false,
          roomSearchDone: true,
          roomErrorMessage: '',
          availableRoomTypes: roomList,
          selectedRoomTypeId: null,
          selectedRoomInfo: null,
          roomCount: 1,
          selectedRoomPrice: 0,
          totalPrice: 0
        });
      })
      .catch((err) => {
        console.error('查询可用房型失败', err);
        Toast.clear();
        Toast.fail('查询房型失败');

        this.setData({
          roomLoading: false,
          roomSearchDone: true,
          roomErrorMessage: normalizeErrorMessage(err, '日期已更新，但房型暂时加载失败，请稍后重试。'),
          availableRoomTypes: [],
          selectedRoomTypeId: null,
          selectedRoomInfo: null,
          roomCount: 1,
          selectedRoomPrice: 0,
          totalPrice: 0
        });
      });
  },

  retryFetchRooms() {
    const validationMessage = validateStayDates(this.data.checkInDate, this.data.checkOutDate);
    if (validationMessage) {
      Toast.fail(validationMessage);
      return;
    }

    this.setData({
      roomLoading: true,
      roomSearchDone: false,
      roomErrorMessage: ''
    }, () => {
      this.fetchAvailableRooms();
    });
  },

  selectRoomType(event) {
    const roomType = (event.currentTarget && event.currentTarget.dataset && event.currentTarget.dataset.item) || {};
    const roomTypeId = roomType.roomTypeId || roomType.typeId || roomType.id || null;
    const roomPrice = Number(roomType.price || roomType.roomPrice || 0);

    this.setData({
      selectedRoomTypeId: roomTypeId,
      selectedRoomInfo: roomType,
      roomCount: 1,
      selectedRoomPrice: roomPrice,
      totalPrice: roomPrice * this.data.days
    });
  },

  updateRoomCount(nextCount) {
    const { selectedRoomInfo, days, selectedRoomPrice } = this.data;
    const maxCount = Number(selectedRoomInfo && selectedRoomInfo.availableCount) || 0;

    if (!Number.isInteger(nextCount) || nextCount < 1) {
      Toast.fail('房间数量至少为 1');
      return;
    }

    if (maxCount > 0 && nextCount > maxCount) {
      Toast.fail('预订数量不能超过可用房量');
      return;
    }

    this.setData({
      roomCount: nextCount,
      totalPrice: selectedRoomPrice * days * nextCount
    });
  },

  decreaseRoomCount() {
    this.updateRoomCount(this.data.roomCount - 1);
  },

  increaseRoomCount() {
    this.updateRoomCount(this.data.roomCount + 1);
  },

  onNameChange(event) {
    this.setData({ guestName: (event.detail || '').trim() });
  },

  onPhoneChange(event) {
    this.setData({ guestPhone: (event.detail || '').trim() });
  },

  ensureGuestId() {
    const { guestName, guestPhone } = this.data;

    return guestApi.getGuestByPhone(guestPhone)
      .then((res) => {
        const guest = res && res.data ? res.data : res;
        const guestId = extractGuestId(guest);

        if (guestId) {
          return guestId;
        }

        return createGuestByApi(guestName, guestPhone).then((createRes) => {
          const createdGuest = createRes && createRes.data ? createRes.data : createRes;
          const createdGuestId = extractGuestId(createdGuest);

          if (!createdGuestId) {
            throw new Error('入住人创建成功，但未返回入住人编号');
          }

          return createdGuestId;
        });
      })
      .catch((err) => {
        console.warn('未查询到入住人信息，尝试自动创建', err);

        return createGuestByApi(guestName, guestPhone).then((createRes) => {
          const createdGuest = createRes && createRes.data ? createRes.data : createRes;
          const createdGuestId = extractGuestId(createdGuest);

          if (!createdGuestId) {
            throw new Error('入住人创建成功，但未返回入住人编号');
          }

          return createdGuestId;
        });
      });
  },

  onSubmitOrder() {
    const { hotelId, checkInDate, checkOutDate, selectedRoomTypeId, selectedRoomInfo, roomCount, guestName, guestPhone, totalPrice } = this.data;
    const validationMessage = validateStayDates(checkInDate, checkOutDate);
    const maxCount = Number(selectedRoomInfo && selectedRoomInfo.availableCount) || 0;

    if (!hotelId) return Toast.fail('缺少酒店信息');
    if (validationMessage) return Toast.fail(validationMessage);
    if (!selectedRoomTypeId) return Toast.fail('请先选择房型');
    if (!Number.isInteger(roomCount) || roomCount < 1) return Toast.fail('房间数量至少为 1');
    if (maxCount > 0 && roomCount > maxCount) return Toast.fail('预订数量不能超过可用房量');
    if (!guestName || !guestPhone) return Toast.fail('请完善入住人信息');

    Toast.loading({ message: '提交订单中...', forbidClick: true, duration: 0 });

    this.ensureGuestId()
      .then((guestId) => {
        const reservationData = {
          hotelId,
          guestId,
          roomTypeId: selectedRoomTypeId,
          typeId: selectedRoomTypeId,
          roomCount,
          totalPrice,
          guestName,
          guestPhone,
          checkInDate,
          checkOutDate,
          status: 0
        };

        return reservationApi.createReservation(reservationData);
      })
      .then((res) => {
        const orderId = extractOrderId(res);
        Toast.clear();

        if (!orderId) {
          throw new Error('订单创建成功，但未返回订单号');
        }

        Toast.success('订单提交成功');
        setTimeout(() => {
          wx.navigateTo({ url: `/pages/payment/payment?orderId=${orderId}&amount=${totalPrice}` });
        }, 800);
      })
      .catch((err) => {
        console.error('提交订单失败', err);
        Toast.clear();
        Toast.fail(err && err.message ? err.message : '提交订单失败');
      });
  }
});