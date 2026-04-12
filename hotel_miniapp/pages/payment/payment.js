import { reservationApi } from '../../api/reservation.js';
import Toast from '@vant/weapp/toast/toast';
import Dialog from '@vant/weapp/dialog/dialog';

/*

Page({
  data: {
    orderId: '',
    amount: 0,
    payMethod: 'wechat'
  },

  onLoad(options) {
    this.setData({
      orderId: options.orderId || '',
      amount: options.amount || 0
    });
  },

  onMethodChange(e) {
    this.setData({ payMethod: e.detail });
  },

  onClickMethod(e) {
    this.setData({ payMethod: e.currentTarget.dataset.name });
  },

  handlePay() {
    Dialog.confirm({
      title: '确认支付',
      message: `确认使用${this.data.payMethod === 'wechat' ? '微信' : '余额'}支付 ¥${this.data.amount} 吗？`,
    }).then(() => {
      this.executePayment();
    }).catch(() => {});
  },

  // 模拟支付过程
  executePayment() {
    Toast.loading({ message: '支付中...', forbidClick: true, duration: 0 });

    setTimeout(() => {
      reservationApi.updateStatus(this.data.orderId, 2)
        .then(res => {
          Toast.clear();
          if (res) {
            Toast.success('支付成功');
            setTimeout(() => {
              wx.reLaunch({ url: '/pages/index/index' }); 
            }, 1500);
          }
        })
        .catch(err => {
          Toast.clear();
          Toast.fail('状态更新失败');
        });
    }, 2000);
  }
});
*/

Page({
  data: {
    orderId: '',
    amount: 0,
    payMethod: 'wechat'
  },

  onLoad(options) {
    this.setData({
      orderId: options.orderId || '',
      amount: Number(options.amount || 0)
    });
  },

  onMethodChange(e) {
    this.setData({ payMethod: e.detail });
  },

  onClickMethod(e) {
    this.setData({ payMethod: e.currentTarget.dataset.name });
  },

  handlePay() {
    if (!this.data.orderId) {
      Toast.fail('Missing order id');
      return;
    }

    Dialog.confirm({
      title: 'Confirm Payment',
      message: `Pay now: ¥${this.data.amount}`
    }).then(() => {
      this.executePayment();
    }).catch(() => {});
  },

  executePayment() {
    Toast.loading({ message: 'Paying...', forbidClick: true, duration: 0 });

    reservationApi.updateStatus(this.data.orderId, 2)
      .then(() => {
        Toast.success('Payment success');
        setTimeout(() => {
          wx.switchTab({ url: '/pages/order/list' });
        }, 600);
      })
      .catch(err => {
        console.error('pay error', err);
        Toast.fail('Payment failed');
      })
      .finally(() => {
        Toast.clear();
      });
  }
});
