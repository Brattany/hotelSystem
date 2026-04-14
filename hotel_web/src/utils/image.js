export const resolveImageUrl = (path) => {
  if (!path) {
    return ''
  }

  if (/^https?:\/\//i.test(path)) {
    return path
  }

  if (path.startsWith('//')) {
    return `http:${path}`
  }

  return path.startsWith('/') ? path : `/${path}`
}

export default resolveImageUrl
