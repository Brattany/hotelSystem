import { tr } from 'element-plus/es/locale/index.mjs'
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useHotelStore = defineStore(
  'hotel', 
  () => {
    const hotelId = ref(null)
    const hotelName = ref('')

    function setHotel(id, name) {
      hotelId.value = id
      hotelName.value = name
    }

    function clearHotel() {
      hotelId.value = null
      hotelName.value = ''
    }

    return { hotelId, hotelName, setHotel, clearHotel }
  }, 
  {
    persist: {
      key: 'my-hotel-storage',
      storage: localStorage,
      paths: ['hotelId', 'hotelName']
    }
  }
)