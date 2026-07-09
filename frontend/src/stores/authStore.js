import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const TOKEN_KEY = 'syde-auth-token'
const USER_KEY = 'syde-current-user'
const PROFILE_KEY = 'syde-user-profile'

const defaultProfile = {
  likedFoods: '',
  dislikedFoods: '',
  allergies: '',
  dietaryRestrictions: [],
  flavorPreferences: [],
  cookingSkill: '',
  servingSize: 2,
  maxPrepTime: 30,
  budget: '',
  equipment: [],
}

const readJson = (key, fallback) => {
  try {
    const savedValue = localStorage.getItem(key)
    return savedValue ? JSON.parse(savedValue) : fallback
  } catch (error) {
    console.warn(`Could not read ${key} from localStorage`, error)
    return fallback
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref('')
  const currentUser = ref(null)
  const profile = ref({ ...defaultProfile })

  const isAuthenticated = computed(() => Boolean(token.value && currentUser.value))

  const saveProfile = (nextProfile) => {
    profile.value = {
      ...defaultProfile,
      ...nextProfile,
      equipment: [...(nextProfile.equipment || [])],
      dietaryRestrictions: [...(nextProfile.dietaryRestrictions || [])],
      flavorPreferences: [...(nextProfile.flavorPreferences || [])],
    }
    localStorage.setItem(PROFILE_KEY, JSON.stringify(profile.value))
  }

  const loadAuthFromStorage = () => {
    token.value = localStorage.getItem(TOKEN_KEY) || ''
    currentUser.value = readJson(USER_KEY, null)
    profile.value = {
      ...defaultProfile,
      ...readJson(PROFILE_KEY, defaultProfile),
    }
  }

  const login = (form) => {
    token.value = 'mock-jwt-token'
    currentUser.value = {
      id: 1,
      username: 'Demo User',
      email: form.email,
    }

    localStorage.setItem(TOKEN_KEY, token.value)
    localStorage.setItem(USER_KEY, JSON.stringify(currentUser.value))

    if (!localStorage.getItem(PROFILE_KEY)) {
      saveProfile(defaultProfile)
    } else {
      loadAuthFromStorage()
    }
  }

  const register = (form) => {
    console.log('Register form:', { ...form })
  }

  const logout = () => {
    token.value = ''
    currentUser.value = null
    profile.value = { ...defaultProfile }
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return {
    token,
    currentUser,
    profile,
    isAuthenticated,
    login,
    register,
    logout,
    loadAuthFromStorage,
    saveProfile,
  }
})
