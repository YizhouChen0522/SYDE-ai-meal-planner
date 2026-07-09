<script setup>
import { computed, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useInventoryStore } from '../stores/inventoryStore'

const inventoryStore = useInventoryStore()

inventoryStore.loadInventoryFromStorage()

const consumeSelections = reactive({})

const newItem = reactive({
  name: '',
  quantity: 1,
  unit: 'kg',
  category: '',
  note: '',
})

const getDaysStored = (addedDate) => {
  const added = new Date(`${addedDate}T00:00:00`)
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  const millisecondsPerDay = 1000 * 60 * 60 * 24
  return Math.max(0, Math.floor((today - added) / millisecondsPerDay))
}

const inventoryRows = computed(() =>
  inventoryStore.items.map((item) => ({
    ...item,
    daysStored: getDaysStored(item.addedDate),
  })),
)

const getWarningType = (daysStored) => {
  if (daysStored >= 14) {
    return 'danger'
  }

  if (daysStored >= 7) {
    return 'warning'
  }

  return 'info'
}

const getWarningText = (daysStored) => {
  if (daysStored >= 14) {
    return `Stored for ${daysStored} days. Use soon.`
  }

  if (daysStored >= 7) {
    return `Stored for ${daysStored} days`
  }

  return 'Fresh'
}

const applyConsumption = (item) => {
  const percent = consumeSelections[item.id] || 25
  inventoryStore.applyConsumption(item.id, percent)
  ElMessage.success(`${percent}% of ${item.name} marked as consumed.`)
}

const addManualItem = () => {
  inventoryStore.addItem(newItem)
  ElMessage.success('Inventory item added.')

  newItem.name = ''
  newItem.quantity = 1
  newItem.unit = 'kg'
  newItem.category = ''
  newItem.note = ''
}
</script>

<template>
  <section class="page">
    <div class="page-heading">
      <p class="eyebrow">Virtual Fridge</p>
      <h1>Inventory</h1>
      <p>Track mock fridge and pantry items so future meal plans can use what you already have.</p>
    </div>

    <el-card class="page-card inventory-form-card" shadow="never">
      <template #header>
        <h2>Add Item</h2>
      </template>

      <el-form label-position="top" @submit.prevent="addManualItem">
        <div class="form-grid">
          <el-form-item label="Food name" required>
            <el-input v-model="newItem.name" placeholder="Eggs" required />
          </el-form-item>

          <el-form-item label="Quantity" required>
            <el-input-number v-model="newItem.quantity" :min="0" :step="0.1" :precision="2" required />
          </el-form-item>

          <el-form-item label="Unit" required>
            <el-select v-model="newItem.unit" placeholder="Select unit">
              <el-option label="kg" value="kg" />
              <el-option label="g" value="g" />
              <el-option label="lb" value="lb" />
              <el-option label="pcs" value="pcs" />
              <el-option label="cups" value="cups" />
            </el-select>
          </el-form-item>

          <el-form-item label="Category" required>
            <el-select v-model="newItem.category" placeholder="Select category">
              <el-option label="Vegetable" value="Vegetable" />
              <el-option label="Fruit" value="Fruit" />
              <el-option label="Protein" value="Protein" />
              <el-option label="Grain" value="Grain" />
              <el-option label="Dairy" value="Dairy" />
              <el-option label="Pantry" value="Pantry" />
            </el-select>
          </el-form-item>

          <el-form-item label="Note">
            <el-input v-model="newItem.note" placeholder="Optional note" />
          </el-form-item>
        </div>

        <el-button type="primary" native-type="submit" :disabled="!newItem.name || !newItem.category">
          Add to Inventory
        </el-button>
      </el-form>
    </el-card>

    <el-card class="page-card inventory-table-card" shadow="never">
      <template #header>
        <h2>Current Items</h2>
      </template>

      <el-table :data="inventoryRows" empty-text="No inventory items yet." class="inventory-table">
        <el-table-column prop="name" label="Food name" min-width="150" />
        <el-table-column label="Quantity" min-width="120">
          <template #default="{ row }">
            {{ row.quantity }} {{ row.unit }}
          </template>
        </el-table-column>
        <el-table-column prop="addedDate" label="Added date" min-width="125" />
        <el-table-column prop="daysStored" label="Days stored" min-width="120" />
        <el-table-column prop="category" label="Category" min-width="120" />
        <el-table-column label="Storage reminder" min-width="220">
          <template #default="{ row }">
            <el-tag :type="getWarningType(row.daysStored)">
              {{ getWarningText(row.daysStored) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Consume" min-width="230">
          <template #default="{ row }">
            <div class="consume-controls">
              <el-select v-model="consumeSelections[row.id]" placeholder="25%">
                <el-option label="25%" :value="25" />
                <el-option label="50%" :value="50" />
                <el-option label="75%" :value="75" />
                <el-option label="100%" :value="100" />
              </el-select>
              <el-button plain @click="applyConsumption(row)">Apply</el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="note" label="Note" min-width="180" />
      </el-table>
    </el-card>
  </section>
</template>
