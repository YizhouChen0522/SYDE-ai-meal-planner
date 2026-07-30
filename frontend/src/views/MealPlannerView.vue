<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getApiErrorMessage } from '../api/http'
import {
  addMealPlanRecipe,
  confirmCurrentMealPlanDraft,
  deleteCurrentMealPlanDraft,
  deleteMealPlanRecipe,
  generateMealPlanDraft,
  getCurrentMealPlanDraft,
  replaceMealPlanRecipe,
} from '../api/mealPlannerApi'
import { useInventoryStore } from '../stores/inventoryStore'

const router = useRouter()
const cravingText = ref('')
const mealPlan = ref([])
const currentDraft = ref(null)
const selectedStepsRecipe = ref(null)
const selectedNutritionRecipe = ref(null)
const inventoryStore = useInventoryStore()

const newInventoryItem = reactive({
  name: '',
  quantity: 1,
  unit: 'kg',
})
const isAddingInventoryItem = ref(false)
const isDraftLoading = ref(false)
const isGenerating = ref(false)
const isMutatingDraft = ref(false)
const isConfirming = ref(false)

const applyDraft = (draft) => {
  currentDraft.value = draft
  cravingText.value = draft?.userRequest || ''
  mealPlan.value = Array.isArray(draft?.recipes) ? draft.recipes : []
}

onMounted(async () => {
  try {
    isDraftLoading.value = true
    const [draft] = await Promise.all([
      getCurrentMealPlanDraft(),
      inventoryStore.loadInventory(),
    ])
    applyDraft(draft)
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, 'Could not load meal planner data.'))
  } finally {
    isDraftLoading.value = false
  }
})

const hasMealPlan = computed(() => mealPlan.value.length > 0)
const isStepsDialogVisible = computed({
  get: () => Boolean(selectedStepsRecipe.value),
  set: (value) => {
    if (!value) {
      selectedStepsRecipe.value = null
    }
  },
})
const isNutritionDialogVisible = computed({
  get: () => Boolean(selectedNutritionRecipe.value),
  set: (value) => {
    if (!value) {
      selectedNutritionRecipe.value = null
    }
  },
})

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

const generateRecipes = async () => {
  if (!cravingText.value.trim()) {
    ElMessage.warning('Please describe what you want to eat.')
    return
  }

  isGenerating.value = true

  try {
    applyDraft(await generateMealPlanDraft({ userRequest: cravingText.value.trim() }))
    ElMessage.success('Meal plan draft generated.')
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, 'Could not generate meal plan.'))
  } finally {
    isGenerating.value = false
  }
}

const clearPlan = async () => {
  if (!currentDraft.value) {
    applyDraft(null)
    return
  }

  isMutatingDraft.value = true

  try {
    await deleteCurrentMealPlanDraft()
    applyDraft(null)
    ElMessage.success('Meal plan draft deleted.')
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, 'Could not delete meal plan draft.'))
  } finally {
    isMutatingDraft.value = false
  }
}

const deleteRecipe = async (recipe) => {
  isMutatingDraft.value = true

  try {
    applyDraft(await deleteMealPlanRecipe(recipe.id))
    ElMessage.success(`${recipe.title} deleted.`)
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, 'Could not delete recipe.'))
  } finally {
    isMutatingDraft.value = false
  }
}

const confirmDeleteRecipe = async (recipe) => {
  try {
    await ElMessageBox.confirm(
      `Are you sure you want to delete ${recipe.title}?`,
      'Confirm deletion',
      {
        confirmButtonText: 'Confirm',
        cancelButtonText: 'Cancel',
        type: 'warning',
      },
    )

    await deleteRecipe(recipe)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(getApiErrorMessage(error, 'Could not delete recipe.'))
    }
  }
}

const replaceRecipe = async (id) => {
  isMutatingDraft.value = true

  try {
    applyDraft(await replaceMealPlanRecipe(id))
    ElMessage.success('Recipe replaced.')
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, 'Could not replace recipe.'))
  } finally {
    isMutatingDraft.value = false
  }
}

const addOneRecipe = async () => {
  isMutatingDraft.value = true

  try {
    applyDraft(await addMealPlanRecipe())
    ElMessage.success('Recipe added.')
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, 'Could not add recipe.'))
  } finally {
    isMutatingDraft.value = false
  }
}

