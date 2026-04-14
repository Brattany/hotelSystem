import { roomTypeApi } from '../../api/roomType.js';
import { reservationApi } from '../../api/reservation.js';
import { guestApi } from '../../api/guest.js';
import Dialog from '@vant/weapp/dialog/dialog';
import Toast from '@vant/weapp/toast/toast';
import { addDays, diffDays, formatDate, validateStayDates } from '../../utils/date.js';

const HOTEL_CACHE_KEY = 'selectedHotel';
const RESERVATION_DRAFT_KEY_PREFIX = 'reservationDraft';

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
  if (typeof res === 'number' || typeof res === 'string') {
    return res;
  }

  return res.id || res.reservationId || res.orderId || (res.data && (res.data.id || res.data.reservationId || res.data.orderId)) || null;
};

const extractOrderMeta = (res) => {
  if (!res) {
    return { reservationId: null, orderNo: '' };
  }

  if (typeof res === 'number' || typeof res === 'string') {
    return {
      reservationId: res,
      orderNo: String(res)
    };
  }

  const candidates = [res, res.data].filter(Boolean);

  for (let i = 0; i < candidates.length; i += 1) {
    const candidate = candidates[i];
    const reservationId = candidate.reservationId || candidate.id || candidate.orderId || null;
    const orderNo = candidate.orderNo || candidate.orderCode || candidate.order_id || (reservationId ? String(reservationId) : '');

    if (reservationId || orderNo) {
      return { reservationId, orderNo };
    }
  }

  return { reservationId: null, orderNo: '' };
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
  const detailTags = [bedType, area ? `${area}${/㎡|m²|平/.test(String(area)) ? '' : '㎡'}` : '']
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

const containsAny = (text, keywords) => keywords.some((keyword) => text.includes(keyword));

const buildRoomLoadErrorMessage = (error, params = {}) => {
  const { hotelId, checkInDate, checkOutDate } = params;
  const dateValidationMessage = validateStayDates(checkInDate, checkOutDate);
  const statusCode = Number(error && error.statusCode) || 0;
  const code = Number(error && error.code) || 0;
  const response = (error && error.response) || {};
  const detailText = [
    error && error.message,
    response && response.message,
    response && response.error
  ]
    .filter(Boolean)
    .join(' | ');

  if (!hotelId) {
    return '缺少 hotelId，无法查询当前酒店房型。';
  }

  if (dateValidationMessage) {
    return dateValidationMessage;
  }

  if ((error && error.unauthorized) || statusCode === 401 || code === 401) {
    return '登录状态已失效，请重新登录后再查询房型。';
  }

  if (containsAny(detailText, ['LocalDate', 'DateTimeParseException', 'Cannot deserialize value of type `java.time.LocalDate`'])) {
    return '日期格式错误，房型查询接口要求 yyyy-MM-dd。';
  }

  if (containsAny(detailText, ['hotelId', 'HotelId'])) {
    return 'hotelId 缺失或无效，无法查询当前酒店房型。';
  }

  if (statusCode >= 500 || code === 500) {
    return detailText ? `后端房型接口异常：${detailText}` : '后端房型接口返回 500，请检查服务日志。';
  }

  return normalizeErrorMessage(error, '房型暂时加载失败，请稍后重试。');
};

const buildSubmitOrderErrorMessage = (error) => {
  const statusCode = Number(error && error.statusCode) || 0;
  const code = Number(error && error.code) || 0;
  const response = (error && error.response) || {};
  const detailText = [
    error && error.message,
    response && response.message,
    response && response.error
  ]
    .filter(Boolean)
    .join(' | ');

  if ((error && error.unauthorized) || statusCode === 401 || code === 401) {
    return '登录状态已失效，请重新登录后再提交订单。';
  }

  if (containsAny(detailText, ['LocalDate', 'DateTimeParseException', 'Cannot deserialize value of type `java.time.LocalDate`'])) {
    return '日期格式错误，请重新选择入住与离店日期后再提交。';
  }

  if (containsAny(detailText, ['hotelId', 'HotelId'])) {
    return '缺少酒店信息，暂时无法提交订单。';
  }

  if (containsAny(detailText, ['roomTypeId', 'typeId'])) {
    return '缺少房型信息，请重新选择房型后再提交。';
  }

  if (statusCode >= 500 || code === 500) {
    return detailText ? `订单提交失败：${detailText}` : '后端提交订单接口异常，请稍后重试。';
  }

  return normalizeErrorMessage(error, '提交订单失败，请稍后重试。');
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
    submittingOrder: false,
    guestName: '',
    guestPhone: '',
    totalPrice: 0
  },

  onLoad(options) {
    this.shouldPersistDraft = true;
    this.shouldRefreshOnShow = false;

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
    const draft = this.getReservationDraft(hotelId);
    const today = formatDate(new Date());
    const tomorrow = formatDate(addDays(new Date(), 1));
    const currentCheckInDate = draft && draft.checkInDate ? draft.checkInDate : today;
    const currentCheckOutDate = draft && draft.checkOutDate ? draft.checkOutDate : tomorrow;
    const currentDays = diffDays(currentCheckInDate, currentCheckOutDate);
    const hotelCoverImage = cachedHotel.coverImage || cachedHotel.cover || cachedHotel.coverUrl || cachedHotel.image || cachedHotel.imageUrl || cachedHotel.img || cachedHotel.imgUrl || '../../assets/logo.jpg';

    this.setData({
      hotelId,
      hotelName: hotelName || (draft && draft.hotelName) || cachedHotel.hotelName || cachedHotel.name || '',
      hotelCoverImage: (draft && draft.hotelCoverImage) || hotelCoverImage,
      checkInDate: currentCheckInDate,
      checkOutDate: currentCheckOutDate,
      dateString: `${currentCheckInDate} 至 ${currentCheckOutDate}`,
      days: currentDays,
      roomLoading: true,
      roomSearchDone: false,
      roomErrorMessage: '',
      selectedRoomTypeId: (draft && draft.selectedRoomTypeId) || null,
      roomCount: (draft && draft.roomCount) || 1,
      guestName: (draft && draft.guestName) || '',
      guestPhone: (draft && draft.guestPhone) || ''
    }, () => {
      this.fetchCurrentGuestInfo();
      this.fetchAvailableRooms();
    });
  },

  onShow() {
    if (this.shouldRefreshOnShow) {
      this.shouldRefreshOnShow = false;
      this.shouldPersistDraft = true;
      this.refreshReservationView();
      return;
    }

    this.shouldPersistDraft = true;
  },

  onHide() {
    this.saveReservationDraft();
  },

  onUnload() {
    this.saveReservationDraft();
  },

  getDraftStorageKey(hotelId = this.data.hotelId) {
    return hotelId ? `${RESERVATION_DRAFT_KEY_PREFIX}:${hotelId}` : RESERVATION_DRAFT_KEY_PREFIX;
  },

  getReservationDraft(hotelId) {
    if (!hotelId) {
      return null;
    }

    const draft = wx.getStorageSync(this.getDraftStorageKey(hotelId));

    if (!draft || Number(draft.hotelId || 0) !== Number(hotelId)) {
      return null;
    }

    const checkInDate = draft.checkInDate || '';
    const checkOutDate = draft.checkOutDate || '';
    const validationMessage = validateStayDates(checkInDate, checkOutDate);

    if (validationMessage) {
      return null;
    }

    return {
      hotelId,
      hotelName: draft.hotelName || '',
      hotelCoverImage: draft.hotelCoverImage || '',
      checkInDate,
      checkOutDate,
      selectedRoomTypeId: draft.selectedRoomTypeId || null,
      roomCount: Math.max(Number(draft.roomCount || 1), 1),
      guestName: draft.guestName || '',
      guestPhone: draft.guestPhone || ''
    };
  },

  saveReservationDraft(extraData = {}) {
    if (this.shouldPersistDraft === false) {
      return;
    }

    const snapshot = {
      ...this.data,
      ...extraData
    };
    const hotelId = Number(snapshot.hotelId || 0);

    if (!hotelId) {
      return;
    }

    wx.setStorageSync(this.getDraftStorageKey(hotelId), {
      hotelId,
      hotelName: snapshot.hotelName || '',
      hotelCoverImage: snapshot.hotelCoverImage || '',
      checkInDate: snapshot.checkInDate || '',
      checkOutDate: snapshot.checkOutDate || '',
      selectedRoomTypeId: snapshot.selectedRoomTypeId || null,
      roomCount: Math.max(Number(snapshot.roomCount || 1), 1),
      guestName: snapshot.guestName || '',
      guestPhone: snapshot.guestPhone || ''
    });
  },

  clearReservationDraft(options = {}) {
    const hotelId = Number(options.hotelId || this.data.hotelId || 0);

    if (hotelId) {
      wx.removeStorageSync(this.getDraftStorageKey(hotelId));
    }

    if (options.disablePersist) {
      this.shouldPersistDraft = false;
    }
  },

  buildSelectionState(roomList) {
    const { selectedRoomTypeId, roomCount, days } = this.data;
    const defaultState = {
      selectedRoomTypeId: null,
      selectedRoomInfo: null,
      roomCount: 1,
      selectedRoomPrice: 0,
      totalPrice: 0
    };

    if (!selectedRoomTypeId) {
      return defaultState;
    }

    const matchedRoom = roomList.find((roomType) => Number(roomType.roomTypeId) === Number(selectedRoomTypeId));

    if (!matchedRoom) {
      return defaultState;
    }

    const maxCount = Number(matchedRoom.availableCount) || 0;
    const safeRoomCount = Math.max(Number(roomCount || 1), 1);
    const nextRoomCount = maxCount > 0 ? Math.min(safeRoomCount, maxCount) : safeRoomCount;
    const selectedRoomPrice = Number(matchedRoom.price || 0);

    return {
      selectedRoomTypeId: matchedRoom.roomTypeId,
      selectedRoomInfo: matchedRoom,
      roomCount: nextRoomCount,
      selectedRoomPrice,
      totalPrice: selectedRoomPrice * days * nextRoomCount
    };
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

        const nextGuestName = this.data.guestName || guest.name || guest.guestName || '';
        const nextGuestPhone = this.data.guestPhone || guest.phone || guest.guestPhone || phone;

        if (nextGuestName === this.data.guestName && nextGuestPhone === this.data.guestPhone) {
          return;
        }

        this.setData({
          guestName: nextGuestName,
          guestPhone: nextGuestPhone
        }, () => {
          this.saveReservationDraft();
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
      availableRoomTypes: []
    }, () => {
      this.saveReservationDraft();
      this.fetchAvailableRooms();
    });
  },

  fetchAvailableRooms() {
    const { hotelId, checkInDate, checkOutDate, hotelCoverImage } = this.data;
    const roomRequestPayload = {
      hotelId,
      checkInDate,
      checkOutDate
    };
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

    roomTypeApi.getAvailableRoomTypes(roomRequestPayload)
      .then((res) => {
        const roomList = extractArray(res).map((roomType) => normalizeRoomType(roomType, hotelCoverImage));
        const nextSelectionState = this.buildSelectionState(roomList);

        Toast.clear();
        this.setData({
          roomLoading: false,
          roomSearchDone: true,
          roomErrorMessage: '',
          availableRoomTypes: roomList,
          ...nextSelectionState
        }, () => {
          this.saveReservationDraft();
        });
      })
      .catch((err) => {
        console.error('查询可用房型失败', err);
        Toast.clear();
        Toast.fail('查询房型失败');

        this.setData({
          roomLoading: false,
          roomSearchDone: true,
          roomErrorMessage: buildRoomLoadErrorMessage(err, roomRequestPayload),
          availableRoomTypes: []
        }, () => {
          this.saveReservationDraft();
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
      this.saveReservationDraft();
      this.fetchAvailableRooms();
    });
  },

  refreshReservationView() {
    const { checkInDate, checkOutDate } = this.data;
    const validationMessage = validateStayDates(checkInDate, checkOutDate);

    this.setData({
      roomLoading: !validationMessage,
      roomSearchDone: false,
      roomErrorMessage: validationMessage || '',
      availableRoomTypes: [],
      selectedRoomTypeId: null,
      selectedRoomInfo: null,
      roomCount: 1,
      selectedRoomPrice: 0,
      totalPrice: 0
    }, () => {
      this.saveReservationDraft();
      this.fetchCurrentGuestInfo();
      if (!validationMessage) {
        this.fetchAvailableRooms();
      }
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
    }, () => {
      this.saveReservationDraft();
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
    }, () => {
      this.saveReservationDraft();
    });
  },

  decreaseRoomCount() {
    this.updateRoomCount(this.data.roomCount - 1);
  },

  increaseRoomCount() {
    this.updateRoomCount(this.data.roomCount + 1);
  },

  onNameChange(event) {
    this.setData({ guestName: (event.detail || '').trim() }, () => {
      this.saveReservationDraft();
    });
  },

  onPhoneChange(event) {
    this.setData({ guestPhone: (event.detail || '').trim() }, () => {
      this.saveReservationDraft();
    });
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
    const { hotelId, checkInDate, checkOutDate, selectedRoomTypeId, selectedRoomInfo, roomCount, guestName, guestPhone, totalPrice, submittingOrder } = this.data;
    if (submittingOrder) return;
    const validationMessage = validateStayDates(checkInDate, checkOutDate);
    const maxCount = Number(selectedRoomInfo && selectedRoomInfo.availableCount) || 0;

    if (!hotelId) return Toast.fail('缺少酒店信息');
    if (validationMessage) return Toast.fail(validationMessage);
    if (!selectedRoomTypeId) return Toast.fail('请先选择房型');
    if (!Number.isInteger(roomCount) || roomCount < 1) return Toast.fail('房间数量至少为 1');
    if (maxCount > 0 && roomCount > maxCount) return Toast.fail('预订数量不能超过可用房量');
    if (!guestName || !guestPhone) return Toast.fail('请完善入住人信息');

    this.setData({ submittingOrder: true });
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
        const { reservationId, orderNo } = extractOrderMeta(res);
        const orderId = extractOrderId(reservationId || orderNo);
        const displayOrderNo = orderNo || (reservationId ? String(reservationId) : '');
        Toast.clear();

        if (!orderId) {
          throw new Error('订单创建成功，但未返回订单标识');
        }

        return Dialog.confirm({
          title: '订单提交成功',
          message: `订单已创建成功。\n预订编号：${reservationId || orderId}${displayOrderNo ? `\n订单号：${displayOrderNo}` : ''}\n可继续前往支付，或返回当前页重新浏览房型。`,
          confirmButtonText: '去支付',
          cancelButtonText: '留在当前页',
          showCancelButton: true
        }).then(() => {
          this.clearReservationDraft({ disablePersist: true });
          this.shouldRefreshOnShow = true;
          wx.navigateTo({
            url: `/pages/payment/payment?orderId=${reservationId || orderId}&amount=${totalPrice || 0}`
          });
        }).catch(() => {
          this.clearReservationDraft();
          this.refreshReservationView();
        });
      })
      .catch((err) => {
        console.error('提交订单失败', err);
        Toast.clear();
        return Dialog.alert({
          title: '提交订单失败',
          message: buildSubmitOrderErrorMessage(err),
          confirmButtonText: '我知道了'
        });
      })
      .finally(() => {
        this.setData({ submittingOrder: false });
      });
  }
});
