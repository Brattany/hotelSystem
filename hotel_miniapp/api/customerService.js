const BASE_URL = 'http://localhost:8080';

const CHAT_ENDPOINTS = [
  '/assistant/chat',
  '/ai/chat',
  '/chat/send',
  '/chat',
  '/agent/chat',
  '/customer-service/chat'
];

const buildMockReply = (message) => {
  const content = (message || '').trim();

  if (!content) {
    return '您好，我可以帮您查询酒店、房型、预订流程、支付和订单问题。';
  }

  if (content.includes('预订') || content.includes('下单')) {
    return '您可以先搜索酒店，进入详情页后选择入住日期和房型，再填写入住人信息提交订单。';
  }

  if (content.includes('支付')) {
    return '提交订单后会进入支付页，确认金额无误后即可完成支付。';
  }

  if (content.includes('取消') || content.includes('退款')) {
    return '如果需要处理订单取消或退款，建议您先到“我的订单”查看当前订单状态，再联系客服进一步确认。';
  }

  if (content.includes('入住') || content.includes('退房')) {
    return '入住和退房时间以酒店和房型页面展示为准，您也可以告诉我具体酒店名称，我来帮您梳理预订步骤。';
  }

  return `已收到您的问题：“${content}”。当前客服前端已接通，后端接口稳定后会返回更准确的智能答复。您也可以继续咨询酒店搜索、房型预订、支付或订单问题。`;
};

const normalizeReply = (result) => {
  if (!result) {
    return '';
  }

  if (typeof result === 'string') {
    return result;
  }

  if (typeof result.reply === 'string') return result.reply;
  if (typeof result.answer === 'string') return result.answer;
  if (typeof result.content === 'string') return result.content;
  if (typeof result.message === 'string') return result.message;
  if (typeof result.text === 'string') return result.text;

  if (result.data) {
    return normalizeReply(result.data);
  }

  return '';
};

const requestChat = (endpoint, payload) => {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token');
    const header = {
      'Content-Type': 'application/json'
    };

    if (token) {
      header.Authorization = token;
    }

    wx.request({
      url: `${BASE_URL}${endpoint}`,
      method: 'POST',
      data: payload,
      header,
      success: (res) => {
        if (res.statusCode !== 200) {
          reject(new Error(`HTTP_${res.statusCode}`));
          return;
        }

        const raw = res.data;
        const result = raw && typeof raw === 'object' && 'data' in raw ? raw.data : raw;
        const reply = normalizeReply(result) || normalizeReply(raw);

        if (!reply) {
          reject(new Error('EMPTY_REPLY'));
          return;
        }

        resolve({
          reply,
          sessionId: (result && (result.sessionId || result.conversationId)) || payload.sessionId || '',
          isMock: false
        });
      },
      fail: reject
    });
  });
};

const tryChatEndpoints = async (payload) => {
  let lastError = null;

  for (let i = 0; i < CHAT_ENDPOINTS.length; i += 1) {
    try {
      return await requestChat(CHAT_ENDPOINTS[i], payload);
    } catch (error) {
      lastError = error;
    }
  }

  throw lastError || new Error('CHAT_UNAVAILABLE');
};

export const customerServiceApi = {
  async sendMessage({ message, sessionId = '', history = [] }) {
    const payload = {
      message,
      content: message,
      question: message,
      sessionId,
      history
    };

    try {
      return await tryChatEndpoints(payload);
    } catch (error) {
      return {
        reply: buildMockReply(message),
        sessionId: sessionId || `mock-${Date.now()}`,
        isMock: true,
        errorMessage: '智能客服服务暂未完全接通，当前为演示回复。'
      };
    }
  }
};