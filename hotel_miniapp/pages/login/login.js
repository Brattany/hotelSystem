import { guestApi } from '../../api/guest.js';
import Toast from '@vant/weapp/toast/toast';

const CODE_TEXT = '获取验证码';

Page({
  data: {
    phone: '',
    code: '',
    codeText: CODE_TEXT,
    codeDisabled: false
  },

  onPhoneChange(e) {
    this.setData({ phone: e.detail });
  },

  onCodeChange(e) {
    this.setData({ code: e.detail });
  },

  sendVerifyCode() {
    const { phone } = this.data;
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      Toast.fail('请输入正确的手机号');
      return;
    }

    guestApi.sendCode(phone)
      .then(() => {
        Toast.success('验证码已发送');
        this.startCountdown();
      })
      .catch(() => {});
  },

  startCountdown() {
    let seconds = 60;
    this.setData({ codeDisabled: true, codeText: `${seconds}秒` });

    const timer = setInterval(() => {
      seconds -= 1;

      if (seconds <= 0) {
        clearInterval(timer);
        this.setData({ codeDisabled: false, codeText: CODE_TEXT });
        return;
      }

      this.setData({ codeText: `${seconds}秒` });
    }, 1000);
  },

  handleLogin() {
    const { phone, code } = this.data;

    if (!phone || !code) {
      Toast.fail('请填写完整登录信息');
      return;
    }

    Toast.loading({ message: '登录中...', forbidClick: true });

    guestApi.login({
      phone,
      code,
      openId: wx.getStorageSync('pendingOpenId') || ''
    })
      .then(token => {
        const app = getApp();
        app.saveSession(token);
        wx.setStorageSync('userPhone', phone);
        Toast.success('登录成功');

        setTimeout(() => {
          wx.reLaunch({ url: '/pages/index/index' });
        }, 800);
      })
      .catch(err => {
        console.error('login error', err);
      });
  },

  goToRegister() {
    wx.navigateTo({ url: '/pages/login/register' });
  }
});
