import { guestApi } from '../../api/guest.js';
import Dialog from '@vant/weapp/dialog/dialog';

const DEFAULT_AVATAR = '../../assets/logo.jpg';

const getAvatarCacheKey = (phone) => `profileAvatar:${phone || 'guest'}`;
const unwrapResponse = (res) => (res && res.data ? res.data : res);

const normalizeUserInfo = (rawUserInfo, phone) => {
  const source = rawUserInfo || {};
  const profilePhone = source.phone || source.guestPhone || phone || '';
  const profileName = source.name || source.guestName || source.nickName || '游客';
  const cachedAvatar = wx.getStorageSync(getAvatarCacheKey(profilePhone));
  const avatarUrl = source.avatarUrl || source.avatar || source.headImg || source.headImage || source.headimgurl || cachedAvatar || DEFAULT_AVATAR;

  return {
    ...source,
    phone: profilePhone,
    guestPhone: profilePhone,
    name: profileName,
    guestName: profileName,
    avatarUrl
  };
};

Page({
  data: {
    userInfo: null,
    defaultAvatar: DEFAULT_AVATAR
  },

  onShow() {
    this.loadUserData();
  },

  loadUserData() {
    const phone = wx.getStorageSync('userPhone');
    if (!phone) {
      this.setData({ userInfo: null });
      return;
    }

    guestApi.getGuestByPhone(phone)
      .then((res) => {
        const userInfo = normalizeUserInfo(unwrapResponse(res), phone);
        this.setData({ userInfo });
      })
      .catch((err) => {
        console.error('加载个人信息失败', err);
        this.setData({
          userInfo: normalizeUserInfo({ phone }, phone)
        });
      });
  },

  goToOrders() {
    wx.switchTab({ url: '/pages/order/list' });
  },

  goToLogin() {
    wx.navigateTo({ url: '/pages/login/login' });
  },

  onChooseAvatar(event) {
    const avatarUrl = event.detail && event.detail.avatarUrl;
    if (!avatarUrl) {
      wx.showToast({ title: '未获取到头像', icon: 'none' });
      return;
    }

    this.applyAvatar(avatarUrl);
  },

  chooseAvatarFromAlbum() {
    if (!this.data.userInfo || !this.data.userInfo.phone) {
      wx.showToast({ title: '请先登录后再设置头像', icon: 'none' });
      return;
    }

    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const avatarUrl = (res.tempFilePaths && res.tempFilePaths[0]) || '';
        if (!avatarUrl) {
          wx.showToast({ title: '头像选择失败', icon: 'none' });
          return;
        }

        this.applyAvatar(avatarUrl);
      },
      fail: (err) => {
        if (err && err.errMsg && err.errMsg.includes('cancel')) {
          return;
        }

        console.error('选择头像失败', err);
        wx.showToast({ title: '头像选择失败', icon: 'none' });
      }
    });
  },

  applyAvatar(avatarUrl) {
    const { userInfo } = this.data;
    if (!userInfo || !userInfo.phone) {
      wx.showToast({ title: '请先登录后再设置头像', icon: 'none' });
      return;
    }

    const nextUserInfo = {
      ...userInfo,
      avatarUrl
    };

    wx.setStorageSync(getAvatarCacheKey(userInfo.phone), avatarUrl);
    this.setData({ userInfo: nextUserInfo });
    this.syncAvatarToServer(nextUserInfo);
  },

  syncAvatarToServer(userInfo) {
    const payload = {
      ...userInfo,
      id: userInfo.id || userInfo.guestId,
      guestId: userInfo.guestId || userInfo.id,
      phone: userInfo.phone,
      guestPhone: userInfo.phone,
      name: userInfo.name,
      guestName: userInfo.name,
      avatarUrl: userInfo.avatarUrl,
      avatar: userInfo.avatarUrl,
      headImg: userInfo.avatarUrl
    };

    guestApi.updateGuestInfo(payload)
      .then((res) => {
        const mergedUserInfo = normalizeUserInfo({
          ...userInfo,
          ...unwrapResponse(res)
        }, userInfo.phone);

        wx.setStorageSync(getAvatarCacheKey(userInfo.phone), mergedUserInfo.avatarUrl);
        this.setData({ userInfo: mergedUserInfo });
        wx.showToast({ title: '头像已更新', icon: 'success' });
      })
      .catch((err) => {
        console.error('头像同步失败', err);
        wx.showToast({ title: '头像已更新到本地', icon: 'none' });
      });
  },

  navigateToEdit() {
    wx.showToast({ title: '个人资料编辑功能开发中', icon: 'none' });
  },

  handleLogout() {
    Dialog.confirm({
      title: '提示',
      message: '确认退出登录吗？'
    }).then(() => {
      wx.removeStorageSync('token');
      wx.removeStorageSync('userPhone');
      wx.removeStorageSync('pendingOpenId');
      this.setData({ userInfo: null });
      wx.reLaunch({ url: '/pages/login/login' });
    }).catch(() => {});
  }
});