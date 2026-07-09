import { ref } from 'vue'
import { defineStore } from 'pinia'

const SHOPPING_LIST_KEY = 'syde-shopping-list'

const normalizeName = (name) => name.trim().toLowerCase()
const getKey = (name, unit) => `${normalizeName(name)}|${unit.trim().toLowerCase()}`
const roundQuantity = (value) => Number(value.toFixed(2))
const createShoppingListId = () => `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`

const addQuantityToMap = (map, item) => {
  const key = getKey(item.name, item.unit)
  const savedItem = map.get(key)

  if (savedItem) {
    savedItem.quantity = roundQuantity(savedItem.quantity + Number(item.quantity))
    return
  }

  map.set(key, {
    name: item.name,
    unit: item.unit,
    quantity: Number(item.quantity),
  })
}

const readSavedShoppingList = () => {
  try {
    const savedList = localStorage.getItem(SHOPPING_LIST_KEY)
    return savedList ? JSON.parse(savedList) : []
  } catch (error) {
    console.warn('Could not read shopping list from localStorage', error)
    return []
  }
}

const cleanShoppingListItems = (shoppingListItems) => {
  const usedIds = new Set()

  return shoppingListItems.map((item) => {
    let itemId = item.id

    if (!itemId || usedIds.has(itemId)) {
      itemId = createShoppingListId()
    }

    usedIds.add(itemId)

    return {
      id: itemId,
      name: item.name || '',
      requiredQuantity: Number(item.requiredQuantity) || 0,
      availableFromInventory: Number(item.availableFromInventory) || 0,
      quantityToBuy: Number(item.quantityToBuy) || 0,
      unit: item.unit || '',
    }
  })
}

export const useShoppingListStore = defineStore('shoppingList', () => {
  const items = ref([])

  const saveShoppingList = () => {
    localStorage.setItem(SHOPPING_LIST_KEY, JSON.stringify(items.value))
  }

  const loadShoppingListFromStorage = () => {
    items.value = cleanShoppingListItems(readSavedShoppingList())
    saveShoppingList()
  }

  const generateShoppingList = (recipes, inventoryItems) => {
    const requiredMap = new Map()
    const inventoryMap = new Map()

    recipes.forEach((recipe) => {
      recipe.ingredients.forEach((ingredient) => addQuantityToMap(requiredMap, ingredient))
    })

    inventoryItems.forEach((item) => addQuantityToMap(inventoryMap, item))

    const newItems = Array.from(requiredMap.entries())
      .map(([key, requiredItem]) => {
        const availableFromInventory = inventoryMap.get(key)?.quantity || 0
        const quantityToBuy = roundQuantity(
          Math.max(0, requiredItem.quantity - availableFromInventory),
        )

        return {
          id: createShoppingListId(),
          name: requiredItem.name,
          requiredQuantity: roundQuantity(requiredItem.quantity),
          availableFromInventory: roundQuantity(availableFromInventory),
          quantityToBuy,
          unit: requiredItem.unit,
        }
      })
      .filter((item) => item.quantityToBuy > 0)

    newItems.forEach((newItem) => {
      const matchingItem = items.value.find(
        (item) => normalizeName(item.name) === normalizeName(newItem.name) && item.unit === newItem.unit,
      )

      if (matchingItem) {
        matchingItem.requiredQuantity = roundQuantity(
          matchingItem.requiredQuantity + newItem.requiredQuantity,
        )
        matchingItem.availableFromInventory = newItem.availableFromInventory
        matchingItem.quantityToBuy = roundQuantity(matchingItem.quantityToBuy + newItem.quantityToBuy)
        return
      }

      items.value.push(newItem)
    })

    saveShoppingList()
  }

  const removeShoppingListItem = (itemId) => {
    items.value = items.value.filter((item) => item.id !== itemId)
    saveShoppingList()
  }

  const clearShoppingList = () => {
    items.value = []
    saveShoppingList()
  }

  return {
    items,
    clearShoppingList,
    generateShoppingList,
    loadShoppingListFromStorage,
    removeShoppingListItem,
  }
})
