import { guestApi } from '../../api/guest.js';
import Toast from '@vant/weapp/toast/toast';

const CODE_TEXT = '获取验证码';

Page({
  data: {
    nickname: '',
    phone: '',
    code: '',
    codeText: CODE_TEXT,
    codeDisabled: false
  },

  onFieldChange(e) {
    const { name } = e.currentTarget.dataset;
    this.setData({ [name]: e.detail });
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

  handleRegister() {
    const { nickname, phone, code } = this.data;
    if (!nickname || !phone || !code) {
      Toast.fail('请填写完整注册信息');
      return;
    }

    Toast.loading({ message: '注册中...', forbidClick: true });

    guestApi.register({
      guestName: nickname,
      phone,
      code,
      openId: wx.getStorageSync('pendingOpenId') || ''
    })
      .then(res => {
        if (!res) return;

        Toast.success('注册成功');
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
