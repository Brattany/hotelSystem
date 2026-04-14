import { guestApi } from '../../api/guest.js';
import Dialog from '@vant/weapp/dialog/dialog';
import {
  DEFAULT_PROFILE_AVATAR,
  getAvatarCacheKey,
  normalizeProfileData,
  unwrapProfileResponse
} from '../../utils/profile.js';

const DEFAULT_AVATAR = DEFAULT_PROFILE_AVATAR;

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
        const userInfo = normalizeProfileData(unwrapProfileResponse(res), phone, DEFAULT_AVATAR);
        this.setData({ userInfo });
      })
      .catch((err) => {
        console.error('加载个人信息失败', err);
        this.setData({
          userInfo: normalizeProfileData({ phone }, phone, DEFAULT_AVATAR)
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
        const response = unwrapProfileResponse(res) || {};
        if (response.token) {
          getApp().saveSession(response.token);
        }

        const nextPhone = response.phone || userInfo.phone;
        const mergedUserInfo = normalizeProfileData({
          ...userInfo,
          ...response,
          guestId: response.guestId || userInfo.guestId || userInfo.id
        }, nextPhone, DEFAULT_AVATAR);

        if (userInfo.phone && userInfo.phone !== nextPhone) {
          wx.removeStorageSync(getAvatarCacheKey(userInfo.phone));
        }

        wx.setStorageSync(getAvatarCacheKey(nextPhone), mergedUserInfo.avatarUrl);
        this.setData({ userInfo: mergedUserInfo });
        wx.showToast({ title: '头像已更新', icon: 'success' });
      })
      .catch((err) => {
        console.error('头像同步失败', err);
        wx.showToast({ title: '头像已更新到本地', icon: 'none' });
      });
  },

  navigateToEdit() {
    if (!this.data.userInfo || !this.data.userInfo.phone) {
      wx.showToast({ title: '请先登录后再编辑资料', icon: 'none' });
      return;
    }

    wx.navigateTo({ url: '/pages/profile/infoEdit' });
  },

  handleLogout() {
    Dialog.confirm({
      title: '提示',
      message: '确认退出登录吗？'
    }).then(() => {
      const currentPhone = this.data.userInfo && this.data.userInfo.phone;
      console.log('[profile] user confirmed logout', { phone: currentPhone });

      getApp().clearSession({
        phone: currentPhone,
        manualLogout: true
      });

      this.setData({ userInfo: null });

      wx.showToast({ title: '已退出登录', icon: 'success' });
      setTimeout(() => {
        wx.reLaunch({ url: '/pages/login/login' });
      }, 400);
    }).catch(() => {});
  }
});
