import { ref } from 'vue'
import { defineStore } from 'pinia'

const INVENTORY_KEY = 'syde-inventory-items'

const mockItems = [
  {
    id: 'mock-chili',
    name: 'Chili',
    quantity: 1,
    unit: 'kg',
    addedDate: '2026-06-01',
  },
  {
    id: 'mock-chicken-breast',
    name: 'Chicken breast',
    quantity: 0.8,
    unit: 'kg',
    addedDate: '2026-06-10',
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

const createInventoryId = () => `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`
const normalizeName = (name) => name.trim().toLowerCase()
const roundQuantity = (value) => Number(value.toFixed(2))

const cleanInventoryItems = (inventoryItems) => {
  const usedIds = new Set()

  return inventoryItems.map((item) => {
    let itemId = item.id

    if (!itemId || usedIds.has(itemId)) {
      itemId = createInventoryId()
    }

    usedIds.add(itemId)

    return {
      id: itemId,
      name: item.name || '',
      quantity: Number(item.quantity) || 0,
      unit: item.unit || '',
      addedDate: item.addedDate || getToday(),
    }
  })
}

export const useInventoryStore = defineStore('inventory', () => {
  const items = ref([])

  const saveInventory = () => {
    localStorage.setItem(INVENTORY_KEY, JSON.stringify(items.value))
  }

  const loadInventoryFromStorage = () => {
    items.value = cleanInventoryItems(readInventory())
    saveInventory()
  }

  const addItem = (form) => {
    items.value.push({
      id: form.id || createInventoryId(),
      name: form.name.trim(),
      quantity: Number(form.quantity),
      unit: form.unit,
      addedDate: form.addedDate || getToday(),
    })
    saveInventory()
  }

  const addOrMergeItem = (form) => {
    const itemName = form.name.trim()
    const itemUnit = form.unit
    const matchingItem = items.value.find(
      (item) => normalizeName(item.name) === normalizeName(itemName) && item.unit === itemUnit,
    )

    if (matchingItem) {
      matchingItem.quantity = roundQuantity(matchingItem.quantity + Number(form.quantity))
      saveInventory()
      return
    }

    addItem(form)
  }

  const removeItem = (itemId) => {
    items.value = items.value.filter((item) => item.id !== itemId)
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
    addOrMergeItem,
    applyConsumption,
    createInventoryId,
    removeItem,
    loadInventoryFromStorage,
  }
})
