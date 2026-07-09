<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useInventoryStore } from '../stores/inventoryStore'
import { useShoppingListStore } from '../stores/shoppingListStore'

const router = useRouter()
const inventoryStore = useInventoryStore()
const shoppingListStore = useShoppingListStore()

inventoryStore.loadInventoryFromStorage()
shoppingListStore.loadShoppingListFromStorage()

const hasShoppingList = computed(() => shoppingListStore.items.length > 0)

const placeMockOrder = () => {
  if (!hasShoppingList.value) {
    ElMessage.warning('There are no shopping list items to order.')
    return
  }

  shoppingListStore.items.forEach((item) => {
    inventoryStore.addOrMergeItem({
      id: inventoryStore.createInventoryId(),
      name: item.name,
      quantity: item.quantityToBuy,
      unit: item.unit,
    })
  })

  shoppingListStore.clearShoppingList()
  ElMessage.success('Mock order placed. Items were added to inventory.')
  router.push('/inventory')
}

const removeShoppingListItem = (item) => {
  shoppingListStore.removeShoppingListItem(item.id)
  ElMessage.success(`${item.name} removed from shopping list.`)
}
</script>

<template>
  <section class="page">
    <div class="page-heading">
      <p class="eyebrow">Mock Order</p>
      <h1>Shopping List</h1>
      <p>Review what needs to be purchased after comparing the confirmed recipes with inventory.</p>
    </div>

    <el-card class="page-card" shadow="never">
      <template #header>
        <h2>Items to Buy</h2>
      </template>

      <el-table :data="shoppingListStore.items" empty-text="No shopping list items yet." class="inventory-table">
        <el-table-column prop="name" label="Ingredient name" min-width="160" />
        <el-table-column prop="requiredQuantity" label="Required quantity" min-width="150" />
        <el-table-column prop="availableFromInventory" label="Available from Inventory" min-width="190" />
        <el-table-column prop="quantityToBuy" label="Quantity to buy" min-width="150" />
        <el-table-column prop="unit" label="Unit" min-width="90" />
        <el-table-column label="Delete" min-width="100">
          <template #default="{ row }">
            <el-button type="danger" plain @click="removeShoppingListItem(row)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="planner-actions">
        <el-button type="primary" :disabled="!hasShoppingList" @click="placeMockOrder">
          Place Mock Order
        </el-button>
      </div>
    </el-card>
  </section>
</template>
