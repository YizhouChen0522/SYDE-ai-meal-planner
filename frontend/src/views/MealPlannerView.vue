<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useInventoryStore } from '../stores/inventoryStore'
import { useMealPlanStore } from '../stores/mealPlanStore'
import { useShoppingListStore } from '../stores/shoppingListStore'

const router = useRouter()
const cravingText = ref('')
const mealPlan = ref([])
const selectedStepsRecipe = ref(null)
const selectedNutritionRecipe = ref(null)
const inventoryStore = useInventoryStore()
const mealPlanStore = useMealPlanStore()
const shoppingListStore = useShoppingListStore()

inventoryStore.loadInventoryFromStorage()
shoppingListStore.loadShoppingListFromStorage()

const newInventoryItem = reactive({
  name: '',
  quantity: 1,
  unit: 'kg',
})

const mockRecipes = [
  {
    id: 1,
    title: 'Lemon Chickpea Power Bowl',
    ingredients: [
      { name: 'Chickpeas', quantity: 0.3, unit: 'kg' },
      { name: 'Brown rice', quantity: 0.2, unit: 'kg' },
      { name: 'Spinach', quantity: 0.1, unit: 'kg' },
      { name: 'Cucumber', quantity: 1, unit: 'piece' },
      { name: 'Greek yogurt', quantity: 0.08, unit: 'kg' },
    ],
    steps: [
      'Warm chickpeas with olive oil, garlic, and paprika.',
      'Layer brown rice, spinach, cucumber, and chickpeas in a bowl.',
      'Mix Greek yogurt with lemon juice and spoon it over the bowl.',
    ],
    nutrition: {
      calories: { amount: 520, unit: 'kcal', dailyValuePercent: 26 },
      protein: { amount: 28, unit: 'g', dailyValuePercent: 56 },
      fiber: { amount: 9, unit: 'g', dailyValuePercent: 32 },
      vitaminC: { amount: 24, unit: 'mg', dailyValuePercent: 27 },
      iron: { amount: 4, unit: 'mg', dailyValuePercent: 22 },
    },
  },
  {
    id: 2,
    title: 'Teriyaki Tofu Stir-Fry',
    ingredients: [
      { name: 'Tofu', quantity: 0.35, unit: 'kg' },
      { name: 'Broccoli', quantity: 0.2, unit: 'kg' },
      { name: 'Bell pepper', quantity: 1, unit: 'piece' },
      { name: 'Soy sauce', quantity: 30, unit: 'ml' },
      { name: 'Rice', quantity: 0.2, unit: 'kg' },
    ],
    steps: [
      'Press tofu briefly, then cut it into cubes.',
      'Pan-sear tofu until the edges are golden.',
      'Stir-fry broccoli and bell pepper, then add soy sauce and tofu.',
      'Serve over warm rice.',
    ],
    nutrition: {
      calories: { amount: 610, unit: 'kcal', dailyValuePercent: 31 },
      protein: { amount: 32, unit: 'g', dailyValuePercent: 64 },
      fiber: { amount: 8, unit: 'g', dailyValuePercent: 29 },
      vitaminC: { amount: 88, unit: 'mg', dailyValuePercent: 98 },
      iron: { amount: 5, unit: 'mg', dailyValuePercent: 28 },
    },
  },
  {
    id: 3,
    title: 'Turkey Taco Lettuce Cups',
    ingredients: [
      { name: 'Ground turkey', quantity: 0.35, unit: 'kg' },
      { name: 'Romaine lettuce', quantity: 6, unit: 'leaves' },
      { name: 'Black beans', quantity: 0.2, unit: 'kg' },
      { name: 'Corn', quantity: 0.15, unit: 'kg' },
      { name: 'Tomato', quantity: 1, unit: 'piece' },
    ],
    steps: [
      'Cook ground turkey with taco seasoning until browned.',
      'Warm black beans and corn in a small pan.',
      'Spoon turkey, beans, corn, and tomato into lettuce leaves.',
    ],
    nutrition: {
      calories: { amount: 470, unit: 'kcal', dailyValuePercent: 24 },
      protein: { amount: 35, unit: 'g', dailyValuePercent: 70 },
      fiber: { amount: 10, unit: 'g', dailyValuePercent: 36 },
      vitaminC: { amount: 32, unit: 'mg', dailyValuePercent: 36 },
      iron: { amount: 4.5, unit: 'mg', dailyValuePercent: 25 },
    },
  },
  {
    id: 4,
    title: 'Creamy Tomato Lentil Pasta',
    ingredients: [
      { name: 'Pasta', quantity: 0.25, unit: 'kg' },
      { name: 'Red lentils', quantity: 0.15, unit: 'kg' },
      { name: 'Crushed tomatoes', quantity: 0.4, unit: 'kg' },
      { name: 'Garlic', quantity: 2, unit: 'cloves' },
      { name: 'Parmesan', quantity: 0.04, unit: 'kg' },
    ],
    steps: [
      'Simmer red lentils in crushed tomatoes with garlic.',
      'Cook pasta until al dente.',
      'Combine pasta with the lentil tomato sauce and parmesan.',
    ],
    nutrition: {
      calories: { amount: 680, unit: 'kcal', dailyValuePercent: 34 },
      protein: { amount: 30, unit: 'g', dailyValuePercent: 60 },
      fiber: { amount: 13, unit: 'g', dailyValuePercent: 46 },
      vitaminC: { amount: 20, unit: 'mg', dailyValuePercent: 22 },
      iron: { amount: 6, unit: 'mg', dailyValuePercent: 33 },
    },
  },
  {
    id: 5,
    title: 'Sheet Pan Salmon and Veg',
    ingredients: [
      { name: 'Salmon', quantity: 0.3, unit: 'kg' },
      { name: 'Baby potatoes', quantity: 0.35, unit: 'kg' },
      { name: 'Green beans', quantity: 0.2, unit: 'kg' },
      { name: 'Dijon mustard', quantity: 20, unit: 'g' },
      { name: 'Lemon', quantity: 1, unit: 'piece' },
    ],
    steps: [
      'Roast baby potatoes until they start to soften.',
      'Add salmon and green beans to the sheet pan.',
      'Brush salmon with Dijon and lemon, then finish roasting.',
    ],
    nutrition: {
      calories: { amount: 590, unit: 'kcal', dailyValuePercent: 30 },
      protein: { amount: 39, unit: 'g', dailyValuePercent: 78 },
      fiber: { amount: 7, unit: 'g', dailyValuePercent: 25 },
      vitaminC: { amount: 46, unit: 'mg', dailyValuePercent: 51 },
      iron: { amount: 3, unit: 'mg', dailyValuePercent: 17 },
    },
  },
  {
    id: 6,
    title: 'Black Bean Sweet Potato Chili',
    ingredients: [
      { name: 'Sweet potato', quantity: 0.35, unit: 'kg' },
      { name: 'Black beans', quantity: 0.3, unit: 'kg' },
      { name: 'Tomatoes', quantity: 0.4, unit: 'kg' },
      { name: 'Onion', quantity: 1, unit: 'piece' },
      { name: 'Vegetable broth', quantity: 0.5, unit: 'l' },
    ],
    steps: [
      'Saute onion with chili powder until fragrant.',
      'Add sweet potato, black beans, tomatoes, and vegetable broth.',
      'Simmer until the chili is thick and the sweet potato is tender.',
    ],
    nutrition: {
      calories: { amount: 540, unit: 'kcal', dailyValuePercent: 27 },
      protein: { amount: 22, unit: 'g', dailyValuePercent: 44 },
      fiber: { amount: 18, unit: 'g', dailyValuePercent: 64 },
      vitaminC: { amount: 38, unit: 'mg', dailyValuePercent: 42 },
      iron: { amount: 5.5, unit: 'mg', dailyValuePercent: 31 },
    },
  },
]

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
  fiber: 'Fiber',
  vitaminC: 'Vitamin C',
  iron: 'Iron',
}

