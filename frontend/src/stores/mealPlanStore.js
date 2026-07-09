import { ref } from 'vue'
import { defineStore } from 'pinia'

const CURRENT_DRAFT_KEY = 'currentMealPlanDraft'
const HISTORY_KEY = 'syde-meal-plan-history'

const createHistoryId = () => `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`
const copyData = (value) => JSON.parse(JSON.stringify(value))

const readJson = (key, fallback) => {
  try {
    const savedValue = localStorage.getItem(key)
    return savedValue ? JSON.parse(savedValue) : fallback
  } catch (error) {
    console.warn(`Could not read ${key} from localStorage`, error)
    return fallback
  }
}

export const useMealPlanStore = defineStore('mealPlan', () => {
  const currentMealPlanDraft = ref(null)
  const mealPlanHistory = ref([])

  const saveCurrentDraft = () => {
    localStorage.setItem(CURRENT_DRAFT_KEY, JSON.stringify(currentMealPlanDraft.value))
  }

  const saveHistory = () => {
    localStorage.setItem(HISTORY_KEY, JSON.stringify(mealPlanHistory.value))
  }

  const loadHistoryFromStorage = () => {
    currentMealPlanDraft.value = readJson(CURRENT_DRAFT_KEY, null)
    mealPlanHistory.value = readJson(HISTORY_KEY, [])
  }

  const confirmMealPlan = ({ desiredFoodInput, recipes, shoppingListSnapshot }) => {
    const savedAt = new Date().toISOString()
    const savedRecipes = copyData(recipes)
    const savedShoppingList = copyData(shoppingListSnapshot || [])

    currentMealPlanDraft.value = {
      desiredFoodInput,
      recipes: savedRecipes,
    }

    const historyRecord = {
      id: createHistoryId(),
      createdAt: savedAt,
      desiredFoodInput,
      recipes: savedRecipes,
      shoppingListSnapshot: savedShoppingList,
    }

    mealPlanHistory.value = [historyRecord, ...mealPlanHistory.value]
    saveCurrentDraft()
    saveHistory()

    return historyRecord
  }

  return {
    currentMealPlanDraft,
    mealPlanHistory,
    confirmMealPlan,
    loadHistoryFromStorage,
  }
})
