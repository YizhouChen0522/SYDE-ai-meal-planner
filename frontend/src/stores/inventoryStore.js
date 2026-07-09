import { ref } from 'vue'
import { defineStore } from 'pinia'

const INVENTORY_KEY = 'syde-inventory-items'

const mockItems = [
  {
    id: 1,
    name: 'Chili',
    quantity: 1,
    unit: 'kg',
    addedDate: '2026-06-01',
    category: 'Vegetable',
    note: 'Bought outside the system',
  },
  {
    id: 2,
    name: 'Chicken breast',
    quantity: 0.8,
    unit: 'kg',
    addedDate: '2026-06-10',
    category: 'Protein',
    note: '',
  },
]

const readInventory = () => {
  try {
    const savedItems = localStorage.getItem(INVENTORY_KEY)
    return savedItems ? JSON.parse(savedItems) : mockItems
  } catch (error) {
    console.warn('Could not read inventory from localStorage', error)
    return mockItems
  }
}

const getToday = () => new Date().toISOString().slice(0, 10)

export const useInventoryStore = defineStore('inventory', () => {
  const items = ref([])

  const saveInventory = () => {
    localStorage.setItem(INVENTORY_KEY, JSON.stringify(items.value))
  }

  const loadInventoryFromStorage = () => {
    items.value = readInventory()
    saveInventory()
  }

  const addItem = (form) => {
    items.value.push({
      id: Date.now(),
      name: form.name,
      quantity: Number(form.quantity),
      unit: form.unit,
      addedDate: getToday(),
      category: form.category,
      note: form.note,
    })
    saveInventory()
  }

  const applyConsumption = (itemId, consumedPercent) => {
    const percent = Number(consumedPercent)

    if (percent === 100) {
      items.value = items.value.filter((item) => item.id !== itemId)
      saveInventory()
      return
    }

    items.value = items.value.map((item) => {
      if (item.id !== itemId) {
        return item
      }

      const remainingPercent = (100 - percent) / 100
      const nextQuantity = Number((item.quantity * remainingPercent).toFixed(2))

      return {
        ...item,
        quantity: nextQuantity,
      }
    })
    saveInventory()
  }

  return {
    items,
    addItem,
    applyConsumption,
    loadInventoryFromStorage,
  }
})
