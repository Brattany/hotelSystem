<template>
  <div class="hotel-info-container">
    <div class="info-card">
      <div class="card-header">
        <h2 class="title">关于 {{ hotelForm.hotelName || '当前酒店' }}</h2>
        <el-button :type="isEditing ? 'success' : 'primary'" @click="toggleEdit">
          {{ isEditing ? '保存修改' : '编辑信息' }}
        </el-button>
      </div>

      <el-divider />

      <el-form :model="hotelForm" label-width="100px" :disabled="!isEditing">
        <el-form-item label="酒店图片">
          <el-upload
            class="image-uploader"
            :show-file-list="false"
            :disabled="!isEditing"
            accept="image/*"
            :http-request="uploadHotelPicture"
            :before-upload="beforeImageUpload"
          >
            <img v-if="hotelPicturePreview" :src="hotelPicturePreview" class="hotel-preview" alt="酒店图片" />
            <div v-else class="image-placeholder">
              <span>{{ isEditing ? '点击上传酒店图片' : '暂无酒店图片' }}</span>
            </div>
          </el-upload>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="酒店名称">
              <el-input v-model="hotelForm.hotelName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="hotelForm.phone" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="省份">
              <el-input v-model="hotelForm.province" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="城市">
              <el-input v-model="hotelForm.city" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="区县">
              <el-input v-model="hotelForm.district" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="详细地址">
          <el-input v-model="hotelForm.address" type="textarea" />
        </el-form-item>

        <el-form-item label="酒店描述">
          <el-input v-model="hotelForm.description" type="textarea" :rows="4" placeholder="向用户介绍您的酒店" />
        </el-form-item>

        <el-form-item label="酒店标签">
          <div class="tag-group">
            <el-tag
              v-for="tag in tags"
              :key="tag.tagId || tag.id"
              closable
              :disable-transitions="false"
              @close="handleCloseTag(tag)"
              class="hotel-tag"
            >
              {{ tag.tag || tag.tagName || tag.name }}
            </el-tag>
            <el-input
              v-if="inputVisible"
              ref="saveTagInput"
              v-model="newTagName"
              class="new-tag-input"
              size="small"
              @keyup.enter="handleTagInputConfirm"
              @blur="handleTagInputConfirm"
            />
            <el-button v-else class="button-new-tag" size="small" @click="showTagInput">
              + 新增标签
            </el-button>
          </div>
        </el-form-item>
      </el-form>

      <div class="notice-box" v-if="isEditing">
        请确认酒店信息和图片无误后再保存，保存后前台和小程序都会读取最新图片路径。
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { hotelApi } from '@/api/hotel'
import { uploadApi } from '@/api/upload'
import { resolveImageUrl } from '@/utils/image'
import { ElMessage } from 'element-plus'

const hotelId = localStorage.getItem('hotelId')
const isEditing = ref(false)
const hotelForm = ref({
  hotelId: hotelId ? Number(hotelId) : null,
  hotelName: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  address: '',
  description: '',
  picture: ''
})
const tags = ref([])
const inputVisible = ref(false)
const newTagName = ref('')
const saveTagInput = ref(null)
const hotelPicturePreview = computed(() => resolveImageUrl(hotelForm.value.picture))

onMounted(() => {
  fetchHotelData()
})

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

const uploadHotelPicture = async ({ file, onSuccess, onError }) => {
  try {
    const result = await uploadApi.uploadImage(file, 'hotel')
    hotelForm.value.picture = result.path || result.url || ''
    ElMessage.success('酒店图片上传成功')
    onSuccess(result)
  } catch (error) {
    ElMessage.error(error?.message || '酒店图片上传失败')
    onError(error)
  }
}

const fetchHotelData = async () => {
  try {
    const res = await hotelApi.getById(hotelId)
    hotelForm.value = {
      ...hotelForm.value,
      ...(res.data || {})
    }

    const tagRes = await hotelApi.getTags(hotelId)
    tags.value = tagRes.data || []
  } catch (err) {
    ElMessage.error(err?.message || '获取酒店信息失败')
  }
}

const toggleEdit = async () => {
  if (!isEditing.value) {
    isEditing.value = true
    return
  }

  try {
    await hotelApi.update(hotelForm.value)
    ElMessage.success('酒店信息已更新')
    isEditing.value = false
    fetchHotelData()
  } catch (err) {
    ElMessage.error(err?.message || '更新失败，请重试')
  }
}

const showTagInput = () => {
  inputVisible.value = true
  nextTick(() => {
    saveTagInput.value?.focus?.()
  })
}

const handleTagInputConfirm = async () => {
  const tagName = newTagName.value.trim()
  if (tagName) {
    const res = await hotelApi.addTag({ hotelId, tag: tagName })
    if (res.data) {
      tags.value.push(res.data)
      ElMessage.success('标签添加成功')
    }
  }
  inputVisible.value = false
  newTagName.value = ''
}

const handleCloseTag = async (tag) => {
  const tagId = tag.tagId || tag.id
  if (!tagId) {
    ElMessage.error('标签缺少标识，无法删除')
    return
  }

  try {
    await hotelApi.deleteTag(tagId)
    tags.value = tags.value.filter((item) => (item.tagId || item.id) !== tagId)
    ElMessage.success('标签已删除')
  } catch (err) {
    ElMessage.error(err?.message || '删除标签失败')
  }
}
</script>

<style scoped>
.hotel-info-container {
  padding: 30px;
}

.info-card {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px;
  border: 2px solid #4a7c44;
  background: #fff;
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  color: #4a7c44;
  margin: 0;
}

.image-uploader {
  width: 100%;
}

.hotel-preview,
.image-placeholder {
  width: 100%;
  max-width: 360px;
  height: 220px;
  border-radius: 12px;
  border: 1px dashed #dcdfe6;
  overflow: hidden;
}

.hotel-preview {
  display: block;
  object-fit: cover;
}

.image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  background: #fafafa;
}

.tag-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hotel-tag {
  background-color: #f0f9eb;
  border-color: #e1f3d8;
  color: #67c23a;
}

.notice-box {
  margin-top: 30px;
  padding: 10px;
  background-color: #fff4f4;
  border: 1px solid #ffdbdb;
  color: #f56c6c;
  font-size: 14px;
}

.new-tag-input {
  width: 120px;
}
</style>
