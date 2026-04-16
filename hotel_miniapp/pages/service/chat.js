const { customerServiceApi } = require('../../api/customerService.js');

const INTENT_LABEL_MAP = {
  search_hotels: '酒店查询',
  query_orders: '订单查询',
  get_recent_orders: '最近订单',
  get_order_detail: '订单详情',
  update_order: '订单修改',
  cancel_order: '订单取消',
  knowledge_query: '知识问答',
  general: '通用咨询'
};

function createMessage(role, content, extra) {
  return {
    id: role + '-' + Date.now() + '-' + Math.random().toString(36).slice(2, 8),
    role: role,
    content: content,
    ...(extra || {})
  };
}

function formatIntentLabel(intent) {
  return INTENT_LABEL_MAP[intent] || intent || '';
}

function formatHotelMatchLabel(structuredData) {
  const nextStructuredData = structuredData || {};
  const level = nextStructuredData.match_level || nextStructuredData.matchLevel || '';
  const query = nextStructuredData.query || {};

  if (level === 'exact') {
    return '精确匹配结果';
  }
  if (level === 'fuzzy') {
    return '相关结果';
  }
  if (level === 'city_fallback') {
    return (query.city || '当前城市') + '范围结果';
  }
  return '';
}

function formatKnowledgeRouteLabel(routeType) {
  if (routeType === 'hybrid') {
    return '混合结果';
  }
  if (routeType === 'rag') {
    return '知识库结果';
  }
  if (routeType === 'structured') {
    return '结构化结果';
  }
  return '';
}

function buildFacilitySummary(facilities) {
  const nextFacilities = facilities || {};
  const required = Array.isArray(nextFacilities.required) ? nextFacilities.required : [];
  if (!required.length) {
    return '';
  }

  const labels = {
    wifi: 'WiFi',
    breakfast: '早餐',
    parking: '停车场'
  };
  const facilityText = required.map((item) => labels[item] || item).join('、');
  const mode = nextFacilities.matchMode || 'all';
  if (mode === 'any') {
    return facilityText + ' 任意一个满足即可';
  }
  if (mode === 'at_least') {
    return facilityText + ' 至少满足 ' + (nextFacilities.minMatchCount || 1) + ' 个';
  }
  return facilityText + ' 全部满足';
}

function normalizeOrder(order) {
  const nextOrder = order || {};
  const orderId = nextOrder.orderId || nextOrder.reservationId || nextOrder.id || null;
  const province = nextOrder.province || '';
  const city = nextOrder.city || '';
  const district = nextOrder.district || '';
  const address = nextOrder.address || nextOrder.hotelAddress || '';
  const regionLabel = [city, district].filter(Boolean).join(' ');
  const hotelFullAddress = nextOrder.hotelFullAddress || [province, city, district, address].filter(Boolean).join('');

  return {
    ...nextOrder,
    orderId,
    reservationId: nextOrder.reservationId || orderId,
    orderNo: nextOrder.orderNo || (orderId ? String(orderId) : ''),
    roomTypeName: nextOrder.roomTypeName || nextOrder.typeName || '',
    hotelAddress: hotelFullAddress || address,
    hotelFullAddress,
    regionLabel,
    statusText: nextOrder.statusDescription || nextOrder.status || ''
  };
}

function normalizeKnowledgeHit(hit) {
  const nextHit = hit || {};
  const content = nextHit.content || nextHit.document || '';

  return {
    ...nextHit,
    source: nextHit.source || nextHit.file_name || nextHit.fileName || '知识文档',
    fileName: nextHit.file_name || nextHit.fileName || nextHit.source || '',
    docType: nextHit.doc_type || nextHit.docType || '',
    scoreText: typeof nextHit.score === 'number' ? (nextHit.score * 100).toFixed(1) + '%' : '',
    preview: content.length > 120 ? content.slice(0, 120) + '...' : content
  };
}

function normalizeKnowledgeSource(source) {
  const nextSource = source || {};
  return {
    ...nextSource,
    source: nextSource.source || nextSource.file_name || nextSource.fileName || '知识文档',
    fileName: nextSource.file_name || nextSource.fileName || nextSource.source || '',
    docType: nextSource.doc_type || nextSource.docType || ''
  };
}

function detectMessageType(structuredData, intent) {
  const nextStructuredData = structuredData || {};

  if (Array.isArray(nextStructuredData.hotels) && nextStructuredData.hotels.length) {
    return 'hotel_result';
  }
  if (Array.isArray(nextStructuredData.displayOrders) && nextStructuredData.displayOrders.length) {
    return 'order_list';
  }
  if (nextStructuredData.reservation) {
    return 'order_detail';
  }
  if (nextStructuredData.knowledge && (nextStructuredData.knowledge.total || (nextStructuredData.knowledge.hits || []).length)) {
    return 'knowledge_result';
  }
  if (intent === 'search_hotels') {
    return 'hotel_result';
  }
  return 'text';
}

