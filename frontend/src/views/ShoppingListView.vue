<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getApiErrorMessage } from '../api/http'
import { useShoppingListStore } from '../stores/shoppingListStore'

const router = useRouter()
const shoppingListStore = useShoppingListStore()

const isPlacingOrder = ref(false)

const hasShoppingList = computed(() => shoppingListStore.items.length > 0)

const loadShoppingList = async () => {
  try {
    await shoppingListStore.loadShoppingList()
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, 'Could not load shopping list.'))
  }
}

onMounted(() => {
  loadShoppingList()
})

const placeOrder = async () => {
  if (!hasShoppingList.value) {
    ElMessage.warning('There are no shopping list items to order.')
    return
  }

  isPlacingOrder.value = true

  try {
    await shoppingListStore.placeOrder()
    ElMessage.success('Order placed. Items were added to inventory.')
    router.push('/inventory')
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, 'Could not place order.'))
  } finally {
    isPlacingOrder.value = false
  }
}

const removeShoppingListItem = async (item) => {
  await shoppingListStore.removeShoppingListItem(item.id)
  ElMessage.success(`${item.name} removed from shopping list.`)
}

const confirmRemoveShoppingListItem = async (item) => {
  try {
    await ElMessageBox.confirm(
      `Are you sure you want to remove ${item.name} from your shopping list?`,
      'Confirm deletion',
      {
        confirmButtonText: 'Confirm',
        cancelButtonText: 'Cancel',
        type: 'warning',
      },
    )

    await removeShoppingListItem(item)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(getApiErrorMessage(error, 'Could not remove shopping list item.'))
    }
  }
}
</script>

<template>
  <section class="page">
    <div class="page-heading">
      <p class="eyebrow">Order</p>
      <h1>Shopping List</h1>
      <p>Review what needs to be purchased after comparing the confirmed recipes with inventory.</p>
    </div>

    <el-alert
      v-if="shoppingListStore.error"
      :title="shoppingListStore.error"
      type="error"
      show-icon
      class="profile-alert"
      :closable="false"
    />

    <el-card class="page-card" shadow="never" v-loading="shoppingListStore.isLoading">
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
            <el-button type="danger" plain @click="confirmRemoveShoppingListItem(row)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="planner-actions">
        <el-button type="primary" :disabled="!hasShoppingList" :loading="isPlacingOrder" @click="placeOrder">
          Place Order
        </el-button>
      </div>
    </el-card>
  </section>
</template>
