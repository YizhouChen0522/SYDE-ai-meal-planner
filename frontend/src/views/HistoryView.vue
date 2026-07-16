<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useMealPlanStore } from '../stores/mealPlanStore'

const router = useRouter()
const mealPlanStore = useMealPlanStore()

mealPlanStore.loadHistoryFromStorage()

const selectedRecord = ref(null)
const activeDialog = ref('')

const hasHistory = computed(() => mealPlanStore.mealPlanHistory.length > 0)

const nutritionLabels = {
  calories: 'Calories',
  protein: 'Protein',
  fiber: 'Fiber',
  vitaminC: 'Vitamin C',
  iron: 'Iron',
}

const formatDateTime = (dateValue) => {
  return new Date(dateValue).toLocaleString()
}

const openDetails = (record, dialogName) => {
  selectedRecord.value = record
  activeDialog.value = dialogName
}

const closeDialog = () => {
  selectedRecord.value = null
  activeDialog.value = ''
}
</script>

<template>
  <section class="page">
    <div class="page-heading">
      <p class="eyebrow">Saved plans</p>
      <h1>Meal Plan History</h1>
      <p>Review confirmed mock meal plans, recipe details, and the shopping list generated at confirmation.</p>
    </div>

    <el-empty v-if="!hasHistory" description="No confirmed meal plans yet.">
      <el-button type="primary" @click="router.push('/home')">Open Home</el-button>
    </el-empty>

    <div v-else class="history-list">
      <el-card
        v-for="record in mealPlanStore.mealPlanHistory"
        :key="record.id"
        class="page-card history-card"
        shadow="never"
      >
        <template #header>
          <div class="history-card-header">
            <div>
              <h2>{{ formatDateTime(record.createdAt) }}</h2>
              <p>{{ record.recipes.length }} recipes confirmed</p>
            </div>
            <el-tag type="success">Confirmed</el-tag>
          </div>
        </template>

        <div class="history-section">
          <h3>Desired food input</h3>
          <p class="muted-text">{{ record.desiredFoodInput || 'No request entered.' }}</p>
        </div>

        <div class="history-section">
          <h3>Recipe titles</h3>
          <ul>
            <li v-for="recipe in record.recipes" :key="recipe.id">{{ recipe.title }}</li>
          </ul>
        </div>

        <div class="recipe-detail-actions">
          <el-button plain @click="openDetails(record, 'ingredients')">View Ingredients</el-button>
          <el-button plain @click="openDetails(record, 'steps')">View Steps</el-button>
          <el-button plain @click="openDetails(record, 'nutrition')">View Nutrition</el-button>
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

    <el-dialog
      :model-value="activeDialog === 'ingredients'"
      :title="selectedRecord ? `Ingredients - ${formatDateTime(selectedRecord.createdAt)}` : 'Ingredients'"
      width="640px"
      @close="closeDialog"
    >
      <div v-if="selectedRecord" class="history-dialog-content">
        <section v-for="recipe in selectedRecord.recipes" :key="recipe.id">
          <h3>{{ recipe.title }}</h3>
          <ul>
            <li v-for="ingredient in recipe.ingredients" :key="ingredient.name">
              {{ ingredient.name }}: {{ ingredient.quantity }} {{ ingredient.unit }}
            </li>
          </ul>
        </section>
      </div>
    </el-dialog>

    <el-dialog
      :model-value="activeDialog === 'steps'"
      :title="selectedRecord ? `Steps - ${formatDateTime(selectedRecord.createdAt)}` : 'Steps'"
      width="640px"
      @close="closeDialog"
    >
      <div v-if="selectedRecord" class="history-dialog-content">
        <section v-for="recipe in selectedRecord.recipes" :key="recipe.id">
          <h3>{{ recipe.title }}</h3>
          <ol>
            <li v-for="step in recipe.steps" :key="step">{{ step }}</li>
          </ol>
        </section>
      </div>
    </el-dialog>

    <el-dialog
      :model-value="activeDialog === 'nutrition'"
      :title="selectedRecord ? `Nutrition - ${formatDateTime(selectedRecord.createdAt)}` : 'Nutrition'"
      width="640px"
      @close="closeDialog"
    >
      <div v-if="selectedRecord" class="history-dialog-content">
        <section v-for="recipe in selectedRecord.recipes" :key="recipe.id">
          <h3>{{ recipe.title }}</h3>
          <p v-if="!recipe.nutrition" class="muted-text">No nutrition information saved for this recipe.</p>
          <div v-else class="nutrition-list">
            <p v-for="(nutrient, key) in recipe.nutrition" :key="key">
              <strong>{{ nutritionLabels[key] || key }}:</strong>
              {{ nutrient.amount }} {{ nutrient.unit }},
              {{ nutrient.dailyValuePercent }}% of recommended daily intake
            </p>
          </div>
        </section>
      </div>
    </el-dialog>
  </section>
</template>
