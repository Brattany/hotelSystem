<template>
  <div class="room-manage">
    <div class="section">
      <div class="header">
        <div class="left">
          <el-select v-model="filterStatus" placeholder="筛选房间状态" clearable @change="loadRoomsByStatus">
            <el-option label="全部房间" :value="0" />
            <el-option label="空闲" :value="1" />
            <el-option label="已入住" :value="2" />
            <el-option label="维修中" :value="3" />
          </el-select>
        </div>
        <div class="right">
          <el-button type="primary" @click="openAddRoom">新增房间</el-button>
        </div>
      </div>

      <el-table :data="rooms" border v-loading="loading">
        <el-table-column prop="roomNumber" label="房间号" />
        <el-table-column label="房型">
          <template #default="{ row }">
            {{ getTypeName(row.typeId) }}
          </template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ formatStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="openEditRoom(row)">修改</el-button>
            <el-button type="danger" size="small" @click="handleDeleteRoom(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="section section-gap">
      <div class="header">
        <span class="title">房型管理</span>
        <el-button type="primary" @click="openAddType">新增房型</el-button>
      </div>

      <el-table :data="roomTypes" border stripe>
        <el-table-column label="图片" width="120">
          <template #default="{ row }">
            <img v-if="resolveImageUrl(row.picture)" :src="resolveImageUrl(row.picture)" class="table-image" alt="房型图片" />
            <div v-else class="table-image placeholder">暂无图片</div>
          </template>
        </el-table-column>
        <el-table-column prop="typeName" label="房型名称" />
        <el-table-column prop="capacity" label="总数量" />
        <el-table-column prop="price" label="价格" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="openEditType(row)">修改</el-button>
            <el-button type="danger" size="small" @click="deleteType(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="roomDialogVisible" :title="isEditRoom ? '修改房间' : '新增房间'">
      <el-form :model="roomForm" label-width="80px">
        <el-form-item label="房间号">
          <el-input v-model="roomForm.roomNumber" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="roomForm.status">
            <el-option label="空闲" :value="1" />
            <el-option label="已入住" :value="2" />
            <el-option label="维修中" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="房型">
          <el-select v-model="roomForm.typeId">
            <el-option
              v-for="item in roomTypes"
              :key="item.typeId"
              :label="item.typeName"
              :value="item.typeId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roomDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRoom">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="typeDialogVisible" :title="isEditType ? '修改房型' : '新增房型'" width="640px">
      <el-form :model="typeForm" label-width="90px">
        <el-form-item label="房型名称">
          <el-input v-model="typeForm.typeName" />
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="typeForm.capacity" :min="1" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="typeForm.price" :min="0" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="typeForm.description" type="textarea" :rows="3" placeholder="请输入房型描述" />
        </el-form-item>
        <el-form-item label="房型图片">
          <el-upload
            class="image-uploader"
            :show-file-list="false"
            accept="image/*"
            :http-request="uploadRoomTypePicture"
            :before-upload="beforeImageUpload"
          >
            <img v-if="typePicturePreview" :src="typePicturePreview" class="type-preview" alt="房型图片" />
            <div v-else class="type-preview placeholder">点击上传房型图片</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitType">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { roomApi } from '@/api/room'
import { roomTypeApi } from '@/api/roomType'
import { uploadApi } from '@/api/upload'
import { resolveImageUrl } from '@/utils/image'
import { ElMessage, ElMessageBox } from 'element-plus'

const rooms = ref([])
const roomTypes = ref([])
const loading = ref(false)
const filterStatus = ref(null)
const hotelId = localStorage.getItem('hotelId')

const roomDialogVisible = ref(false)
const typeDialogVisible = ref(false)
const isEditRoom = ref(false)
const isEditType = ref(false)

const roomForm = ref({})
const typeForm = ref(createEmptyTypeForm())
const typePicturePreview = computed(() => resolveImageUrl(typeForm.value.picture))

onMounted(async () => {
  await loadRoomTypes()
  await loadRooms()
})

function createEmptyTypeForm() {
  return {
    hotelId: hotelId ? Number(hotelId) : null,
    typeName: '',
    capacity: 1,
    price: 0,
    description: '',
    picture: ''
  }
}

