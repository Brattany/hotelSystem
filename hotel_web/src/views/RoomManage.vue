<template>
  <div class="room-manage">

    <!-- 房间管理 -->
    <div class="section">
      <div class="header">
        <div class="left">
          <el-select v-model="filterStatus" placeholder="筛选房间状态" clearable @change="loadRoomsByStatus">
            <el-option label="所有房间" :value="null" />
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

        <!-- 新增：房型名称 -->
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

    <!-- 房型管理 -->
    <div class="section" style="margin-top: 30px;">
      <div class="header">
        <span class="title">房型管理</span>
        <el-button type="primary" @click="openAddType">新增房型</el-button>
      </div>

      <el-table :data="roomTypes" border stripe>
        <el-table-column prop="typeName" label="房型名称" />
        <el-table-column prop="capacity" label="容纳人数" />
        <el-table-column prop="price" label="价格" />

        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="openEditType(row)">修改</el-button>
            <el-button type="danger" size="small" @click="deleteType(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 房间弹窗 -->
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

    <!-- 房型弹窗 -->
    <el-dialog v-model="typeDialogVisible" :title="isEditType ? '修改房型' : '新增房型'">
      <el-form :model="typeForm" label-width="80px">

        <el-form-item label="名称">
          <el-input v-model="typeForm.typeName" />
        </el-form-item>

        <el-form-item label="容量">
          <el-input-number v-model="typeForm.capacity" :min="1" />
        </el-form-item>

        <el-form-item label="价格">
          <el-input-number v-model="typeForm.price" :min="0" />
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
import { ref, onMounted } from 'vue'
import { roomApi } from '@/api/room'
import { roomTypeApi } from '@/api/roomType'
import { ElMessage, ElMessageBox } from 'element-plus'

const rooms = ref([])
const roomTypes = ref([])
const loading = ref(false)
const filterStatus = ref(null)
const hotelId = localStorage.getItem('hotelId')

/* 弹窗控制 */
const roomDialogVisible = ref(false)
const typeDialogVisible = ref(false)
const isEditRoom = ref(false)
const isEditType = ref(false)

/* 表单 */
const roomForm = ref({})
const typeForm = ref({})

onMounted(async () => {
  await loadRoomTypes() // 先加载房型
  await loadRooms()
})

/* ---------- 房间 ---------- */

const loadRooms = async () => {
  loading.value = true
  try {
    const res = await roomApi.getByHotel(hotelId)
    console.log('hotelId:', hotelId)
    console.log('Loaded rooms:', res.data) // 调试输出
    rooms.value = res.data || []
  } finally {
    loading.value = false
  }
}

const loadRoomsByStatus = async (val) => {
  if (!val) return loadRooms()
  const res = await roomApi.getByTS({ hotelId, status: val })
  rooms.value = res.data || []
}

const handleDeleteRoom = (row) => {
  ElMessageBox.confirm(`删除房间 ${row.roomNumber}?`).then(async () => {
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
    ElMessage.success('修改成功')
  } else {
    await roomApi.create({ ...roomForm.value, hotelId })
    ElMessage.success('新增成功')
  }
  roomDialogVisible.value = false
  loadRooms()
}

/* ---------- 房型 ---------- */

const loadRoomTypes = async () => {
  const res = await roomTypeApi.getAll(hotelId)
  roomTypes.value = (res.data || []).filter(i => i && i.typeId != null)
}

const deleteType = (row) => {
  ElMessageBox.confirm(`删除房型 ${row.typeName}?`).then(async () => {
    await roomTypeApi.deleteType(row.typeId)
    ElMessage.success('删除成功')
    await loadRoomTypes()
    loadRooms()
  })
}

const openAddType = () => {
  isEditType.value = false
  typeForm.value = {}
  typeDialogVisible.value = true
}

const openEditType = (row) => {
  isEditType.value = true
  typeForm.value = { ...row }
  typeDialogVisible.value = true
}

const submitType = async () => {
  if (isEditType.value) {
    console.log('Updating type with data:', typeForm.value) // 调试输出
    await roomTypeApi.update(typeForm.value)
    ElMessage.success('修改成功')
  } else {
    await roomTypeApi.create({ ...typeForm.value, hotelId })
    ElMessage.success('新增成功')
  }
  typeDialogVisible.value = false
  await loadRoomTypes()
  loadRooms()
}

/* ---------- 工具方法 ---------- */

//typeId → 房型名称
const getTypeName = (typeId) => {
  const t = roomTypes.value.find(i => i.typeId === typeId)
  return t ? t.typeName : '未知房型'
}

/* ---------- 状态 ---------- */

const formatStatus = (s) => ({1:'空闲',2:'已入住',3:'维修中'}[s] || '未知')
const getStatusType = (s) => s===1?'success':s===2?'danger':'info'
</script>

<style scoped>
.room-manage { padding: 10px; }

.section {
  border: 1px solid #ddd;
  padding: 15px;
  background: #fff;
  border-radius: 6px;
}

.header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.title {
  font-weight: bold;
  font-size: 16px;
}
</style>