<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getApiErrorMessage } from '../api/http'
import { getMealHistory } from '../api/mealHistoryApi'

const router = useRouter()
const historyRecords = ref([])
const selectedRecipe = ref(null)
const isLoading = ref(false)
const loadError = ref('')
const currentPage = ref(1)
const pageSize = ref(5)
const totalRecords = ref(0)
const totalPages = ref(0)

const hasHistory = computed(() => historyRecords.value.length > 0)
const hasAnyHistory = computed(() => totalRecords.value > 0)
const isDetailsDialogVisible = computed({
  get: () => Boolean(selectedRecipe.value),
  set: (value) => {
    if (!value) {
      selectedRecipe.value = null
    }
  },
})

const getRecipes = (record) => {
  return Array.isArray(record?.recipes) ? record.recipes : []
}

const nutritionLabels = {
  calories: 'Calories',
  protein: 'Protein',
  fat: 'Fat',
  carbohydrates: 'Carbohydrates',
  fiber: 'Fiber',
  vitaminC: 'Vitamin C',
  iron: 'Iron',
}

const formatNutritionLabel = (key) => {
  if (!key) {
    return 'Nutrition'
  }

  return nutritionLabels[key] || String(key)
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/[_-]+/g, ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase())
}

const getRecipeTitle = (recipe) => {
  return recipe?.title || 'Untitled recipe'
}

const getRecipeIngredients = (recipe) => {
  return Array.isArray(recipe?.ingredients) ? recipe.ingredients : []
}

const getRecipeSteps = (recipe) => {
  return Array.isArray(recipe?.steps) ? recipe.steps : []
}

const getRecipeNutrition = (recipe) => {
  return recipe?.nutrition && typeof recipe.nutrition === 'object' ? recipe.nutrition : {}
}

const formatIngredient = (ingredient) => {
  if (!ingredient) {
    return ''
  }

  const name = ingredient.name || ingredient.foodName || 'Ingredient'
  const details = [ingredient.quantity, ingredient.unit]
    .filter((value) => value !== null && value !== undefined && String(value).trim() !== '')
    .join(' ')

  return details ? `${name}: ${details}` : name
}

const formatStep = (step) => {
  return String(step || '').replace(/^\s*\d+[\).:-]\s*/, '')
}

const formatNutritionValue = (nutrient) => {
  if (!nutrient || typeof nutrient !== 'object') {
    return 'Not available'
  }

  const amount = nutrient.amount ?? ''
  const unit = nutrient.unit ?? ''
  const dailyValuePercent = nutrient.dailyValuePercent
  const value = [amount, unit]
    .filter((item) => item !== null && item !== undefined && String(item).trim() !== '')
    .join(' ')

  if (dailyValuePercent !== null && dailyValuePercent !== undefined && String(dailyValuePercent).trim() !== '') {
    return value
      ? `${value}, ${dailyValuePercent}% of recommended daily intake`
      : `${dailyValuePercent}% of recommended daily intake`
  }

  return value || 'Not available'
}

const formatDateTime = (dateValue) => {
  if (!dateValue) {
    return ''
  }

  return new Date(dateValue).toLocaleString()
}

const loadHistory = async (page = currentPage.value) => {
  isLoading.value = true
  loadError.value = ''

  try {
    const response = await getMealHistory({ page, size: pageSize.value })
    historyRecords.value = Array.isArray(response?.records) ? response.records : []
    currentPage.value = response?.page || page
    totalRecords.value = response?.total || 0
    totalPages.value = response?.totalPages || 0
  } catch (error) {
    historyRecords.value = []
    totalRecords.value = 0
    totalPages.value = 0
    loadError.value = getApiErrorMessage(error, 'Could not load meal history.')
    ElMessage.error(loadError.value)
  } finally {
    isLoading.value = false
  }
}

const openRecipeDialog = (recipe) => {
  selectedRecipe.value = recipe
}

const closeDialog = () => {
  selectedRecipe.value = null
}

const handlePageChange = async (page) => {
  if (page === currentPage.value || isLoading.value) {
    return
  }

  await loadHistory(page)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  loadHistory()
})
</script>

