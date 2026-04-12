import { guestApi } from '../../api/guest.js';
import Toast from '@vant/weapp/toast/toast';

/*

Page({
  data: {
    nickname: '',
    phone: '',
    code: '',
    password: '',
    codeText: '发送验证码',
    codeDisabled: false
  },

  onFieldChange(e) {
    const { name } = e.currentTarget.dataset;
    this.setData({ [name]: e.detail });
  },

  // 发送验证码
  sendVerifyCode() {
    const { phone } = this.data;
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      return Toast.fail('手机号格式错误');
    }

    guestApi.sendCode(phone).then(() => {
      Toast.success('验证码已发送');
      this.startCountdown();
    });
  },

  startCountdown() {
    let seconds = 60;
    this.setData({ codeDisabled: true });
    const timer = setInterval(() => {
      seconds--;
      this.setData({ codeText: `${seconds}s后重发` });
      if (seconds <= 0) {
        clearInterval(timer);
        this.setData({ codeText: '发送验证码', codeDisabled: false });
      }
    }, 1000);
  },

  // 提交注册
  handleRegister() {
    const { nickname, phone, code, password } = this.data;
    if (!nickname || !phone || !code || !password) {
      return Toast.fail('请完善注册信息');
    }

    Toast.loading({ message: '注册中...', forbidClick: true });
    const guestData = {
      name: nickname,
      phone: phone,
      password: password 
    };

    guestApi.register(guestData)
      .then(res => {
        if (res) { 
          Toast.success('注册成功');
          wx.setStorageSync('userPhone', phone);
          setTimeout(() => wx.navigateBack(), 1500);
        }
      });
  },

  backToLogin() {
    wx.navigateBack();
  }
});
*/

Page({
  data: {
    nickname: '',
    phone: '',
    code: '',
    codeText: 'Send Code',
    codeDisabled: false
  },

  onFieldChange(e) {
    const { name } = e.currentTarget.dataset;
    this.setData({ [name]: e.detail });
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

  handleRegister() {
    const { nickname, phone, code } = this.data;
    if (!nickname || !phone || !code) {
      Toast.fail('Please complete register info');
      return;
    }

    Toast.loading({ message: 'Registering...', forbidClick: true });

    guestApi.register({
      guestName: nickname,
      phone,
      code,
      openId: wx.getStorageSync('pendingOpenId') || ''
    })
      .then(res => {
        if (!res) return;

        Toast.success('Register success');
        setTimeout(() => {
          wx.navigateBack();
        }, 800);
      })
      .catch(err => {
        console.error('register error', err);
      });
  },

  backToLogin() {
    wx.navigateBack();
  }
});
