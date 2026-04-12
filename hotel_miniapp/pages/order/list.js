import { reservationApi } from '../../api/reservation.js';
import Dialog from '@vant/weapp/dialog/dialog';
import Toast from '@vant/weapp/toast/toast';

/*

Page({
  data: {
    activeTab: 'all',
    allOrders: [], // 保存所有的订单原始数据
    filteredOrders: [], // 保存当前 Tab 下过滤后的订单数据
    
    // 状态字典映射，方便 WXML 渲染文本
    statusMap: {
      '-1': '已取消',
      '0': '待支付',
      '1': '待入住',
      '2': '已入住',
      '3': '已完成'
    }
  },

  onShow() {
    // 每次页面展示时，刷新订单列表
    this.fetchMyOrders();
  },

  // 获取当前用户的订单列表
  fetchMyOrders() {
    const phone = wx.getStorageSync('userPhone');
    if (!phone) {
      // 如果没有登录，引导去登录
      wx.navigateTo({ url: '/pages/login/login' });
      return;
    }

    reservationApi.getReservationsByPhone(phone)
      .then(res => {
        const orders = res || [];
        
        orders.forEach(order => {
          if(order.checkInDate) order.checkInDate = order.checkInDate.split(' ')[0];
          if(order.checkOutDate) order.checkOutDate = order.checkOutDate.split(' ')[0];
        });

        this.setData({ allOrders: orders }, () => {
          this.filterOrders();
        });
      })
      .catch(err => {
        console.error('获取订单失败', err);
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
    } else {
      const targetStatus = Number(activeTab);
      const filtered = allOrders.filter(order => order.status === targetStatus);
      this.setData({ filteredOrders: filtered });
    }
  },

  // 取消订单
  cancelOrder(e) {
    const orderId = e.currentTarget.dataset.id;
    Dialog.confirm({
      title: '提示',
      message: '确定要取消该订单吗？',
    }).then(() => {
      reservationApi.updateStatus(orderId, 3).then(res => {
        if (res) {
          Toast.success('订单已取消');
          this.fetchMyOrders(); 
        }
      });
    }).catch(() => {
    });
  },

  goToPay(e) {
    const { id, price } = e.currentTarget.dataset;
    wx.navigateTo({
      url: `/pages/payment/payment?orderId=${id}&amount=${price}`
    });
  }
});
*/

Page({
  data: {
    activeTab: 'all',
    allOrders: [],
    filteredOrders: [],
    statusMap: {
      1: 'Pending Payment',
      2: 'Pending Check-In',
      3: 'Canceled',
      4: 'Checked In',
      5: 'Completed'
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
      title: 'Confirm',
      message: 'Cancel this order?'
    }).then(() => {
      reservationApi.updateStatus(orderId, 3).then(() => {
        Toast.success('Canceled');
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