<template>
  <section class="page">
    <div class="page-heading">
      <p class="eyebrow">Saved plans</p>
      <h1>Meal Plan History</h1>
      <p>Review confirmed meal plans, recipe details, and the shopping list generated at confirmation.</p>
    </div>

    <el-alert
      v-if="loadError"
      :title="loadError"
      type="error"
      show-icon
      class="profile-alert"
      :closable="false"
    />

    <el-empty v-if="!isLoading && !hasAnyHistory" description="No confirmed meal plans yet.">
      <el-button type="primary" @click="router.push('/home')">Open Home</el-button>
    </el-empty>

    <div v-else v-loading="isLoading">
      <div class="history-list">
        <el-card
          v-for="record in historyRecords"
          :key="record.id"
          class="page-card history-card"
          shadow="never"
        >
          <template #header>
            <div class="history-card-header">
              <div>
                <h2>{{ formatDateTime(record.confirmedTime || record.createTime) }}</h2>
                <p>{{ getRecipes(record).length }} recipes confirmed</p>
              </div>
              <el-tag type="success">Confirmed</el-tag>
            </div>
          </template>

          <div class="history-section">
            <h3>Desired food input</h3>
            <p class="muted-text">{{ record.userRequest || 'No request entered.' }}</p>
          </div>

          <div class="history-section">
            <h3>Recipe titles</h3>
            <div v-if="getRecipes(record).length" class="history-recipe-list">
              <div
                v-for="recipe in getRecipes(record)"
                :key="recipe.id || getRecipeTitle(recipe)"
                class="history-recipe-item"
              >
                <h4>{{ getRecipeTitle(recipe) }}</h4>
                <div class="history-recipe-actions">
                  <el-button plain @click="openRecipeDialog(recipe)">
                    View Details
                  </el-button>
                </div>
              </div>
            </div>
            <p v-else class="muted-text">No recipes saved for this history item.</p>
          </div>

          <div class="history-section">
            <h3>Shopping List Generated</h3>
            <el-table
              v-if="record.shoppingListSnapshot?.length"
              :data="record.shoppingListSnapshot"
              class="inventory-table"
            >
              <el-table-column prop="name" label="Ingredient name" min-width="150" />
              <el-table-column prop="requiredQuantity" label="Required quantity" min-width="145" />
              <el-table-column prop="availableFromInventory" label="Available inventory quantity" min-width="210" />
              <el-table-column prop="quantityToBuy" label="Quantity to buy" min-width="140" />
              <el-table-column prop="unit" label="Unit" min-width="80" />
            </el-table>
            <p v-else class="muted-text">No shopping list snapshot saved for this plan.</p>
          </div>
        </el-card>
      </div>

      <el-empty v-if="!isLoading && hasAnyHistory && !hasHistory" description="No history records on this page." />

      <div v-if="hasAnyHistory" class="history-pagination">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :current-page="currentPage"
          :page-size="pageSize"
          :total="totalRecords"
          :disabled="isLoading"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <el-dialog
      v-model="isDetailsDialogVisible"
      :title="selectedRecipe ? getRecipeTitle(selectedRecipe) : 'Recipe Details'"
      width="min(1180px, calc(100vw - 32px))"
      class="recipe-details-dialog"
      @close="closeDialog"
    >
      <div v-if="selectedRecipe" class="recipe-details-layout recipe-details-layout-three">
        <section class="recipe-details-section recipe-details-ingredients" aria-labelledby="history-steps-ingredients-heading">
          <h3 id="history-steps-ingredients-heading">Ingredients</h3>
          <ul v-if="getRecipeIngredients(selectedRecipe).length" class="dialog-list">
            <li v-for="ingredient in getRecipeIngredients(selectedRecipe)" :key="`${ingredient.name || ingredient.foodName}-${ingredient.unit || ''}`">
              {{ formatIngredient(ingredient) }}
            </li>
          </ul>
          <p v-else class="muted-text">No ingredients saved for this recipe.</p>
        </section>

        <section class="recipe-details-section recipe-details-secondary" aria-labelledby="history-steps-heading">
          <h3 id="history-steps-heading">Preparation Steps</h3>
          <ol v-if="getRecipeSteps(selectedRecipe).length" class="dialog-list">
            <li v-for="step in getRecipeSteps(selectedRecipe)" :key="step">{{ formatStep(step) }}</li>
          </ol>
          <p v-else class="muted-text">No preparation steps saved for this recipe.</p>
        </section>

        <section class="recipe-details-section recipe-details-secondary" aria-labelledby="history-nutrition-heading">
          <h3 id="history-nutrition-heading">Nutrition</h3>
          <dl v-if="Object.keys(getRecipeNutrition(selectedRecipe)).length" class="nutrition-list">
            <div v-for="(nutrient, key) in getRecipeNutrition(selectedRecipe)" :key="key" class="nutrition-row">
              <dt>{{ formatNutritionLabel(key) }}</dt>
              <dd>{{ formatNutritionValue(nutrient) }}</dd>
            </div>
          </dl>
          <p v-else class="muted-text">No nutrition information saved for this recipe.</p>
        </section>
      </div>
    </el-dialog>
  </section>
</template>