const viewSteps = (recipe) => {
  selectedStepsRecipe.value = recipe
}

const viewNutrition = (recipe) => {
  selectedNutritionRecipe.value = recipe
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

const addItemToInventory = async () => {
  if (!newInventoryItem.name.trim()) {
    ElMessage.warning('Please enter a food name.')
    return
  }

  if (Number(newInventoryItem.quantity) <= 0) {
    ElMessage.warning('Please enter a quantity greater than 0.')
    return
  }

  if (!newInventoryItem.unit) {
    ElMessage.warning('Please select a unit.')
    return
  }

  isAddingInventoryItem.value = true

  try {
    await inventoryStore.addItem(newInventoryItem)
    ElMessage.success(`${newInventoryItem.name.trim()} added to inventory.`)

    newInventoryItem.name = ''
    newInventoryItem.quantity = 1
    newInventoryItem.unit = 'kg'
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, 'Could not add inventory item.'))
  } finally {
    isAddingInventoryItem.value = false
  }
}

const confirmMealPlan = async () => {
  if (!currentDraft.value) {
    ElMessage.warning('Please generate a meal plan first.')
    return
  }

  isConfirming.value = true

  try {
    await confirmCurrentMealPlanDraft()
    applyDraft(null)
    ElMessage.success('Meal plan confirmed successfully.')
    router.push('/shopping-list')
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, 'Could not confirm meal plan.'))
  } finally {
    isConfirming.value = false
  }
}
</script>

