<template>
  <div class="register-container">
    <div class="header">
      <span class="page-title">酒店注册</span>
      <h2 class="logo-text">Aventale</h2>
    </div>

    <el-form :model="form" label-position="top" class="register-form">
      <el-row :gutter="40">
        <el-col :span="12">
          <h3>基本信息</h3>
          <el-form-item label="酒店名称">
            <el-input v-model="form.hotelName" placeholder="请输入酒店名称" />
          </el-form-item>
          <el-form-item label="省 / 市 / 区">
            <el-cascader
              v-model="area"
              :options="options"
              :props="cascaderProps"
              placeholder="请选择省市区"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="详细地址">
            <el-input v-model="form.address" placeholder="请输入详细地址" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="form.phone" placeholder="请输入联系电话" />
          </el-form-item>
          <el-form-item label="酒店图片">
            <el-upload
              class="image-uploader"
              :show-file-list="false"
              accept="image/*"
              :http-request="uploadHotelPicture"
              :before-upload="beforeImageUpload"
            >
              <img v-if="hotelPicturePreview" :src="hotelPicturePreview" class="upload-preview" alt="酒店图片" />
              <div v-else class="upload-placeholder">
                <span class="upload-title">上传酒店图片</span>
                <span class="upload-tip">支持 JPG / PNG，建议横图</span>
              </div>
            </el-upload>
          </el-form-item>
          <div class="footer-actions">
            <el-button type="primary" size="large" @click="submitRegister" class="reg-btn">提交注册</el-button>
          </div>
        </el-col>

        <el-col :span="12">
          <h3>其他信息</h3>
          <el-form-item label="最低价格">
            <el-input-number v-model="form.priceMin" :min="0" style="width: 100%" />
          </el-form-item>
          <el-form-item label="最高价格">
            <el-input-number v-model="form.priceMax" :min="0" style="width: 100%" />
          </el-form-item>
          <el-form-item label="设施服务">
            <el-checkbox v-model="form.wifi" label="WIFI" />
            <el-checkbox v-model="form.breakfast" label="早餐" />
            <el-checkbox v-model="form.parking" label="停车" />
          </el-form-item>
          <el-form-item label="免费取消时间">
            下单后 <el-input-number v-model="form.cancelDays" :min="0" size="small" /> 天内
          </el-form-item>
          <el-form-item label="取消政策">
            <el-input v-model="form.cancelPolicy" type="textarea" placeholder="请输入取消政策" />
          </el-form-item>
          <el-form-item label="酒店描述">
            <el-input v-model="form.description" type="textarea" placeholder="请输入酒店描述" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { regionData } from 'element-china-area-data'
import { hotelApi } from '@/api/hotel'
import { uploadApi } from '@/api/upload'
import { resolveImageUrl } from '@/utils/image'

const router = useRouter()
const area = ref([])
const options = regionData
const cascaderProps = {
  value: 'label',
  label: 'label',
  children: 'children'
}

const form = reactive({
  hotelName: '',
  province: '',
  city: '',
  district: '',
  address: '',
  phone: '',
  priceMin: 0,
  priceMax: 0,
  wifi: false,
  breakfast: false,
  parking: false,
  cancelDays: 7,
  cancelPolicy: '',
  description: '',
  picture: ''
})

const hotelPicturePreview = computed(() => resolveImageUrl(form.picture))

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
    form.picture = result.path || result.url || ''
    ElMessage.success('酒店图片上传成功')
    onSuccess(result)
  } catch (error) {
    ElMessage.error(error?.message || '酒店图片上传失败')
    onError(error)
  }
}

const submitRegister = async () => {
  if (!form.hotelName.trim()) {
    ElMessage.error('酒店名称不能为空')
    return
  }

  const [province = '', city = '', district = ''] = area.value || []
  const postData = {
    hotelName: form.hotelName,
    province,
    city,
    district,
    address: form.address,
    phone: form.phone,
    priceMin: form.priceMin,
    priceMax: form.priceMax,
    hasWifi: form.wifi ? 1 : 0,
    hasBreakfast: form.breakfast ? 1 : 0,
    hasParking: form.parking ? 1 : 0,
    freeCancel: form.cancelDays,
    cancelPolicy: form.cancelPolicy,
    description: form.description,
    picture: form.picture,
    status: 1,
    isDelete: 0
  }

  try {
    await hotelApi.register(postData)
    ElMessage.success('酒店注册成功')
    router.push('/')
  } catch (error) {
    ElMessage.error(error?.message || '酒店注册失败，请稍后重试')
  }
}
</script>

<style scoped>
.register-container {
  padding: 40px;
  max-width: 1000px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 2px solid #67c23a;
  margin-bottom: 20px;
}

.logo-text {
  color: #67c23a;
  font-style: italic;
}

.footer-actions {
  margin-top: 40px;
  text-align: center;
}

.reg-btn {
  width: 220px;
  border: 2px solid #67c23a;
  background: transparent;
  color: #67c23a;
}

.reg-btn:hover {
  background: #67c23a;
  color: #fff;
}

.hint {
  margin-top: 12px;
  font-size: 12px;
  color: #999;
  line-height: 1.6;
}

.image-uploader {
  width: 100%;
}

.upload-preview,
.upload-placeholder {
  width: 100%;
  height: 220px;
  border-radius: 12px;
  border: 1px dashed #cdd0d6;
  overflow: hidden;
}

.upload-preview {
  display: block;
  object-fit: cover;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: #fafafa;
  color: #909399;
}

.upload-title {
  font-size: 16px;
  color: #303133;
}

.upload-tip {
  font-size: 13px;
}
</style>
