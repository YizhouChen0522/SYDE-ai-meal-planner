<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getApiErrorMessage } from '../api/http'
import { getProfile, updateProfile } from '../api/profileApi'
import { useAuthStore } from '../stores/authStore'

const authStore = useAuthStore()

authStore.loadAuthFromStorage()

const profile = reactive({
  likedFoods: '',
  dislikedFoods: '',
  allergies: '',
  dietaryRestrictions: [...authStore.profile.dietaryRestrictions],
  flavorPreferences: [...authStore.profile.flavorPreferences],
  equipment: [...authStore.profile.equipment],
  cookingSkill: authStore.profile.cookingSkill,
  servingSize: authStore.profile.servingSize,
  maxPrepTime: authStore.profile.maxPrepTime,
  budget: authStore.profile.budget,
})

const isLoading = ref(true)
const isSaving = ref(false)
const loadError = ref('')

const toInputValue = (items) => {
  return Array.isArray(items) ? items.join(', ') : ''
}

const toPreferenceList = (value) => {
  if (!value) {
    return []
  }

  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

const applyBackendProfile = (backendProfile) => {
  profile.likedFoods = toInputValue(backendProfile?.likedFoods)
  profile.dislikedFoods = toInputValue(backendProfile?.dislikedFoods)
  profile.allergies = toInputValue(backendProfile?.allergies)
}

const persistLocalProfile = () => {
  authStore.saveProfile({
    ...profile,
    dietaryRestrictions: [...profile.dietaryRestrictions],
    flavorPreferences: [...profile.flavorPreferences],
    equipment: [...profile.equipment],
  })
}

const loadProfile = async () => {
  isLoading.value = true
  loadError.value = ''

  try {
    applyBackendProfile(await getProfile())
    persistLocalProfile()
  } catch (error) {
    loadError.value = getApiErrorMessage(error, 'Could not load profile.')
    ElMessage.error(loadError.value)
  } finally {
    isLoading.value = false
  }
}

const saveProfile = async () => {
  isSaving.value = true

  try {
    const savedProfile = await updateProfile({
      likedFoods: toPreferenceList(profile.likedFoods),
      dislikedFoods: toPreferenceList(profile.dislikedFoods),
      allergies: toPreferenceList(profile.allergies),
    })

    applyBackendProfile(savedProfile)
    persistLocalProfile()
    ElMessage.success('Profile saved.')
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, 'Could not save profile.'))
  } finally {
    isSaving.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<template>
  <section class="page">
    <div class="page-heading">
      <p class="eyebrow">Preferences</p>
      <h1>Food Preference Profile</h1>
      <p>Capture the constraints and tastes the meal planner should consider in later versions.</p>
    </div>

    <el-alert
      v-if="loadError"
      :title="loadError"
      type="error"
      show-icon
      class="profile-alert"
      :closable="false"
    />

    <el-card class="page-card" shadow="never" v-loading="isLoading">
      <el-form label-position="top" @submit.prevent="saveProfile">
        <div class="form-grid">
          <el-form-item label="Liked foods">
            <el-input v-model="profile.likedFoods" type="textarea" :rows="3" placeholder="Chicken, tofu, rice bowls" />
          </el-form-item>

          <el-form-item label="Disliked foods">
            <el-input v-model="profile.dislikedFoods" type="textarea" :rows="3" placeholder="Mushrooms, cilantro" />
          </el-form-item>

          <el-form-item label="Allergies">
            <el-input v-model="profile.allergies" placeholder="Peanuts, shellfish" />
          </el-form-item>

          <el-form-item label="Dietary restrictions">
            <el-select v-model="profile.dietaryRestrictions" multiple placeholder="Select restrictions">
              <el-option label="Vegetarian" value="Vegetarian" />
              <el-option label="Vegan" value="Vegan" />
              <el-option label="Gluten-free" value="Gluten-free" />
              <el-option label="Halal" value="Halal" />
              <el-option label="Dairy-free" value="Dairy-free" />
            </el-select>
          </el-form-item>

          <el-form-item label="Flavor preferences">
            <el-select v-model="profile.flavorPreferences" multiple placeholder="Select flavors">
              <el-option label="Savory" value="Savory" />
              <el-option label="Spicy" value="Spicy" />
              <el-option label="Fresh" value="Fresh" />
              <el-option label="Sweet" value="Sweet" />
              <el-option label="Umami" value="Umami" />
            </el-select>
          </el-form-item>

          <el-form-item label="Cooking skill">
            <el-select v-model="profile.cookingSkill" placeholder="Select skill level">
              <el-option label="Beginner" value="Beginner" />
              <el-option label="Intermediate" value="Intermediate" />
              <el-option label="Advanced" value="Advanced" />
            </el-select>
          </el-form-item>

          <el-form-item label="Serving size">
            <el-input-number v-model="profile.servingSize" :min="1" :max="12" />
          </el-form-item>

          <el-form-item label="Max preparation time">
            <el-input-number v-model="profile.maxPrepTime" :min="10" :max="180" :step="5" />
          </el-form-item>

          <el-form-item label="Budget">
            <el-select v-model="profile.budget" placeholder="Select budget">
              <el-option label="Low" value="Low" />
              <el-option label="Medium" value="Medium" />
              <el-option label="Flexible" value="Flexible" />
            </el-select>
          </el-form-item>

          <el-form-item label="Kitchen equipment">
            <el-select v-model="profile.equipment" multiple placeholder="Select equipment">
              <el-option label="Stovetop" value="Stovetop" />
              <el-option label="Oven" value="Oven" />
              <el-option label="Microwave" value="Microwave" />
              <el-option label="Air fryer" value="Air fryer" />
              <el-option label="Blender" value="Blender" />
            </el-select>
          </el-form-item>
        </div>

        <el-button type="primary" native-type="submit" :loading="isSaving" :disabled="isLoading">
          Save Profile
        </el-button>
      </el-form>
    </el-card>
  </section>
</template>
