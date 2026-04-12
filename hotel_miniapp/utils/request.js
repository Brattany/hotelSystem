/*
const BASE_URL = 'http://localhost:8080'; 

const request = (url, options = {}) => {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token'); 
    
    const header = {
      'Content-Type': 'application/json',
      ...options.header
    };

    if (token) {
      header['Authorization'] = token; 
    }

    wx.request({
      url: BASE_URL + url,
      method: options.method || 'GET',
      data: options.data,
      header: header,
      success: (res) => {
        const result = res.data;

        if (res.statusCode !== 200) {
          if (res.statusCode === 401) {
            handleUnauthorized();
            return reject(new Error('未授权，请登录'));
          }
          wx.showToast({ title: '服务器开小差了~', icon: 'none' });
          return reject(result);
        }

        if (result.code === 200) {
          resolve(result.data); 
        } else if (result.code === 401) {
          handleUnauthorized();
          reject(new Error(result.message || '未授权，请登录'));
        } else {
          wx.showToast({ title: result.message || '请求失败', icon: 'none' });
          reject(result);
        }
      },
      fail: (err) => {
        wx.showToast({ title: '网络异常，请检查网络设置', icon: 'none' });
        reject(err);
      }
    });
  });
};

function handleUnauthorized() {
  wx.showToast({ title: '登录已过期，请重新登录', icon: 'none' });
  wx.removeStorageSync('token');
  wx.navigateTo({ url: '/pages/login/login' });
}

export default {
  get: (url, data, config) => request(url, { method: 'GET', data, ...config }),
  post: (url, data, config) => request(url, { method: 'POST', data, ...config }),
  put: (url, data, config) => request(url, { method: 'PUT', data, ...config }),
  delete: (url, data, config) => request(url, { method: 'DELETE', data, ...config })
};
*/

const BASE_URL = 'http://localhost:8080';

const request = (url, options = {}) => {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token');
    const header = {
      'Content-Type': 'application/json',
      ...options.header
    };

    if (token) {
      header.Authorization = token;
    }

    wx.request({
      url: BASE_URL + url,
      method: options.method || 'GET',
      data: options.data,
      header,
      success: res => {
        const result = res.data || {};

        if (res.statusCode !== 200) {
          if (res.statusCode === 401) {
            handleUnauthorized();
            reject(new Error('未授权，请重新登录'));
            return;
          }

          wx.showToast({ title: '服务器开小差了', icon: 'none' });
          reject(result);
          return;
        }

        if (result.code === 200) {
          resolve(result.data);
          return;
        }

        if (result.code === 401) {
          handleUnauthorized();
          reject(new Error(result.message || '未授权，请重新登录'));
          return;
        }

        wx.showToast({ title: result.message || '请求失败', icon: 'none' });
        reject(result);
      },
      fail: err => {
        wx.showToast({ title: '网络异常，请检查网络设置', icon: 'none' });
        reject(err);
      }
    });
  });
};

function handleUnauthorized() {
  wx.showToast({ title: '登录已过期，请重新登录', icon: 'none' });
  wx.removeStorageSync('token');
  wx.removeStorageSync('userPhone');
  wx.reLaunch({ url: '/pages/login/login' });
}

export default {
  get: (url, data, config) => request(url, { method: 'GET', data, ...config }),
  post: (url, data, config) => request(url, { method: 'POST', data, ...config }),
  put: (url, data, config) => request(url, { method: 'PUT', data, ...config }),
  delete: (url, data, config) => request(url, { method: 'DELETE', data, ...config })
};
