import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  createInventoryItem,
  deleteInventoryItem,
  getInventoryItems,
  updateInventoryItem,
} from '../api/inventoryApi'

const getToday = () => new Date().toISOString().slice(0, 10)
const normalizeName = (name) => name.trim().toLowerCase()
const roundQuantity = (value) => Number(value.toFixed(2))

const toFrontendItem = (item) => ({
  id: item.id,
  name: item.foodName,
  quantity: Number(item.quantity),
  unit: item.unit,
  addedDate: item.addedDate,
  reminderDays: item.reminderDays,
  createTime: item.createTime,
  updateTime: item.updateTime,
})

const toCreatePayload = (form) => ({
  foodName: form.name.trim(),
  quantity: Number(form.quantity),
  unit: form.unit.trim(),
  addedDate: form.addedDate || getToday(),
  reminderDays: Number(form.reminderDays ?? 0),
})

const toUpdatePayload = (item, quantity) => ({
  quantity: Number(quantity),
  addedDate: item.addedDate || getToday(),
  reminderDays: Number(item.reminderDays ?? 0),
})

export const useInventoryStore = defineStore('inventory', () => {
  const items = ref([])
  const isLoading = ref(false)
  const error = ref('')

  const loadInventory = async () => {
    isLoading.value = true
    error.value = ''

    try {
      items.value = (await getInventoryItems()).map(toFrontendItem)
    } catch (requestError) {
      items.value = []
      error.value = 'Could not load inventory.'
      throw requestError
    } finally {
      isLoading.value = false
    }
  }

  const addItem = async (form) => {
    const createdItem = toFrontendItem(await createInventoryItem(toCreatePayload(form)))
    items.value = [...items.value, createdItem]
    return createdItem
  }

  const addOrMergeItem = async (form) => {
    const itemName = form.name.trim()
    const itemUnit = form.unit.trim()
    const matchingItem = items.value.find(
      (item) => normalizeName(item.name) === normalizeName(itemName) && item.unit === itemUnit,
    )

    if (matchingItem) {
      const updatedQuantity = roundQuantity(matchingItem.quantity + Number(form.quantity))
      const updatedItem = toFrontendItem(
        await updateInventoryItem(matchingItem.id, toUpdatePayload(matchingItem, updatedQuantity)),
      )

      items.value = items.value.map((item) => (item.id === updatedItem.id ? updatedItem : item))
      return updatedItem
    }

    return addItem(form)
  }

  const removeItem = async (itemId) => {
    await deleteInventoryItem(itemId)
    items.value = items.value.filter((item) => item.id !== itemId)
  }

  const applyConsumption = async (itemId, consumedPercent) => {
    const item = items.value.find((inventoryItem) => inventoryItem.id === itemId)
    const percent = Number(consumedPercent)

    if (!item) {
      return null
    }

    if (percent === 100) {
      await removeItem(itemId)
      return null
    }

    const remainingPercent = (100 - percent) / 100
    const nextQuantity = roundQuantity(item.quantity * remainingPercent)
    const updatedItem = toFrontendItem(await updateInventoryItem(itemId, toUpdatePayload(item, nextQuantity)))

    items.value = items.value.map((inventoryItem) =>
      inventoryItem.id === updatedItem.id ? updatedItem : inventoryItem,
    )
    return updatedItem
  }

  return {
    items,
    isLoading,
    error,
    addItem,
    addOrMergeItem,
    applyConsumption,
    removeItem,
    loadInventory,
  }
})
