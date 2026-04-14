export const DEFAULT_PROFILE_AVATAR = '../../assets/logo.jpg';

export const getAvatarCacheKey = (phone) => `profileAvatar:${phone || 'guest'}`;

export const unwrapProfileResponse = (res) => (res && res.data ? res.data : res);

export const normalizeProfileData = (rawProfile, phone, defaultAvatar = DEFAULT_PROFILE_AVATAR) => {
  const source = rawProfile || {};
  const nextPhone = source.phone || source.guestPhone || phone || '';
  const cachedAvatar = wx.getStorageSync(getAvatarCacheKey(nextPhone));
  const nextName = source.name || source.guestName || source.nickName || '游客';

  return {
    ...source,
    guestId: source.guestId || source.id || null,
    phone: nextPhone,
    guestPhone: nextPhone,
    name: nextName,
    guestName: nextName,
    idCard: source.idCard || '',
    openId: source.openId || '',
    avatarUrl: source.avatarUrl || source.avatar || source.headImg || source.headImage || source.headimgurl || cachedAvatar || defaultAvatar
  };
};