const generateRecipes = () => {
  mealPlan.value = mockRecipes.slice(0, 5).map((recipe, index) => ({
    ...recipe,
    id: Date.now() + index,
  }))
}

const clearPlan = () => {
  mealPlan.value = []
  cravingText.value = ''
}

const deleteRecipe = (id) => {
  mealPlan.value = mealPlan.value.filter((recipe) => recipe.id !== id)
}

const replaceRecipe = (id) => {
  const existingTitles = mealPlan.value.map((recipe) => recipe.title)
  const replacement = mockRecipes.find((recipe) => !existingTitles.includes(recipe.title)) || mockRecipes[0]

  mealPlan.value = mealPlan.value.map((recipe) =>
    recipe.id === id ? { ...replacement, id: Date.now() } : recipe,
  )
}

const addOneRecipe = () => {
  const existingTitles = mealPlan.value.map((recipe) => recipe.title)
  const nextRecipe = mockRecipes.find((recipe) => !existingTitles.includes(recipe.title)) || mockRecipes[0]

  mealPlan.value.push({
    ...nextRecipe,
    id: Date.now(),
  })
}

const viewSteps = (recipe) => {
  selectedStepsRecipe.value = recipe
}

const viewNutrition = (recipe) => {
  selectedNutritionRecipe.value = recipe
}