<template>
  <section class="page">
    <div class="page-heading">
      <p class="eyebrow">AI prototype</p>
      <h1>Home</h1>
      <p>Describe ingredients, constraints, or a weekly meal goal, then generate recipe ideas.</p>
    </div>

    <div class="home-layout">
      <section class="meal-planner-column">
        <el-card class="page-card planner-input" shadow="never" v-loading="isDraftLoading">
          <template #header>
            <h2>AI Meal Planner</h2>
          </template>

          <el-form label-position="top">
            <el-form-item label="What do you want to eat?">
              <el-input
                v-model="cravingText"
                type="textarea"
                :rows="4"
                placeholder="Example: I want spicy Chinese food, chicken, noodles, or a quick dinner."
              />
            </el-form-item>

            <div class="button-row">
              <el-button
                type="primary"
                :loading="isGenerating"
                :disabled="isDraftLoading || isMutatingDraft || isConfirming"
                @click="generateRecipes"
              >
                Generate 5 Recipes
              </el-button>
              <el-button :disabled="isDraftLoading || isGenerating || isMutatingDraft || isConfirming" @click="clearPlan">
                Clear
              </el-button>
            </div>
          </el-form>
        </el-card>

        <div v-if="hasMealPlan" class="recipe-grid">
          <el-card v-for="recipe in mealPlan" :key="recipe.id" class="recipe-card" shadow="never">
            <template #header>
              <div class="recipe-card-header">
                <h2>{{ recipe.title }}</h2>
              </div>
            </template>

            <h3>Ingredients</h3>
            <ul>
              <li v-for="ingredient in recipe.ingredients" :key="ingredient.name">
                {{ ingredient.name }}: {{ ingredient.quantity }} {{ ingredient.unit }}
              </li>
            </ul>

            <div class="recipe-detail-actions">
              <el-button plain @click="viewSteps(recipe)">View Steps</el-button>
              <el-button plain @click="viewNutrition(recipe)">View Nutrition</el-button>
            </div>

            <div class="recipe-actions">
              <el-button type="danger" plain :disabled="isMutatingDraft || isConfirming" @click="confirmDeleteRecipe(recipe)">
                Delete
              </el-button>
              <el-button plain :disabled="isMutatingDraft || isConfirming" @click="replaceRecipe(recipe.id)">
                Replace
              </el-button>
            </div>
          </el-card>
        </div>

        <div v-if="hasMealPlan" class="planner-actions">
          <el-button :loading="isMutatingDraft" :disabled="isConfirming" @click="addOneRecipe">
            + Add One Recipe
          </el-button>
          <el-button type="primary" :loading="isConfirming" :disabled="isMutatingDraft" @click="confirmMealPlan">
            Confirm Meal Plan
          </el-button>
        </div>
      </section>

      <aside class="inventory-add-column">
        <el-card class="page-card planner-inventory-card" shadow="never">
          <template #header>
            <div>
              <h2>Add Ingredients You Already Bought</h2>
              <p class="card-description">
                Use this when you bought ingredients outside the system and want to add them to your virtual fridge.
              </p>
            </div>
          </template>

          <el-form label-position="top" @submit.prevent="addItemToInventory">
            <el-form-item label="Food name" required>
              <el-input v-model="newInventoryItem.name" placeholder="Tomato" />
            </el-form-item>

            <el-form-item label="Quantity" required>
              <el-input-number v-model="newInventoryItem.quantity" :min="0" :step="0.1" :precision="2" />
            </el-form-item>

            <el-form-item label="Unit" required>
              <el-select v-model="newInventoryItem.unit" placeholder="Select unit">
                <el-option label="kg" value="kg" />
                <el-option label="g" value="g" />
                <el-option label="lb" value="lb" />
                <el-option label="piece" value="piece" />
                <el-option label="pieces" value="pieces" />
                <el-option label="cups" value="cups" />
              </el-select>
            </el-form-item>

            <el-button type="primary" native-type="submit" :loading="isAddingInventoryItem">
              Add to Inventory
            </el-button>
          </el-form>
        </el-card>
      </aside>
    </div>

    <el-dialog
      v-model="isStepsDialogVisible"
      :title="selectedStepsRecipe?.title"
      width="min(920px, calc(100vw - 32px))"
      class="recipe-details-dialog"
    >
      <div v-if="selectedStepsRecipe" class="recipe-details-layout">
        <section class="recipe-details-section recipe-details-ingredients" aria-labelledby="recipe-details-ingredients-heading">
          <h3 id="recipe-details-ingredients-heading">Ingredients</h3>
          <ul v-if="getRecipeIngredients(selectedStepsRecipe).length" class="dialog-list">
            <li v-for="ingredient in getRecipeIngredients(selectedStepsRecipe)" :key="`${ingredient.name || ingredient.foodName}-${ingredient.unit || ''}`">
              {{ formatIngredient(ingredient) }}
            </li>
          </ul>
          <p v-else class="muted-text">No ingredients saved for this recipe.</p>
        </section>

        <section class="recipe-details-section recipe-details-secondary" aria-labelledby="recipe-details-steps-heading">
          <h3 id="recipe-details-steps-heading">Preparation Steps</h3>
          <ol v-if="getRecipeSteps(selectedStepsRecipe).length" class="dialog-list">
            <li v-for="step in getRecipeSteps(selectedStepsRecipe)" :key="step">{{ formatStep(step) }}</li>
          </ol>
          <p v-else class="muted-text">No preparation steps saved for this recipe.</p>
        </section>
      </div>
    </el-dialog>

    <el-dialog
      v-model="isNutritionDialogVisible"
      :title="selectedNutritionRecipe?.title"
      width="min(920px, calc(100vw - 32px))"
      class="recipe-details-dialog"
    >
      <div v-if="selectedNutritionRecipe" class="recipe-details-layout">
        <section class="recipe-details-section recipe-details-ingredients" aria-labelledby="nutrition-details-ingredients-heading">
          <h3 id="nutrition-details-ingredients-heading">Ingredients</h3>
          <ul v-if="getRecipeIngredients(selectedNutritionRecipe).length" class="dialog-list">
            <li v-for="ingredient in getRecipeIngredients(selectedNutritionRecipe)" :key="`${ingredient.name || ingredient.foodName}-${ingredient.unit || ''}`">
              {{ formatIngredient(ingredient) }}
            </li>
          </ul>
          <p v-else class="muted-text">No ingredients saved for this recipe.</p>
        </section>

        <section class="recipe-details-section recipe-details-secondary" aria-labelledby="nutrition-details-heading">
          <h3 id="nutrition-details-heading">Nutrition</h3>
          <dl v-if="Object.keys(getRecipeNutrition(selectedNutritionRecipe)).length" class="nutrition-list">
            <div v-for="(nutrient, key) in getRecipeNutrition(selectedNutritionRecipe)" :key="key" class="nutrition-row">
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
