import { guestApi } from '../../api/guest.js';
import Toast from '@vant/weapp/toast/toast';

/*

Page({
  data: {
    phone: '',
    password: ''
  },

  onPhoneChange(e) {
    this.setData({ phone: e.detail });
  },

  onPasswordChange(e) {
    this.setData({ password: e.detail });
  },

  // 处理登录逻辑
  handleLogin() {
    const { phone, password } = this.data;

    if (!phone || !password) {
      Toast.fail('请填写完整信息');
      return;
    }

    Toast.loading({ message: '登录中...', forbidClick: true });

    guestApi.login({ phone, password })
      .then(token => {
        Toast.success('登录成功');
        
        wx.setStorageSync('token', token);
        
        wx.setStorageSync('userPhone', phone); 
        
        setTimeout(() => {
          wx.reLaunch({ url: '/pages/index/index' });
        }, 1000);
      })
      .catch(err => {
        console.error("登录异常", err);
      });
  },

  goToRegister() {
    wx.navigateTo({ url: '/pages/login/register' }); 
  }
});
*/

Page({
  data: {
    phone: '',
    code: '',
    codeText: 'Send Code',
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
      Toast.fail('Invalid phone');
      return;
    }

    guestApi.sendCode(phone)
      .then(() => {
        Toast.success('Code sent');
        this.startCountdown();
      })
      .catch(() => {});
  },

  startCountdown() {
    let seconds = 60;
    this.setData({ codeDisabled: true, codeText: `${seconds}s` });

    const timer = setInterval(() => {
      seconds -= 1;

      if (seconds <= 0) {
        clearInterval(timer);
        this.setData({ codeDisabled: false, codeText: 'Send Code' });
        return;
      }

      this.setData({ codeText: `${seconds}s` });
    }, 1000);
  },

  handleLogin() {
    const { phone, code } = this.data;

    if (!phone || !code) {
      Toast.fail('Please complete login info');
      return;
    }

    Toast.loading({ message: 'Logging in...', forbidClick: true });

    guestApi.login({
      phone,
      code,
      openId: wx.getStorageSync('pendingOpenId') || ''
    })
      .then(token => {
        const app = getApp();
        app.saveSession(token);
        wx.setStorageSync('userPhone', phone);
        Toast.success('Login success');

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