const addItemToInventory = () => {
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

  inventoryStore.addItem(newInventoryItem)
  ElMessage.success(`${newInventoryItem.name.trim()} added to inventory.`)

  newInventoryItem.name = ''
  newInventoryItem.quantity = 1
  newInventoryItem.unit = 'kg'
}

const confirmMealPlan = () => {
  shoppingListStore.generateShoppingList(mealPlan.value, inventoryStore.items)
  mealPlanStore.confirmMealPlan({
    desiredFoodInput: cravingText.value,
    recipes: mealPlan.value,
    shoppingListSnapshot: shoppingListStore.items,
  })

  console.log('Confirmed meal plan:', mealPlanStore.currentMealPlanDraft)
  ElMessage.success('Meal plan confirmed. Shopping list generated.')
  router.push('/shopping-list')
}
</script>

<template>
  <section class="page">
    <div class="page-heading">
      <p class="eyebrow">AI prototype</p>
      <h1>Meal Planner</h1>
      <p>Describe ingredients, constraints, or a weekly meal goal, then generate mock recipe ideas.</p>
    </div>

    <el-card class="page-card planner-input" shadow="never">
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
          <el-button type="primary" @click="generateRecipes">Generate 5 Recipes</el-button>
          <el-button @click="clearPlan">Clear</el-button>
        </div>
      </el-form>
    </el-card>

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
        <div class="form-grid">
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

        </div>

        <el-button type="primary" native-type="submit">Add to Inventory</el-button>
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
          <el-button type="danger" plain @click="deleteRecipe(recipe.id)">Delete</el-button>
          <el-button plain @click="replaceRecipe(recipe.id)">Replace</el-button>
        </div>
      </el-card>
    </div>

    <div v-if="hasMealPlan" class="planner-actions">
      <el-button @click="addOneRecipe">+ Add One Recipe</el-button>
      <el-button type="primary" @click="confirmMealPlan">Confirm Meal Plan</el-button>
    </div>

    <el-dialog v-model="isStepsDialogVisible" :title="selectedStepsRecipe?.title" width="520px">
      <ol v-if="selectedStepsRecipe" class="dialog-list">
        <li v-for="step in selectedStepsRecipe.steps" :key="step">{{ step }}</li>
      </ol>
    </el-dialog>

    <el-dialog v-model="isNutritionDialogVisible" :title="selectedNutritionRecipe?.title" width="560px">
      <div v-if="selectedNutritionRecipe" class="nutrition-list">
        <p v-for="(nutrient, key) in selectedNutritionRecipe.nutrition" :key="key">
          <strong>{{ nutritionLabels[key] }}:</strong>
          {{ nutrient.amount }} {{ nutrient.unit }},
          {{ nutrient.dailyValuePercent }}% of recommended daily intake
        </p>
      </div>
    </el-dialog>
  </section>
</template>
