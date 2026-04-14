import request from '@/utils/request'

export const uploadApi = {
  uploadImage(file, type) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post(`/upload/image?type=${encodeURIComponent(type)}`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    }).then((res) => res.data || {})
  }
}

export default uploadApi
