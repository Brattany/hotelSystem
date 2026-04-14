const BASE_URL = 'http://localhost:8080';

const isPlainObject = (value) => Object.prototype.toString.call(value) === '[object Object]';

const normalizeResponse = (result) => {
  if (Array.isArray(result)) {
    return { ok: true, data: result };
  }

  if (!isPlainObject(result)) {
    return { ok: true, data: result };
  }

  if (typeof result.code === 'number') {
    if (result.code === 200) {
      return { ok: true, data: Object.prototype.hasOwnProperty.call(result, 'data') ? result.data : result };
    }

    if (result.code === 401) {
      return { ok: false, unauthorized: true, message: result.message || '未授权，请重新登录' };
    }

    return { ok: false, message: result.message || '请求失败', raw: result };
  }

  if (typeof result.success === 'boolean') {
    if (result.success) {
      return { ok: true, data: Object.prototype.hasOwnProperty.call(result, 'data') ? result.data : result };
    }

    return { ok: false, message: result.message || '请求失败', raw: result };
  }

  return { ok: true, data: result };
};

const showToast = (title) => {
  wx.showToast({ title, icon: 'none' });
};

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
      url: `${BASE_URL}${url}`,
      method: options.method || 'GET',
      data: options.data,
      header,
      success: (res) => {
        const result = res.data;

        if (res.statusCode === 401) {
          handleUnauthorized();
          reject(new Error('未授权，请重新登录'));
          return;
        }

        if (res.statusCode < 200 || res.statusCode >= 300) {
          showToast('服务开小差了，请稍后重试');
          reject(result || new Error(`HTTP_${res.statusCode}`));
          return;
        }

        const normalized = normalizeResponse(result);
        if (normalized.ok) {
          resolve(normalized.data);
          return;
        }

        if (normalized.unauthorized) {
          handleUnauthorized();
          reject(new Error(normalized.message));
          return;
        }

        showToast(normalized.message || '请求失败');
        reject(normalized.raw || new Error(normalized.message || '请求失败'));
      },
      fail: (err) => {
        showToast('网络异常，请检查网络后重试');
        reject(err);
      }
    });
  });
};

function handleUnauthorized() {
  showToast('登录已过期，请重新登录');
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