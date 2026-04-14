import { customerServiceApi } from '../../api/customerService.js';

const createMessage = (role, content, extra = {}) => ({
  id: `${role}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
  role,
  content,
  ...extra
});

Page({
  data: {
    sessionId: '',
    inputValue: '',
    sending: false,
    errorText: '',
    statusText: '',
    scrollIntoView: '',
    messages: [
      createMessage('assistant', '您好，我是智能客服小助手。您可以咨询酒店搜索、房型预订、支付、订单等问题。')
    ]
  },

  onReady() {
    this.scrollToBottom();
  },

  onInputChange(e) {
    this.setData({
      inputValue: e.detail.value || '',
      errorText: ''
    });
  },

  onQuickAsk(e) {
    const question = e.currentTarget.dataset.question || '';
    this.setData({ inputValue: question }, () => {
      this.handleSend();
    });
  },

  async handleSend() {
    const message = this.data.inputValue.trim();
    if (!message || this.data.sending) {
      if (!message) {
        wx.showToast({ title: '请输入咨询内容', icon: 'none' });
      }
      return;
    }

    const userMessage = createMessage('user', message);
    const history = this.data.messages.map((item) => ({
      role: item.role,
      content: item.content
    }));

    this.setData({
      inputValue: '',
      sending: true,
      errorText: '',
      messages: [...this.data.messages, userMessage]
    });
    this.scrollToBottom();

    try {
      const result = await customerServiceApi.sendMessage({
        message,
        sessionId: this.data.sessionId,
        history
      });

      const assistantMessage = createMessage('assistant', result.reply, {
        isMock: !!result.isMock
      });

      this.setData({
        sessionId: result.sessionId || this.data.sessionId,
        statusText: result.isMock ? (result.errorMessage || '当前为演示回复') : '',
        messages: [...this.data.messages, assistantMessage]
      });
    } catch (error) {
      const fallbackMessage = createMessage('assistant', '暂时无法连接智能客服，请稍后重试。');
      this.setData({
        errorText: '发送失败，请检查网络后重试。',
        messages: [...this.data.messages, fallbackMessage]
      });
    } finally {
      this.setData({ sending: false });
      this.scrollToBottom();
    }
  },

  scrollToBottom() {
    const messages = this.data.messages || [];
    if (!messages.length) {
      return;
    }

    const lastMessage = messages[messages.length - 1];
    wx.nextTick(() => {
      this.setData({
        scrollIntoView: `msg-${lastMessage.id}`
      });
    });
  }
});