<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/authStore'

const router = useRouter()
const authStore = useAuthStore()

authStore.loadAuthFromStorage()

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="app-shell">
    <el-header class="top-nav">
      <router-link class="brand" to="/planner">
        <span class="brand-mark">SYDE660</span>
        <span>AI Meal Planner</span>
      </router-link>

      <nav class="nav-links" aria-label="Primary navigation">
        <template v-if="authStore.isAuthenticated">
          <router-link to="/planner">Meal Planner</router-link>
          <router-link to="/profile">Profile</router-link>
          <router-link to="/history">History</router-link>
          <el-button link class="nav-button" @click="handleLogout">Sign Out</el-button>
        </template>

        <template v-else>
          <router-link to="/login">Login</router-link>
          <router-link to="/register">Register</router-link>
        </template>
      </nav>
    </el-header>

    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>
