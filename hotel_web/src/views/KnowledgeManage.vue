<template>
  <div class="knowledge-manage">
    <div class="panel-card">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">知识库管理</h2>
          <p class="panel-subtitle">上传酒店知识文档，同步到 Python RAG 服务，并管理文档状态。</p>
        </div>
        <el-button type="primary" @click="openUploadDialog">上传知识文档</el-button>
      </div>

      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="标题">
          <el-input v-model="filters.title" placeholder="按标题搜索" clearable />
        </el-form-item>
        <el-form-item label="文档类型">
          <el-select v-model="filters.docType" placeholder="全部类型" clearable>
            <el-option label="政策" value="policy" />
            <el-option label="FAQ" value="faq" />
            <el-option label="规则" value="rule" />
            <el-option label="通知" value="notice" />
          </el-select>
        </el-form-item>
        <el-form-item label="同步状态">
          <el-select v-model="filters.syncStatus" placeholder="全部状态" clearable>
            <el-option label="待同步" value="PENDING" />
            <el-option label="同步成功" value="SUCCESS" />
            <el-option label="同步失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-select v-model="filters.enabled" placeholder="全部" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" plain @click="loadDocuments">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="documents" border stripe v-loading="loading">
        <el-table-column prop="documentId" label="ID" width="84" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="docType" label="类型" width="100" />
        <el-table-column prop="sourceName" label="来源标识" min-width="180" show-overflow-tooltip />
        <el-table-column prop="collectionName" label="Collection" min-width="140" show-overflow-tooltip />
        <el-table-column prop="chunkCount" label="Chunk 数" width="92" />
        <el-table-column label="同步状态" width="110">
          <template #default="{ row }">
            <el-tag :type="syncStatusTagType(row.syncStatus)">
              {{ formatSyncStatus(row.syncStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'">
              {{ row.enabled === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastSyncTime" label="最近同步" min-width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.lastSyncTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedTime" label="更新时间" min-width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.updatedTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetailDialog(row)">详情</el-button>
            <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" type="success" @click="handleSync(row)">重同步</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="uploadDialogVisible" title="上传知识文档" width="620px">
      <el-form :model="uploadForm" label-width="110px">
        <el-form-item label="文档标题">
          <el-input v-model="uploadForm.title" placeholder="例如：酒店入住须知" />
        </el-form-item>
        <el-form-item label="酒店 ID">
          <el-input-number v-model="uploadForm.hotelId" :min="1" />
        </el-form-item>
        <el-form-item label="Agent ID">
          <el-input-number v-model="uploadForm.agentId" :min="1" :step="1" />
        </el-form-item>
        <el-form-item label="文档类型">
          <el-select v-model="uploadForm.docType">
            <el-option label="政策" value="policy" />
            <el-option label="FAQ" value="faq" />
            <el-option label="规则" value="rule" />
            <el-option label="通知" value="notice" />
          </el-select>
        </el-form-item>
        <el-form-item label="Collection">
          <el-input v-model="uploadForm.collectionName" placeholder="默认 hotel_knowledge" />
        </el-form-item>
        <el-form-item label="覆盖同源文档">
          <el-switch v-model="uploadForm.replaceBySource" />
        </el-form-item>
        <el-form-item label="选择文件">
          <el-upload
            class="doc-uploader"
            drag
            :auto-upload="false"
            :limit="1"
            :show-file-list="true"
            accept=".txt,.md,.pdf,.docx"
            :on-change="handleUploadFileChange"
            :on-remove="handleUploadFileRemove"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将文档拖到此处，或 <em>点击选择文件</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">支持 txt / md / pdf / docx，上传后会自动同步到 RAG。</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitUpload">开始上传</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialogVisible" title="编辑文档元数据" width="560px">
      <el-form :model="editForm" label-width="110px">
        <el-form-item label="文档标题">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="酒店 ID">
          <el-input-number v-model="editForm.hotelId" :min="1" />
        </el-form-item>
        <el-form-item label="Agent ID">
          <el-input-number v-model="editForm.agentId" :min="1" />
        </el-form-item>
        <el-form-item label="文档类型">
          <el-select v-model="editForm.docType">
            <el-option label="政策" value="policy" />
            <el-option label="FAQ" value="faq" />
            <el-option label="规则" value="rule" />
            <el-option label="通知" value="notice" />
          </el-select>
        </el-form-item>
        <el-form-item label="Collection">
          <el-input v-model="editForm.collectionName" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="editForm.enabledFlag" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="文档详情" width="700px">
      <div v-if="currentDocument" class="detail-grid">
        <div class="detail-item"><span class="label">标题</span><span>{{ currentDocument.title || '-' }}</span></div>
        <div class="detail-item"><span class="label">文档类型</span><span>{{ currentDocument.docType || '-' }}</span></div>
        <div class="detail-item"><span class="label">酒店 ID</span><span>{{ currentDocument.hotelId || '-' }}</span></div>
        <div class="detail-item"><span class="label">Agent ID</span><span>{{ currentDocument.agentId || '-' }}</span></div>
        <div class="detail-item"><span class="label">Collection</span><span>{{ currentDocument.collectionName || '-' }}</span></div>
        <div class="detail-item"><span class="label">Chunk 数</span><span>{{ currentDocument.chunkCount ?? '-' }}</span></div>
        <div class="detail-item"><span class="label">同步状态</span><span>{{ formatSyncStatus(currentDocument.syncStatus) }}</span></div>
        <div class="detail-item detail-item--full"><span class="label">同步消息</span><span>{{ currentDocument.syncMessage || '-' }}</span></div>
        <div class="detail-item detail-item--full"><span class="label">来源标识</span><span>{{ currentDocument.sourceName || '-' }}</span></div>
        <div class="detail-item detail-item--full"><span class="label">文件 URL</span><a v-if="currentDocument.fileUrl" :href="currentDocument.fileUrl" target="_blank" rel="noreferrer">{{ currentDocument.fileUrl }}</a><span v-else>-</span></div>
        <div class="detail-item detail-item--full"><span class="label">文件路径</span><span>{{ currentDocument.filePath || '-' }}</span></div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { knowledgeApi } from '@/api/knowledge'

const storedHotelId = localStorage.getItem('hotelId')

const loading = ref(false)
const submitting = ref(false)
const documents = ref([])
const currentDocument = ref(null)
const uploadFile = ref(null)

const uploadDialogVisible = ref(false)
const editDialogVisible = ref(false)
const detailDialogVisible = ref(false)

const filters = ref({
  hotelId: storedHotelId ? Number(storedHotelId) : undefined,
  title: '',
  docType: '',
  syncStatus: '',
  enabled: undefined
})

const uploadForm = ref(createUploadForm())
const editForm = ref(createEditForm())

onMounted(() => {
  loadDocuments()
})

function createUploadForm() {
  return {
    title: '',
    hotelId: storedHotelId ? Number(storedHotelId) : null,
    agentId: null,
    docType: 'policy',
    collectionName: 'hotel_knowledge',
    replaceBySource: true
  }
}

function createEditForm() {
  return {
    documentId: null,
    title: '',
    hotelId: storedHotelId ? Number(storedHotelId) : null,
    agentId: null,
    docType: 'policy',
    collectionName: 'hotel_knowledge',
    enabledFlag: true
  }
}

function syncStatusTagType(status) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'warning'
}

function formatSyncStatus(status) {
  if (status === 'SUCCESS') return '同步成功'
  if (status === 'FAILED') return '同步失败'
  if (status === 'PENDING') return '待同步'
  return status || '-'
}

function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

async function loadDocuments() {
  loading.value = true
  try {
    const result = await knowledgeApi.list(filters.value)
    documents.value = result.data || []
  } catch (error) {
    ElMessage.error(error?.message || '获取知识文档列表失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.value = {
    hotelId: storedHotelId ? Number(storedHotelId) : undefined,
    title: '',
    docType: '',
    syncStatus: '',
    enabled: undefined
  }
  loadDocuments()
}

function openUploadDialog() {
  uploadForm.value = createUploadForm()
  uploadFile.value = null
  uploadDialogVisible.value = true
}

function handleUploadFileChange(file) {
  uploadFile.value = file.raw
}

function handleUploadFileRemove() {
  uploadFile.value = null
}

async function submitUpload() {
  if (!uploadFile.value) {
    ElMessage.warning('请先选择一个文档文件')
    return
  }

  submitting.value = true
  try {
    await knowledgeApi.uploadDocument(uploadFile.value, uploadForm.value)
    ElMessage.success('知识文档上传并同步成功')
    uploadDialogVisible.value = false
    await loadDocuments()
  } catch (error) {
    ElMessage.error(error?.message || '知识文档上传失败')
  } finally {
    submitting.value = false
  }
}

function openEditDialog(row) {
  editForm.value = {
    documentId: row.documentId,
    title: row.title || '',
    hotelId: row.hotelId || null,
    agentId: row.agentId || null,
    docType: row.docType || 'policy',
    collectionName: row.collectionName || 'hotel_knowledge',
    enabledFlag: row.enabled === 1
  }
  editDialogVisible.value = true
}

async function submitEdit() {
  submitting.value = true
  try {
    await knowledgeApi.updateDocument(editForm.value.documentId, {
      title: editForm.value.title,
      hotelId: editForm.value.hotelId,
      agentId: editForm.value.agentId,
      docType: editForm.value.docType,
      collectionName: editForm.value.collectionName,
      enabled: editForm.value.enabledFlag ? 1 : 0
    })
    ElMessage.success('文档元数据更新成功')
    editDialogVisible.value = false
    await loadDocuments()
  } catch (error) {
    ElMessage.error(error?.message || '文档更新失败')
  } finally {
    submitting.value = false
  }
}

async function openDetailDialog(row) {
  try {
    const result = await knowledgeApi.getById(row.documentId)
    currentDocument.value = result.data || row
    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error?.message || '读取文档详情失败')
  }
}

async function handleSync(row) {
  try {
    await ElMessageBox.confirm(`确定重新同步文档「${row.title}」吗？`, '提示', {
      confirmButtonText: '重新同步',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await knowledgeApi.syncDocument(row.documentId, {
      collectionName: row.collectionName || 'hotel_knowledge',
      replaceBySource: true
    })
    ElMessage.success('文档已重新同步')
    await loadDocuments()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '文档同步失败')
    }
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除文档「${row.title}」吗？删除后会同时清理知识库来源。`, '提示', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await knowledgeApi.deleteDocument(row.documentId)
    ElMessage.success('文档删除成功')
    await loadDocuments()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '文档删除失败')
    }
  }
}
</script>

<style scoped>
.knowledge-manage {
  padding: 28px;
  background: #f6f8fb;
  min-height: 100%;
}

.panel-card {
  background: #fff;
  border-radius: 18px;
  padding: 24px 24px 28px;
  box-shadow: 0 12px 36px rgba(15, 23, 42, 0.06);
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 20px;
}

.panel-title {
  margin: 0;
  font-size: 24px;
  color: #243b53;
}

.panel-subtitle {
  margin: 8px 0 0;
  color: #6b7c93;
  font-size: 14px;
}

.filter-form {
  margin-bottom: 18px;
}

.doc-uploader {
  width: 100%;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px 20px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 12px;
  background: #f8fafc;
}

.detail-item--full {
  grid-column: 1 / -1;
}

.label {
  font-size: 12px;
  color: #7b8794;
}

@media (max-width: 960px) {
  .knowledge-manage {
    padding: 18px;
  }

  .panel-header {
    flex-direction: column;
    align-items: stretch;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
