import { guestApi } from './api/guest.js';

function parseJwtPayload(token) {
  try {
    const payload = token.split('.')[1];
    if (!payload) return null;

    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
    const padded = normalized + '='.repeat((4 - (normalized.length % 4 || 4)) % 4);
    const buffer = wx.base64ToArrayBuffer(padded);
    const bytes = new Uint8Array(buffer);
    const encoded = Array.from(bytes)
      .map(byte => `%${byte.toString(16).padStart(2, '0')}`)
      .join('');

    return JSON.parse(decodeURIComponent(encoded));
  } catch (error) {
    return null;
  }
}

App({
  onLaunch() {
    this.checkLoginStatus();
  },

  checkLoginStatus() {
    const token = wx.getStorageSync('token');

    if (token) {
      this.syncPhoneFromToken(token);
      return;
    }

    this.doSilentLogin();
  },

  saveSession(token) {
    if (!token) {
      return false;
    }

    if (typeof token === 'string' && token.startsWith('UNBOUND:')) {
      const openId = token.replace('UNBOUND:', '');
      wx.removeStorageSync('token');
      wx.removeStorageSync('userPhone');
      wx.setStorageSync('pendingOpenId', openId);
      return false;
    }

    wx.setStorageSync('token', token);
    wx.removeStorageSync('pendingOpenId');
    this.syncPhoneFromToken(token);
    return true;
  },

  syncPhoneFromToken(token) {
    const payload = parseJwtPayload(token);
    if (payload && payload.phone) {
      wx.setStorageSync('userPhone', payload.phone);
      return true;
    }
    return false;
  },

  doSilentLogin() {
    wx.login({
      success: res => {
        if (!res.code) return;

        guestApi.wxLogin(res.code)
          .then(token => {
            this.saveSession(token);
          })
          .catch(err => {
            console.log('静默登录失败，请手动登录', err);
          });
      }
    });
  }
});
