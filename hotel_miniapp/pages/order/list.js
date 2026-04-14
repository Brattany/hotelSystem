import { reservationApi } from '../../api/reservation.js';
import Dialog from '@vant/weapp/dialog/dialog';
import Toast from '@vant/weapp/toast/toast';

Page({
  data: {
    activeTab: 'all',
    allOrders: [],
    filteredOrders: [],
    statusMap: {
      1: '待支付',
      2: '待入住',
      3: '已取消',
      4: '已入住',
      5: '已完成'
    }
  },

  onShow() {
    this.fetchMyOrders();
  },

  fetchMyOrders() {
    const phone = wx.getStorageSync('userPhone');
    if (!phone) {
      this.setData({ allOrders: [], filteredOrders: [] });
      wx.navigateTo({ url: '/pages/login/login' });
      return;
    }

    reservationApi.getReservationsByPhone(phone)
      .then(res => {
        const orders = (Array.isArray(res) ? res : []).map(order => ({
          ...order,
          checkInDate: order.checkInDate || '',
          checkOutDate: order.checkOutDate || ''
        }));

        this.setData({ allOrders: orders }, () => {
          this.filterOrders();
        });
      })
      .catch(err => {
        console.error('load orders error', err);
        this.setData({ allOrders: [], filteredOrders: [] });
      });
  },

  onTabChange(event) {
    this.setData({ activeTab: event.detail.name }, () => {
      this.filterOrders();
    });
  },

  filterOrders() {
    const { activeTab, allOrders } = this.data;
    if (activeTab === 'all') {
      this.setData({ filteredOrders: allOrders });
      return;
    }

    const targetStatus = Number(activeTab);
    this.setData({
      filteredOrders: allOrders.filter(order => order.status === targetStatus)
    });
  },

  cancelOrder(e) {
    const orderId = e.currentTarget.dataset.id;
    if (!orderId) return;

    Dialog.confirm({
      title: '提示',
      message: '确认取消该订单吗？'
    }).then(() => {
      reservationApi.updateStatus(orderId, 3).then(() => {
        Toast.success('订单已取消');
        this.fetchMyOrders();
      });
    }).catch(() => {});
  },

  goToPay(e) {
    const { id, price } = e.currentTarget.dataset;
    if (!id) return;

    wx.navigateTo({
      url: `/pages/payment/payment?orderId=${id}&amount=${price || 0}`
    });
  }
});
