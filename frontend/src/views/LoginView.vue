<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/authStore'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  email: '',
  password: '',
})

const handleLogin = () => {
  authStore.login(form)
  ElMessage.success('Login successful.')
  router.push('/home')
}
</script>

<template>
  <section class="page">
    <el-card class="form-card" shadow="never">
      <template #header>
        <div>
          <p class="eyebrow">Account</p>
          <h1>Login</h1>
        </div>
      </template>

      <el-form label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="Email">
          <el-input v-model="form.email" type="email" placeholder="student@example.com" />
        </el-form-item>

        <el-form-item label="Password">
          <el-input v-model="form.password" type="password" placeholder="Password" show-password />
        </el-form-item>

        <el-button type="primary" native-type="submit">Login</el-button>
      </el-form>

      <p class="form-footer">
        Don't have an account?
        <router-link to="/register">Register first.</router-link>
      </p>
    </el-card>
  </section>
</template>
