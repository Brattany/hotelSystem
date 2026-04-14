const BASE_URL = 'http://localhost:8080';

const resolveImageUrl = (path) => {
  if (!path || typeof path !== 'string') {
    return '';
  }

  if (/^https?:\/\//i.test(path)) {
    return path;
  }

  if (path.startsWith('//')) {
    return `http:${path}`;
  }

  return path.startsWith('/') ? `${BASE_URL}${path}` : `${BASE_URL}/${path}`;
};

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

const getResponseMessage = (result, fallbackMessage) => {
  if (isPlainObject(result)) {
    return result.message || result.msg || result.error || fallbackMessage;
  }

  if (typeof result === 'string' && result.trim()) {
    return result;
  }

  return fallbackMessage;
};

const createRequestError = ({ message, statusCode, result, url, method, unauthorized = false }) => {
  const error = new Error(message || 'Request failed');
  error.statusCode = statusCode || 0;
  error.code = isPlainObject(result) && typeof result.code !== 'undefined' ? result.code : statusCode || 0;
  error.response = result;
  error.url = url;
  error.method = method;
  error.unauthorized = unauthorized;
  return error;
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
        const method = options.method || 'GET';
        const requestUrl = `${BASE_URL}${url}`;

        if (res.statusCode === 401) {
          const error = createRequestError({
            message: getResponseMessage(result, 'Unauthorized, please login again'),
            statusCode: res.statusCode,
            result,
            url: requestUrl,
            method,
            unauthorized: true
          });
          handleUnauthorized();
          reject(error);
          return;
        }

        if (res.statusCode < 200 || res.statusCode >= 300) {
          const message = getResponseMessage(result, `HTTP ${res.statusCode}`);
          const error = createRequestError({
            message,
            statusCode: res.statusCode,
            result,
            url: requestUrl,
            method
          });
          console.error('[request] HTTP error', {
            url: requestUrl,
            method,
            statusCode: res.statusCode,
            requestData: options.data,
            response: result
          });
          showToast(message);
          reject(error);
          return;
        }

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

        {
          const unauthorized = Boolean(normalized.unauthorized);
          const message = getResponseMessage(normalized.raw || result, normalized.message || 'Request failed');
          const error = createRequestError({
            message,
            statusCode: res.statusCode,
            result: normalized.raw || result,
            url: requestUrl,
            method,
            unauthorized
          });
          console.error('[request] business error', {
            url: requestUrl,
            method,
            statusCode: res.statusCode,
            requestData: options.data,
            response: result
          });

          if (unauthorized) {
            handleUnauthorized();
          } else {
            showToast(message);
          }

          reject(error);
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

export { BASE_URL, resolveImageUrl };
