import { guestApi } from '../../api/guest.js';
import Dialog from '@vant/weapp/dialog/dialog';

/*

Page({
  data: {
    userInfo: null
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
      .then(res => {
        if (res) {
          this.setData({ userInfo: res });
        }
      })
      .catch(err => {
        console.error('获取用户信息失败', err);
      });
  },

  navigateToEdit() {
    wx.showToast({ title: '功能开发中', icon: 'none' });
  },

  handleLogout() {
    Dialog.confirm({
      title: '提示',
      message: '确定要退出登录吗？',
    }).then(() => {
      wx.removeStorageSync('token');
      wx.removeStorageSync('userPhone');
      
      this.setData({ userInfo: null });
      
      wx.reLaunch({
        url: '/pages/login/login'
      });
    }).catch(() => {
    });
  }
});
*/

Page({
  data: {
    userInfo: null
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
      .then(res => {
        this.setData({ userInfo: res || null });
      })
      .catch(err => {
        console.error('load profile error', err);
        this.setData({ userInfo: null });
      });
  },

  goToOrders() {
    wx.switchTab({ url: '/pages/order/list' });
  },

  goToLogin() {
    wx.navigateTo({ url: '/pages/login/login' });
  },

  navigateToEdit() {
    wx.showToast({ title: 'Coming soon', icon: 'none' });
  },

  handleLogout() {
    Dialog.confirm({
      title: 'Confirm',
      message: 'Logout now?'
    }).then(() => {
      wx.removeStorageSync('token');
      wx.removeStorageSync('userPhone');
      wx.removeStorageSync('pendingOpenId');
      this.setData({ userInfo: null });
      wx.reLaunch({ url: '/pages/login/login' });
    }).catch(() => {});
  }
});
