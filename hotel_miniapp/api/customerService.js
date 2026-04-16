import { guestApi } from './guest.js';

const AGENT_BASE_URL = 'http://localhost:3366';
const CHAT_ENDPOINT = '/chat';
const GUEST_ID_CACHE_KEY = 'guestId';

const unwrap = (result) => (result && typeof result === 'object' && 'data' in result ? result.data : result);

const normalizeChatResult = (result) => {
  const normalized = unwrap(result) || {};

  return {
    intent: normalized.intent || 'general',
    structuredData: normalized.structured_data || normalized.structuredData || {},
    reply: normalized.reply || normalized.answer || normalized.message || '',
    success: normalized.success !== false,
    error: normalized.error || null,
    usedTools: normalized.used_tools || normalized.usedTools || []
  };
};

const resolveGuestId = async () => {
  const cachedGuestId = wx.getStorageSync(GUEST_ID_CACHE_KEY);
  if (cachedGuestId) {
    return cachedGuestId;
  }

  const phone = wx.getStorageSync('userPhone');
  if (!phone) {
    return null;
  }

  const profile = await guestApi.getGuestByPhone(phone);
  const source = unwrap(profile) || {};
  const guestId = source.guestId || source.id || null;
  if (guestId) {
    wx.setStorageSync(GUEST_ID_CACHE_KEY, guestId);
  }
  return guestId;
};

const requestChat = (payload) => {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token');
    const header = {
      'Content-Type': 'application/json'
    };

    if (token) {
      header.Authorization = token;
    }

    wx.request({
      url: `${AGENT_BASE_URL}${CHAT_ENDPOINT}`,
      method: 'POST',
      data: payload,
      header,
      success: (res) => {
        if (res.statusCode !== 200) {
          reject(new Error(`HTTP_${res.statusCode}`));
          return;
        }

        const normalized = normalizeChatResult(res.data);
        if (!normalized.reply) {
          reject(new Error('EMPTY_REPLY'));
          return;
        }

        resolve(normalized);
      },
      fail: reject
    });
  });
};

export const customerServiceApi = {
  async sendMessage({ message, sessionId = '', history = [], orderCandidates = [] }) {
    const guestId = await resolveGuestId();
    const payload = {
      message,
      session_id: sessionId,
      guest_id: guestId,
      history,
      order_candidates: Array.isArray(orderCandidates) ? orderCandidates : []
    };

    return requestChat(payload);
  }
};
