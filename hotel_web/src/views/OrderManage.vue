```vue
<template>
  <div class="order-container">

    <!-- 顶栏 -->
    <div class="search-bar">
      <el-input v-model="searchPhone" placeholder="手机号查询" style="width:300px" clearable />
      <el-button @click="handleSearch">查询</el-button>

      <!-- 退房 -->
      <el-input v-model="checkoutRoomNumber" placeholder="房间号退房" style="width:200px" />
      <el-button type="warning" @click="confirmCheckOut">退房办理</el-button>

      <el-button type="primary" style="margin-left:auto" @click="openAddDialog">
        + 新增订单
      </el-button>
    </div>

    <!-- 新增订单 -->
    <el-dialog v-model="showAddDialog" title="新增订单" width="500px">
      <el-form :model="newOrderInfo" label-width="100px">

        <el-form-item label="手机号">
          <el-input v-model="newOrderInfo.phone" @blur="autoFillGuest" />
        </el-form-item>

        <el-form-item label="姓名">
          <el-input v-model="newOrderInfo.name" />
        </el-form-item>

        <el-form-item label="身份证">
          <el-input v-model="newOrderInfo.idCard" />
        </el-form-item>

        <el-form-item label="房型">
          <el-select v-model="newOrderInfo.typeId">
            <el-option
              v-for="t in roomTypes"
              :key="t.typeId"
              :label="t.typeName + ' ￥' + t.price"
              :value="t.typeId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="数量">
          <el-input-number v-model="newOrderInfo.roomCount" :min="1" />
        </el-form-item>

        <el-form-item label="日期">
          <el-date-picker
            v-model="newOrderInfo.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>

        <el-form-item label="总价">
          ￥{{ totalPrice }}
        </el-form-item>

      </el-form>

      <template #footer>
        <el-button @click="showAddDialog=false">取消</el-button>
        <el-button type="primary" @click="submitOrder">提交</el-button>
      </template>
    </el-dialog>

    <!-- 订单列表 -->
    <el-table v-if="orderList.length" :data="orderList" border>

      <el-table-column prop="reservationId" label="订单号" width="120" />

      <el-table-column label="客户信息">
        <template #default="scope">
          <div>{{ scope.row.guestName }}</div>
          <div style="color:#999">{{ scope.row.phone }}</div>
        </template>
      </el-table-column>

      <el-table-column prop="typeName" label="房型" />
      <el-table-column prop="roomCount" label="数量" />
      <el-table-column prop="totalPrice" label="金额" />

      <el-table-column label="状态">
        <template #default="scope">
          <el-tag :type="getStatusStyle(scope.row.status)">
            {{ getStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>

      <!-- 入住 -->
      <el-table-column label="入住办理" width="320">
        <template #default="scope">

          <el-select
            v-model="scope.row.selectedRooms"
            multiple
            placeholder="选择房间"
            style="width:180px"
            @focus="loadRooms(scope.row)"
          >
            <el-option
              v-for="room in scope.row.availableRooms || []"
              :key="room.roomId"
              :label="room.roomNumber"
              :value="room.roomNumber"
            />
          </el-select>

          <el-button
            type="primary"
            size="small"
            style="margin-left:5px"
            @click="confirmCheckIn(scope.row)"
          >
            入住
          </el-button>

        </template>
      </el-table-column>

    </el-table>

    <el-empty v-else description="请输入手机号查询订单"/>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { reservationApi } from '@/api/reservation'
import { guestApi } from '@/api/guest'
import { roomTypeApi } from '@/api/roomType'
import { roomApi } from '@/api/room'
import { checkInApi } from '@/api/checkIn'
import { checkOutApi } from '@/api/checkOut'
import { ElMessage } from 'element-plus'

// ===== 数据 =====
const searchPhone = ref('')
const orderList = ref([])

const checkoutRoomNumber = ref('')

const showAddDialog = ref(false)
const roomTypes = ref([])
const totalPrice = ref(0)

// ===== 新增订单 =====
const newOrderInfo = reactive({
  phone: '',
  name: '',
  idCard: '',
  typeId: null,
  roomCount: 1,
  dateRange: []
})

const newOrder = reactive({
  hotelId: localStorage.getItem('hotelId'),
  guestId: null,
  typeId: null,
  roomCount: 1,
  totalPrice: 0,
  checkInDate: '',
  checkOutDate: ''
})

// ===== 状态 =====
const statusMap = {
  1: { text: '已预订', type: 'info' },
  2: { text: '已确认', type: 'primary' },
  3: { text: '已取消', type: 'danger' },
  4: { text: '已入住', type: 'success' },
  5: { text: '已完成', type: 'warning' }
}

const getStatusText = s => statusMap[s]?.text || '未知'
const getStatusStyle = s => statusMap[s]?.type || ''

// ===== 打开新增 =====
const openAddDialog = async () => {
  showAddDialog.value = true
  const res = await roomTypeApi.getAll(localStorage.getItem('hotelId'))
  roomTypes.value = res.data
}

// 自动填充客户
const autoFillGuest = async () => {
  const res = await guestApi.getByPhone(newOrderInfo.phone)
  if (res.data) {
    newOrder.guestId = res.data.guestId
    newOrderInfo.name = res.data.name
    newOrderInfo.idCard = res.data.idCard
  }
}

// 价格计算
watch(
  () => [newOrderInfo.dateRange, newOrderInfo.typeId, newOrderInfo.roomCount],
  () => {
    const type = roomTypes.value.find(t => t.typeId === newOrderInfo.typeId)
    if (!type || newOrderInfo.dateRange.length < 2) {
      totalPrice.value = 0
      return
    }
    const days = (new Date(newOrderInfo.dateRange[1]) - new Date(newOrderInfo.dateRange[0])) / 86400000
    totalPrice.value = days * type.price * newOrderInfo.roomCount
  }
)

// 提交订单
const submitOrder = async () => {
  let guestId = newOrder.guestId

  if (!guestId) {
    const res = await guestApi.getByPhone(newOrderInfo.phone)

    if (res.data) {
      guestId = res.data.guestId
    } else {
      const createRes = await guestApi.create({
        phone: newOrderInfo.phone,
        name: newOrderInfo.name,
        idCard: newOrderInfo.idCard
      })
      const newRes = await guestApi.getByPhone(newOrderInfo.phone)
      guestId = newRes.data.guestId
    }
  }

  newOrder.guestId = guestId
  newOrder.typeId = newOrderInfo.typeId
  newOrder.roomCount = newOrderInfo.roomCount
  newOrder.checkInDate = newOrderInfo.dateRange[0]
  newOrder.checkOutDate = newOrderInfo.dateRange[1]
  newOrder.totalPrice = totalPrice.value

  await reservationApi.create(newOrder)

  ElMessage.success('订单创建成功')
  showAddDialog.value = false
  handleSearch()
}

// ===== 查询订单（含客户信息补全）=====
const handleSearch = async () => {
  if(!searchPhone.value) {
    ElMessage.warning('请输入手机号')
    return
  }
  const res = await reservationApi.getByPhone(searchPhone.value)
  console.log('查询手机号：', searchPhone.value)
  const list = res.data || []

  for (let order of list) {
    const guestRes = await guestApi.getByPhone(order.phone)  
    order.guestName = guestRes.data?.name || '未知'

    order.selectedRooms = []
    order.availableRooms = []
  }

  orderList.value = list
}

// ===== 加载房间 =====
const loadRooms = async (order) => {
  const res = await roomApi.getByTS({
    hotelId: order.hotelId,
    typeId: order.typeId,
    status: 0
  })
  order.availableRooms = res.data
}

// ===== 入住 =====
const confirmCheckIn = async (order) => {
  if (!order.selectedRooms.length) {
    ElMessage.warning('请选择房间')
    return
  }

  if (order.selectedRooms.length > order.roomCount) {
    ElMessage.warning('超过预订数量')
    return
  }

  await checkInApi.create({
    reservationId: order.reservationId,
    roomNumbers: order.selectedRooms
  })

  ElMessage.success('入住成功')
  handleSearch()
}

// ===== 退房 =====
const confirmCheckOut = async () => {
  if (!checkoutRoomNumber.value) {
    ElMessage.warning('请输入房间号')
    return
  }

  await checkOutApi.create(checkoutRoomNumber.value)

  ElMessage.success('退房成功')
  handleSearch()
}
</script>

<style scoped>
.order-container { padding:20px; }
.search-bar { display:flex; gap:10px; margin-bottom:20px; align-items:center; }
</style>