function normalizeStructuredData(structuredData) {
  const nextStructuredData = structuredData || {};
  const query = nextStructuredData.query || {};
  const location = query.location || {};
  const facilities = query.facilities || {};
  const roomType = query.roomType || {};
  const hotels = Array.isArray(nextStructuredData.hotels)
    ? nextStructuredData.hotels.map((hotel) => ({
        ...hotel,
        matchedRoomTypesLabel: Array.isArray(hotel.matchedRoomTypes) ? hotel.matchedRoomTypes.join('、') : ''
      }))
    : [];
  const orders = Array.isArray(nextStructuredData.orders) ? nextStructuredData.orders.map(normalizeOrder) : [];
  const candidates = Array.isArray(nextStructuredData.candidates) ? nextStructuredData.candidates.map(normalizeOrder) : [];
  const reservation = nextStructuredData.reservation || nextStructuredData.order
    ? normalizeOrder(nextStructuredData.reservation || nextStructuredData.order)
    : null;
  const displayOrders = orders.length ? orders : candidates;
  const routeType = nextStructuredData.route_type || nextStructuredData.routeType || '';
  const rawKnowledge = nextStructuredData.knowledge || {};
  const knowledgeHits = Array.isArray(rawKnowledge.hits) ? rawKnowledge.hits.map(normalizeKnowledgeHit) : [];
  const knowledgeSources = Array.isArray(rawKnowledge.sources) ? rawKnowledge.sources.map(normalizeKnowledgeSource) : [];
  const knowledgeTotal = rawKnowledge.total || knowledgeHits.length || knowledgeSources.length || 0;
  const hasKnowledge = knowledgeTotal > 0;

  return {
    ...nextStructuredData,
    hotels,
    orders,
    candidates,
    reservation,
    displayOrders,
    routeType,
    knowledgeRouteLabel: formatKnowledgeRouteLabel(routeType),
    knowledge: {
      ...rawKnowledge,
      total: knowledgeTotal,
      hits: knowledgeHits,
      sources: knowledgeSources
    },
    hasKnowledge,
    total: nextStructuredData.total || displayOrders.length || hotels.length || knowledgeTotal || 0,
    query,
    queryDisplay: {
      location,
      hotelName: query.hotelName || '',
      facilitySummary: buildFacilitySummary(facilities),
      roomTypeLabel: roomType.roomTypeKeyword || (roomType.roomTypeId ? ('房型 ID：' + roomType.roomTypeId) : '')
    }
  };
}

Page({
  data: {
    inputValue: '',
    sending: false,
    errorText: '',
    scrollIntoView: '',
    pendingOrderCandidates: [],
    messages: [
      createMessage('assistant', '您好，我是智能客服助手。您可以咨询酒店搜索、订单查询、订单修改、订单取消，以及知识库问答相关问题。', {
        intent: 'general',
        intentLabel: formatIntentLabel('general'),
        hotelMatchLabel: '',
        structuredData: {},
        messageType: 'text'
      })
    ]
  },

  onReady: function () {
    this.scrollToBottom();
  },

  onInputChange: function (e) {
    this.setData({
      inputValue: e.detail.value || '',
      errorText: ''
    });
  },

  onQuickAsk: function (e) {
    const question = e.currentTarget.dataset.question || '';
    this.setData({ inputValue: question }, () => {
      this.handleSend();
    });
  },

  handleSend: function () {
    const message = (this.data.inputValue || '').trim();
    if (!message || this.data.sending) {
      if (!message) {
        wx.showToast({ title: '请输入咨询内容', icon: 'none' });
      }
      return;
    }

    const userMessage = createMessage('user', message);
    const history = (this.data.messages || []).map((item) => ({
      role: item.role,
      content: item.content
    }));
    const nextMessages = this.data.messages.concat(userMessage);

    this.setData({
      inputValue: '',
      sending: true,
      errorText: '',
      messages: nextMessages
    });
    this.scrollToBottom();

    customerServiceApi.sendMessage({
      message: message,
      history: history,
      orderCandidates: this.data.pendingOrderCandidates || []
    })
      .then((result) => {
        const structuredData = normalizeStructuredData(result.structuredData || {});
        const intent = result.intent || 'general';
        const assistantMessage = createMessage('assistant', result.reply || '已为您处理请求。', {
          intent: intent,
          intentLabel: formatIntentLabel(intent),
          hotelMatchLabel: formatHotelMatchLabel(structuredData),
          success: result.success !== false,
          structuredData: structuredData,
          messageType: detectMessageType(structuredData, intent)
        });

        let pendingOrderCandidates = this.data.pendingOrderCandidates || [];
        if (Array.isArray(structuredData.displayOrders) && structuredData.displayOrders.length) {
          pendingOrderCandidates = structuredData.displayOrders;
        }
        if ((intent === 'update_order' || intent === 'cancel_order') && structuredData.matched && !structuredData.multiple) {
          pendingOrderCandidates = [];
        }
        if (intent === 'search_hotels') {
          pendingOrderCandidates = [];
        }

        this.setData({
          errorText: result.success === false ? (result.error || '智能客服处理失败') : '',
          pendingOrderCandidates,
          messages: nextMessages.concat(assistantMessage)
        });
      })
      .catch((error) => {
        console.error('chat page send failed', error);
        this.setData({
          errorText: '暂时无法连接智能客服，请稍后重试。'
        });
      })
      .finally(() => {
        this.setData({ sending: false });
        this.scrollToBottom();
      });
  },

  scrollToBottom: function () {
    const messages = this.data.messages || [];
    if (!messages.length) {
      return;
    }

    const lastMessage = messages[messages.length - 1];
    wx.nextTick(() => {
      this.setData({
        scrollIntoView: 'msg-' + lastMessage.id
      });
    });
  }
});
