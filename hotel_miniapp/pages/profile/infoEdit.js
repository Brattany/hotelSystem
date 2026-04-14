import { guestApi } from '../../api/guest.js';
import {
  DEFAULT_PROFILE_AVATAR,
  getAvatarCacheKey,
  normalizeProfileData,
  unwrapProfileResponse
} from '../../utils/profile.js';

const DEFAULT_AVATAR = DEFAULT_PROFILE_AVATAR;
const PHONE_PATTERN = /^1[3-9]\d{9}$/;

Page({
  data: {
    loading: true,
    saving: false,
    defaultAvatar: DEFAULT_AVATAR,
    originalPhone: '',
    form: {
      guestId: null,
      name: '',
      phone: '',
      idCard: '',
      openId: '',
      avatarUrl: DEFAULT_AVATAR
    }
  },

  onLoad() {
    this.loadProfile();
  },

  loadProfile() {
    const phone = wx.getStorageSync('userPhone');
    if (!phone) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      setTimeout(() => wx.navigateBack(), 500);
      return;
    }

    wx.showLoading({ title: '加载中...' });

    guestApi.getGuestByPhone(phone)
      .then((res) => {
        const profile = normalizeProfileData(unwrapProfileResponse(res), phone, DEFAULT_AVATAR);

        this.setData({
          loading: false,
          originalPhone: profile.phone,
          form: profile
        });
      })
      .catch((err) => {
        console.error('加载个人资料失败', err);
        wx.showToast({ title: '资料加载失败', icon: 'none' });
        this.setData({
          loading: false,
          originalPhone: phone,
          form: normalizeProfileData({ phone }, phone, DEFAULT_AVATAR)
        });
      })
      .finally(() => {
        wx.hideLoading();
      });
  },

  onFieldChange(event) {
    const field = event.currentTarget && event.currentTarget.dataset && event.currentTarget.dataset.field;
    if (!field) {
      return;
    }

    this.setData({
      [`form.${field}`]: typeof event.detail === 'string' ? event.detail.trim() : event.detail
    });
  },

  onChooseAvatar(event) {
    const avatarUrl = event.detail && event.detail.avatarUrl;
    if (!avatarUrl) {
      wx.showToast({ title: '未获取到头像', icon: 'none' });
      return;
    }

    this.setData({ 'form.avatarUrl': avatarUrl });
  },

  chooseAvatarFromAlbum() {
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

        this.setData({ 'form.avatarUrl': avatarUrl });
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

  validateForm() {
    const { name, phone } = this.data.form;

    if (!name || !name.trim()) {
      return '用户名不能为空';
    }

    if (!PHONE_PATTERN.test(phone || '')) {
      return '请输入正确的手机号';
    }

    return '';
  },

  handleSave() {
    if (this.data.saving) {
      return;
    }

    const validationMessage = this.validateForm();
    if (validationMessage) {
      wx.showToast({ title: validationMessage, icon: 'none' });
      return;
    }

    const { form, originalPhone } = this.data;
    const payload = {
      guestId: form.guestId,
      name: form.name.trim(),
      phone: form.phone.trim(),
      idCard: (form.idCard || '').trim(),
      openId: form.openId || ''
    };

    this.setData({ saving: true });
    wx.showLoading({ title: '保存中...' });

    guestApi.updateGuestInfo(payload)
      .then((res) => {
        const response = unwrapProfileResponse(res) || {};
        const nextPhone = response.phone || payload.phone;
        const avatarUrl = form.avatarUrl || DEFAULT_AVATAR;

        if (response.token) {
          getApp().saveSession(response.token);
        } else {
          wx.setStorageSync('userPhone', nextPhone);
        }

        if (originalPhone && originalPhone !== nextPhone) {
          wx.removeStorageSync(getAvatarCacheKey(originalPhone));
        }

        wx.setStorageSync(getAvatarCacheKey(nextPhone), avatarUrl);
        wx.showToast({ title: '保存成功', icon: 'success' });

        setTimeout(() => {
          wx.navigateBack();
        }, 600);
      })
      .catch((err) => {
        console.error('保存个人资料失败', err);
        wx.showToast({
          title: (err && err.message) || '保存失败，请稍后重试',
          icon: 'none'
        });
      })
      .finally(() => {
        wx.hideLoading();
        this.setData({ saving: false });
      });
  }
});
