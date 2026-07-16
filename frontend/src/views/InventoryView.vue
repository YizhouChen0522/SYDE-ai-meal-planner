<script setup>
import { computed, reactive, watchEffect } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useInventoryStore } from '../stores/inventoryStore'

const inventoryStore = useInventoryStore()

inventoryStore.loadInventoryFromStorage()

const consumeSelections = reactive({})

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

watchEffect(() => {
  inventoryStore.items.forEach((item) => {
    if (consumeSelections[item.id] === undefined) {
      consumeSelections[item.id] = 0
    }
  })
})

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
    return `Stored for ${daysStored} days`
  }

  if (daysStored >= 7) {
    return `Stored for ${daysStored} days`
  }

  return 'Fresh'
}

const applyConsumption = (item) => {
  const percent = consumeSelections[item.id] || 0

  if (percent <= 0) {
    ElMessage.warning('Please select a percentage greater than 0.')
    return
  }

  inventoryStore.applyConsumption(item.id, percent)
  ElMessage.success(`${percent}% of ${item.name} marked as consumed.`)
  consumeSelections[item.id] = 0
}

const deleteItem = (item) => {
  inventoryStore.removeItem(item.id)
  ElMessage.success(`${item.name} removed from inventory.`)
}

const confirmDeleteItem = async (item) => {
  try {
    await ElMessageBox.confirm(
      `Are you sure you want to delete ${item.name} from your inventory?`,
      'Confirm deletion',
      {
        confirmButtonText: 'Confirm',
        cancelButtonText: 'Cancel',
        type: 'warning',
      },
    )

    deleteItem(item)
  } catch (error) {
    // User cancelled. Do nothing.
  }
}
</script>

<template>
  <section class="page">
    <div class="page-heading">
      <p class="eyebrow">Virtual Fridge</p>
      <h1>Inventory</h1>
      <p>Track mock fridge and pantry items so future meal plans can use what you already have.</p>
    </div>

    <el-card class="page-card inventory-table-card" shadow="never">
      <template #header>
        <h2>Current Items</h2>
      </template>

      <el-table :data="inventoryRows" empty-text="No inventory items yet." class="inventory-table">
        <el-table-column prop="name" label="Food name" min-width="120" />
        <el-table-column label="Quantity" min-width="90">
          <template #default="{ row }">
            {{ row.quantity }} {{ row.unit }}
          </template>
        </el-table-column>
        <el-table-column prop="addedDate" label="Added date" min-width="120" />
        <el-table-column prop="daysStored" label="Days stored" min-width="70" />
        <el-table-column label="Storage reminder" min-width="160">
          <template #default="{ row }">
            <el-tag :type="getWarningType(row.daysStored)">
              {{ getWarningText(row.daysStored) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Consume" min-width="200">
          <template #default="{ row }">
            <div class="consume-controls">
              <el-slider
                class="consume-slider"
                :model-value="consumeSelections[row.id] ?? 0"
                :min="0"
                :max="100"
                :step="1"
                @update:model-value="value => (consumeSelections[row.id] = value)"
              />

              <span class="consume-percent">
                {{ consumeSelections[row.id] ?? 0 }}%
              </span>

              <el-button plain @click="applyConsumption(row)">
                Apply
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Delete" min-width="100">
          <template #default="{ row }">
            <el-button type="danger" plain @click="confirmDeleteItem(row)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </section>
</template>
