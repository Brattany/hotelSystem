export const formatDate = (input) => {
  const date = input instanceof Date ? input : new Date(input);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
};

export const addDays = (input, days) => {
  const date = input instanceof Date ? new Date(input.getTime()) : new Date(input);
  date.setDate(date.getDate() + days);
  return date;
};

export const diffDays = (checkInDate, checkOutDate) => {
  const start = new Date(checkInDate);
  const end = new Date(checkOutDate);
  return Math.max(1, Math.round((end.getTime() - start.getTime()) / (24 * 60 * 60 * 1000)));
};

export const validateStayDates = (checkInDate, checkOutDate) => {
  if (!checkInDate) {
    return '入住日期不能为空';
  }

  if (!checkOutDate) {
    return '离店日期不能为空';
  }

  const start = new Date(checkInDate).getTime();
  const end = new Date(checkOutDate).getTime();

  if (Number.isNaN(start) || Number.isNaN(end)) {
    return '日期格式不正确';
  }

  if (start >= end) {
    return '离店日期必须晚于入住日期';
  }

  return '';
};
