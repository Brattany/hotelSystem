import { reservationApi } from '../../api/reservation.js';
import Toast from '@vant/weapp/toast/toast';
import Dialog from '@vant/weapp/dialog/dialog';

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
      Toast.fail('缺少订单信息');
      return;
    }

    Dialog.confirm({
      title: '确认支付',
      message: `确认支付：¥${this.data.amount}`
    }).then(() => {
      this.executePayment();
    }).catch(() => {});
  },

  executePayment() {
    Toast.loading({ message: '支付中...', forbidClick: true, duration: 0 });

    reservationApi.updateStatus(this.data.orderId, 2)
      .then(() => {
        Toast.success('支付成功');
        setTimeout(() => {
          wx.switchTab({ url: '/pages/order/list' });
        }, 600);
      })
      .catch((err) => {
        console.error('支付失败', err);
        Toast.fail('支付失败');
      })
      .finally(() => {
        Toast.clear();
      });
  }
});