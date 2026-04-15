import { guestApi } from './api/guest.js';
import { getAvatarCacheKey } from './utils/profile.js';

const MANUAL_LOGOUT_KEY = 'manualLogout';

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

    if (wx.getStorageSync(MANUAL_LOGOUT_KEY)) {
      console.log('[auth] skip silent login because user logged out manually');
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
    wx.removeStorageSync(MANUAL_LOGOUT_KEY);
    this.syncPhoneFromToken(token);
    return true;
  },

  clearSession(options = {}) {
    const {
      phone = wx.getStorageSync('userPhone'),
      manualLogout = false
    } = options;

    console.log('[auth] clearing local session', { phone, manualLogout });

    wx.removeStorageSync('token');
    wx.removeStorageSync('userPhone');
    wx.removeStorageSync('guestId');
    wx.removeStorageSync('pendingOpenId');

    if (phone) {
      wx.removeStorageSync(getAvatarCacheKey(phone));
    }

    if (manualLogout) {
      wx.setStorageSync(MANUAL_LOGOUT_KEY, true);
      return;
    }

    wx.removeStorageSync(MANUAL_LOGOUT_KEY);
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
