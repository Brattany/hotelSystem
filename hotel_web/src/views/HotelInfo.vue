<template>
  <div class="hotel-info-container">
    <div class="info-card">
      <div class="card-header">
        <h2 class="title">关于 {{ hotelForm.hotelName }}</h2>
        <el-button 
          :type="isEditing ? 'success' : 'primary'" 
          @click="toggleEdit"
        >
          {{ isEditing ? '保存修改' : '编辑信息' }}
        </el-button>
      </div>

      <el-divider />

      <el-form :model="hotelForm" label-width="100px" :disabled="!isEditing">
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

        <el-form-item label="所在城市">
          <el-input v-model="hotelForm.city" style="width: 200px" />
        </el-form-item>

        <el-form-item label="详细地址">
          <el-input v-model="hotelForm.address" type="textarea" />
        </el-form-item>

        <el-form-item label="酒店描述">
          <el-input 
            v-model="hotelForm.description" 
            type="textarea" 
            :rows="4" 
            placeholder="向顾客介绍您的酒店..."
          />
        </el-form-item>

        <el-form-item label="酒店特色">
          <div class="tag-group">
            <el-tag 
              v-for="tag in tags" 
              :key="tag.tagId" 
              closable 
              @close="handleCloseTag(tag)"
              class="hotel-tag"
            >
              {{ tag.tag }}
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
        <p>!注意：请确保相关操作已然经过酒店管理层商讨一致。</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { hotelApi } from '@/api/hotel'
import { ElMessage } from 'element-plus'

const hotelId = localStorage.getItem('hotelId')
const isEditing = ref(false)
const hotelForm = ref({
  hotelName: '',
  phone: '',
  city: '',
  address: '',
  description: ''
})
const tags = ref([])

// 标签相关逻辑
const inputVisible = ref(false)
const newTagName = ref('')
const saveTagInput = ref(null)

onMounted(() => {
  fetchHotelData()
})

const fetchHotelData = async () => {
  try {
    const res = await hotelApi.getById(hotelId)
    hotelForm.value = res.data
    
    const tagRes = await hotelApi.getTags(hotelId)
    tags.value = tagRes.data
  } catch (err) {
    ElMessage.error('获取酒店信息失败')
  }
}

const toggleEdit = async () => {
  if (isEditing.value) {
    // 保存逻辑
    try {
      await hotelApi.update(hotelForm.value)
      ElMessage.success('酒店信息已同步更新')
      isEditing.value = false
    } catch (err) {
      ElMessage.error('更新失败，请重试')
    }
  } else {
    isEditing.value = true
  }
}

// 标签操作
const showTagInput = () => {
  inputVisible.value = true
  nextTick(() => { saveTagInput.value.focus() })
}

const handleTagInputConfirm = async () => {
  if (newTagName.value) {
    const newTag = { hotelId: hotelId, tag: newTagName.value }
    const res = await hotelApi.addTag(newTag)
    tags.value.push(res.data)
    ElMessage.success('标签添加成功')
  }
  inputVisible.value = false
  newTagName.value = ''
}

//标签删除逻辑
const handleCloseTag = async (tag) => {
  const idToDelete = tag.id;

  if (!idToDelete) {
    ElMessage.error('无法删除：标签 ID 不存在');
    console.error('当前标签数据：', tag);
    return;
  }

  try {
    await hotelApi.deleteTag(idToDelete); 

    const index = tags.value.indexOf(tag);
    if (index !== -1) {
      tags.value.splice(index, 1);
    }
    
    ElMessage.success('标签已删除');
  } catch (err) {
    ElMessage.error('删除标签失败，请重试');
    console.error('删除标签错误：', err);
  }
}
</script>

<style scoped>
.hotel-info-container { padding: 30px; }
.info-card {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px;
  border: 2px solid #4a7c44; /* 主题绿 */
  background: #fff;
  border-radius: 8px;
}
.card-header { display: flex; justify-content: space-between; align-items: center; }
.title { color: #4a7c44; margin: 0; }
.tag-group { display: flex; flex-wrap: wrap; gap: 10px; }
.hotel-tag { background-color: #f0f9eb; border-color: #e1f3d8; color: #67c23a; }
.notice-box {
  margin-top: 30px;
  padding: 10px;
  background-color: #fff4f4;
  border: 1px solid #ffdbdb;
  color: #f56c6c;
  font-size: 14px;
}
.new-tag-input { width: 90px; }
</style>