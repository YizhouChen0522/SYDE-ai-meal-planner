<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'

const requestText = ref('')
const mealPlan = ref([])

const mockRecipes = [
  {
    title: 'Lemon Chickpea Power Bowl',
    description: 'A bright grain bowl with chickpeas, greens, and a quick lemon yogurt sauce.',
    ingredients: ['Chickpeas', 'Brown rice', 'Spinach', 'Cucumber', 'Greek yogurt', 'Lemon'],
    steps: ['Warm chickpeas with olive oil and paprika.', 'Layer rice, spinach, and cucumber.', 'Mix yogurt with lemon and spoon over the bowl.'],
    prepTime: '25 minutes',
    difficulty: 'Easy',
    estimatedCost: '$9',
  },
  {
    title: 'Teriyaki Tofu Stir-Fry',
    description: 'Crispy tofu and vegetables tossed in a simple sweet-savory sauce.',
    ingredients: ['Tofu', 'Broccoli', 'Bell pepper', 'Soy sauce', 'Garlic', 'Rice'],
    steps: ['Pan-sear tofu until golden.', 'Stir-fry vegetables with garlic.', 'Add sauce and serve over rice.'],
    prepTime: '30 minutes',
    difficulty: 'Medium',
    estimatedCost: '$11',
  },
  {
    title: 'Turkey Taco Lettuce Cups',
    description: 'A lighter taco dinner with seasoned turkey, beans, and crunchy toppings.',
    ingredients: ['Ground turkey', 'Romaine lettuce', 'Black beans', 'Corn', 'Tomato', 'Avocado'],
    steps: ['Cook turkey with taco seasoning.', 'Warm beans and corn.', 'Fill lettuce cups and add toppings.'],
    prepTime: '20 minutes',
    difficulty: 'Easy',
    estimatedCost: '$13',
  },
  {
    title: 'Creamy Tomato Lentil Pasta',
    description: 'A pantry-friendly pasta with red lentils for protein and a creamy tomato sauce.',
    ingredients: ['Pasta', 'Red lentils', 'Crushed tomatoes', 'Garlic', 'Parmesan', 'Basil'],
    steps: ['Simmer lentils in tomato sauce.', 'Cook pasta until al dente.', 'Combine with parmesan and basil.'],
    prepTime: '35 minutes',
    difficulty: 'Medium',
    estimatedCost: '$10',
  },
  {
    title: 'Sheet Pan Salmon and Veg',
    description: 'A simple sheet pan meal with salmon, potatoes, and green beans.',
    ingredients: ['Salmon', 'Baby potatoes', 'Green beans', 'Dijon mustard', 'Lemon', 'Olive oil'],
    steps: ['Roast potatoes until tender.', 'Add salmon and green beans to the pan.', 'Brush with Dijon lemon glaze and finish roasting.'],
    prepTime: '40 minutes',
    difficulty: 'Easy',
    estimatedCost: '$16',
  },
  {
    title: 'Black Bean Sweet Potato Chili',
    description: 'A filling vegetarian chili that reheats well for lunches.',
    ingredients: ['Sweet potato', 'Black beans', 'Tomatoes', 'Onion', 'Chili powder', 'Vegetable broth'],
    steps: ['Saute onion with spices.', 'Add sweet potato, beans, tomatoes, and broth.', 'Simmer until thick and tender.'],
    prepTime: '45 minutes',
    difficulty: 'Easy',
    estimatedCost: '$8',
  },
]

const hasMealPlan = computed(() => mealPlan.value.length > 0)

const generateRecipes = () => {
  mealPlan.value = mockRecipes.slice(0, 5).map((recipe, index) => ({
    ...recipe,
    id: Date.now() + index,
  }))
}

const clearPlan = () => {
  mealPlan.value = []
  requestText.value = ''
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

const confirmMealPlan = () => {
  console.log('Confirmed meal plan:', mealPlan.value)
  ElMessage.success('Meal plan confirmed for this prototype.')
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
        <el-form-item label="Ingredients or request">
          <el-input
            v-model="requestText"
            type="textarea"
            :rows="5"
            placeholder="Example: I have tofu, rice, broccoli, and need affordable dinners for two."
          />
        </el-form-item>

        <div class="button-row">
          <el-button type="primary" @click="generateRecipes">Generate 5 Recipes</el-button>
          <el-button @click="clearPlan">Clear</el-button>
        </div>
      </el-form>
    </el-card>

    <div v-if="hasMealPlan" class="recipe-grid">
      <el-card v-for="recipe in mealPlan" :key="recipe.id" class="recipe-card" shadow="never">
        <template #header>
          <div class="recipe-card-header">
            <div>
              <h2>{{ recipe.title }}</h2>
              <p>{{ recipe.description }}</p>
            </div>
          </div>
        </template>

        <div class="recipe-meta">
          <el-tag>{{ recipe.prepTime }}</el-tag>
          <el-tag type="success">{{ recipe.difficulty }}</el-tag>
          <el-tag type="warning">{{ recipe.estimatedCost }}</el-tag>
        </div>

        <h3>Ingredients</h3>
        <ul>
          <li v-for="ingredient in recipe.ingredients" :key="ingredient">{{ ingredient }}</li>
        </ul>

        <h3>Steps</h3>
        <ol>
          <li v-for="step in recipe.steps" :key="step">{{ step }}</li>
        </ol>

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
  </section>
</template>
