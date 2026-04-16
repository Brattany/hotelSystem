import request from '@/utils/request'

export const knowledgeApi = {
  list(params = {}) {
    return request.get('/agent/knowledge-documents', { params })
  },

  getById(documentId) {
    return request.get(`/agent/knowledge-documents/${documentId}`)
  },

  uploadDocument(file, payload = {}) {
    const formData = new FormData()
    formData.append('file', file)

    Object.entries(payload).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        formData.append(key, value)
      }
    })

    return request.post('/agent/knowledge-documents/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: 120000
    }).then((res) => res.data || {})
  },

  updateDocument(documentId, payload) {
    return request.put(`/agent/knowledge-documents/${documentId}`, payload).then((res) => res.data || {})
  },

  syncDocument(documentId, payload = {}) {
    return request.post(`/agent/knowledge-documents/${documentId}/sync`, payload, {
      timeout: 120000
    }).then((res) => res.data || {})
  },

  deleteDocument(documentId) {
    return request.delete(`/agent/knowledge-documents/${documentId}`).then((res) => res.data)
  }
}

export default knowledgeApi
