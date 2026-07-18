import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  deleteShoppingListItem,
  getShoppingListItems,
  placeShoppingListOrder,
} from '../api/shoppingListApi'

const toFrontendItem = (item) => ({
  id: item.id,
  name: item.foodName,
  requiredQuantity: Number(item.requiredQuantity),
  availableFromInventory: Number(item.availableQuantity),
  quantityToBuy: Number(item.quantityToBuy),
  unit: item.unit,
  createTime: item.createTime,
  updateTime: item.updateTime,
})

export const useShoppingListStore = defineStore('shoppingList', () => {
  const items = ref([])
  const isLoading = ref(false)
  const error = ref('')

  const loadShoppingList = async () => {
    isLoading.value = true
    error.value = ''

    try {
      items.value = (await getShoppingListItems()).map(toFrontendItem)
    } catch (requestError) {
      items.value = []
      error.value = 'Could not load shopping list.'
      throw requestError
    } finally {
      isLoading.value = false
    }
  }

  const removeShoppingListItem = async (itemId) => {
    await deleteShoppingListItem(itemId)
    items.value = items.value.filter((item) => item.id !== itemId)
  }

  const placeOrder = async () => {
    await placeShoppingListOrder()
    items.value = []
  }

  return {
    items,
    isLoading,
    error,
    loadShoppingList,
    placeOrder,
    removeShoppingListItem,
  }
})