const beforeImageUpload = (file) => {
  const isImage = file.type && file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }

  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }

  return true
}

const uploadRoomTypePicture = async ({ file, onSuccess, onError }) => {
  try {
    const result = await uploadApi.uploadImage(file, 'roomType')
    typeForm.value.picture = result.path || result.url || ''
    ElMessage.success('房型图片上传成功')
    onSuccess(result)
  } catch (error) {
    ElMessage.error(error?.message || '房型图片上传失败')
    onError(error)
  }
}

const loadRooms = async () => {
  loading.value = true
  try {
    const res = await roomApi.getByHotel(hotelId)
    rooms.value = res.data || []
  } finally {
    loading.value = false
  }
}

const loadRoomsByStatus = async (val) => {
  if (!val) {
    await loadRooms()
    return
  }

  const res = await roomApi.getByStatus({ hotelId, status: val })
  rooms.value = res.data || []
}

const handleDeleteRoom = (row) => {
  ElMessageBox.confirm(`删除房间 ${row.roomNumber} ?`, '提示').then(async () => {
    await roomApi.deleteRoom(row.roomId)
    ElMessage.success('删除成功')
    loadRooms()
  })
}

const openAddRoom = () => {
  isEditRoom.value = false
  roomForm.value = { status: 1 }
  roomDialogVisible.value = true
}

const openEditRoom = (row) => {
  isEditRoom.value = true
  roomForm.value = { ...row }
  roomDialogVisible.value = true
}

const submitRoom = async () => {
  if (isEditRoom.value) {
    await roomApi.update(roomForm.value)
    ElMessage.success('房间修改成功')
  } else {
    await roomApi.create({ ...roomForm.value, hotelId })
    ElMessage.success('房间创建成功')
  }
  roomDialogVisible.value = false
  loadRooms()
}

const loadRoomTypes = async () => {
  const res = await roomTypeApi.getAll(hotelId)
  roomTypes.value = (res.data || []).filter((item) => item && item.typeId != null)
}

const deleteType = (row) => {
  ElMessageBox.confirm(`删除房型 ${row.typeName} ?`, '提示').then(async () => {
    await roomTypeApi.deleteType(row.typeId)
    ElMessage.success('删除成功')
    await loadRoomTypes()
    loadRooms()
  })
}

const openAddType = () => {
  isEditType.value = false
  typeForm.value = createEmptyTypeForm()
  typeDialogVisible.value = true
}

const openEditType = (row) => {
  isEditType.value = true
  typeForm.value = {
    ...createEmptyTypeForm(),
    ...row
  }
  typeDialogVisible.value = true
}

const submitType = async () => {
  const payload = {
    ...typeForm.value,
    hotelId: typeForm.value.hotelId || Number(hotelId)
  }

  if (isEditType.value) {
    await roomTypeApi.update(payload)
    ElMessage.success('房型修改成功')
  } else {
    await roomTypeApi.create(payload)
    ElMessage.success('房型创建成功')
  }

  typeDialogVisible.value = false
  await loadRoomTypes()
  loadRooms()
}

const getTypeName = (typeId) => {
  const target = roomTypes.value.find((item) => item.typeId === typeId)
  return target ? target.typeName : '未知房型'
}

const formatStatus = (status) => ({ 1: '空闲', 2: '已入住', 3: '维修中' }[status] || '未知')
const getStatusType = (status) => (status === 1 ? 'success' : status === 2 ? 'danger' : 'info')
</script>

<style scoped>
.room-manage {
  padding: 10px;
}

.section {
  border: 1px solid #ddd;
  padding: 15px;
  background: #fff;
  border-radius: 6px;
}

.section-gap {
  margin-top: 30px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.title {
  font-weight: bold;
  font-size: 16px;
}

.table-image,
.type-preview {
  width: 88px;
  height: 88px;
  border-radius: 10px;
  object-fit: cover;
  border: 1px solid #ebeef5;
  background: #f5f7fa;
}

.type-preview {
  width: 220px;
  height: 160px;
}

.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 12px;
}

.image-uploader {
  width: 220px;
}
</style>
