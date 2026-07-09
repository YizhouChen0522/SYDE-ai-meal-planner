import { ref } from 'vue'
import { defineStore } from 'pinia'

const SHOPPING_LIST_KEY = 'syde-shopping-list'

const normalizeName = (name) => name.trim().toLowerCase()
const getKey = (name, unit) => `${normalizeName(name)}|${unit.trim().toLowerCase()}`
const roundQuantity = (value) => Number(value.toFixed(2))

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

export const useShoppingListStore = defineStore('shoppingList', () => {
  const items = ref([])

  const saveShoppingList = () => {
    localStorage.setItem(SHOPPING_LIST_KEY, JSON.stringify(items.value))
  }

  const loadShoppingListFromStorage = () => {
    items.value = readSavedShoppingList()
  }

  const generateShoppingList = (recipes, inventoryItems) => {
    const requiredMap = new Map()
    const inventoryMap = new Map()

    recipes.forEach((recipe) => {
      recipe.ingredients.forEach((ingredient) => addQuantityToMap(requiredMap, ingredient))
    })

    inventoryItems.forEach((item) => addQuantityToMap(inventoryMap, item))

    items.value = Array.from(requiredMap.entries())
      .map(([key, requiredItem]) => {
        const availableFromInventory = inventoryMap.get(key)?.quantity || 0
        const quantityToBuy = roundQuantity(
          Math.max(0, requiredItem.quantity - availableFromInventory),
        )

        return {
          id: key,
          name: requiredItem.name,
          requiredQuantity: roundQuantity(requiredItem.quantity),
          availableFromInventory: roundQuantity(availableFromInventory),
          quantityToBuy,
          unit: requiredItem.unit,
        }
      })
      .filter((item) => item.quantityToBuy > 0)

    saveShoppingList()
  }

  return {
    items,
    generateShoppingList,
    loadShoppingListFromStorage,
  }
})
