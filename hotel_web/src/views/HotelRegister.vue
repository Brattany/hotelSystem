<template>
  <div class="register-container">
    <div class="header">
      <span class="page-title">酒店注册</span>
      <h2 class="logo-text">Aventale</h2>
    </div>

    <el-form :model="form" label-position="top" class="register-form">
      <el-row :gutter="40">
        <el-col :span="12">
          <h3>基本信息：</h3>
          <el-form-item label="酒店名称 (HotelName)">
            <el-input v-model="form.hotelName" placeholder="不能为空" />
          </el-form-item>
          <el-form-item label="省 / 市 / 区">
            <el-cascader
              v-model="area"
              :options="options"
              :props="cascaderProps"
              placeholder="请选择省/市/区"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="详细地址 (Address)">
            <el-input v-model="form.address" placeholder="address..." />
          </el-form-item>
          <el-form-item label="联系电话 (Phone)">
            <el-input v-model="form.phone" placeholder="phone..." />
          </el-form-item>
          <div class="footer-actions">
            <el-button type="primary" size="large" @click="submitRegister" class="reg-btn">注册</el-button>
            <p class="hint">点击注册</p>
          </div>
        </el-col>

        <el-col :span="12">
          <h3>其他信息：</h3>
          <el-form-item label="最低定价 (PriceMin)">
            <el-input-number v-model="form.priceMin" :min="0" style="width: 100%" />
          </el-form-item>
          <el-form-item label="最高定价 (PriceMax)">
            <el-input-number v-model="form.priceMax" :min="0" style="width: 100%" />
          </el-form-item>
          
          <el-form-item label="设施服务">
            <el-checkbox v-model="form.wifi" label="WIFI" />
            <el-checkbox v-model="form.breakfast" label="breakfast" />
            <el-checkbox v-model="form.parking" label="parking" />
          </el-form-item>

          <el-form-item label="订单免费取消时段">
            下单后 <el-input-number v-model="form.cancelDays" :min="0" size="small" /> 天
          </el-form-item>

          <el-form-item label="取消政策 (CancelPolicy)">
            <el-input v-model="form.cancelPolicy" type="textarea" placeholder="Cancel Policy..." />
          </el-form-item>
          
          <el-form-item label="描述 (Description)">
            <el-input v-model="form.description" type="textarea" placeholder="description..." />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
  </div>
</template>

<script setup>
import { ref,reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { regionData } from 'element-china-area-data'
import axios from 'axios'

const router = useRouter()
const area = ref([])
const options = regionData
const cascaderProps = {
  value: 'label',   // 👈 关键！！！
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
  description: ''
})

const submitRegister = async () => {
  if (!form.hotelName) {
    ElMessage.error('酒店名称不能为空！')
    return
  }

  const postData = {
  hotelName: form.hotelName,
  province: area.value[0], // 确保这里已经处理成英文
  city: area.value[1],
  district: area.value[2],
  address: form.address,
  phone: form.phone,
  priceMin: form.priceMin,
  priceMax: form.priceMax,
  hasWifi: form.wifi ? 1 : 0,        // 对应后端 Integer
  hasBreakfast: form.breakfast ? 1 : 0, 
  hasParking: form.parking ? 1 : 0,
  freeCancel: form.cancelDays,       // 对应后端 Integer
  cancelPolicy: form.cancelPolicy,
  description: form.description,
  status: 1,
  isDelete: 0
 }

 console.log('提交数据：', postData)

  try {
    const res = await axios.post('http://localhost:8080/hotel/register', postData)
    if (res.data.code === 200) {
      ElMessage.success('注册成功！')
      router.push('/') // 成功后跳转回 P1
    }
  } catch (error) {
    ElMessage.error('注册失败，请稍后再试')
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
  margin-top: 100px;
  text-align: center;
}
.reg-btn {
  width: 200px;
  border: 2px solid #a855f7; /* 紫色边框 */
  background: transparent;
  color: #a855f7;
}
.reg-btn:hover {
  background: #a855f7;
  color: white;
}
.hint {
  font-size: 12px;
  color: #999;
}
</style>