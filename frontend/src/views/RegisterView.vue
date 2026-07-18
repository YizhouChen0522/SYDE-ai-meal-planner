<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getApiErrorMessage } from '../api/http'
import { useAuthStore } from '../stores/authStore'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  username: '',
  email: '',
  password: '',
})

const isSubmitting = ref(false)

const handleRegister = async () => {
  isSubmitting.value = true

  try {
    await authStore.register(form)
    ElMessage.success('Registration successful. Please log in.')
    router.push('/login')
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, 'Registration failed.'))
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <section class="page">
    <el-card class="form-card" shadow="never">
      <template #header>
        <div>
          <p class="eyebrow">Account</p>
          <h1>Register</h1>
        </div>
      </template>

      <el-form label-position="top" @submit.prevent="handleRegister">
        <el-form-item label="Username">
          <el-input v-model="form.username" placeholder="Your name" />
        </el-form-item>

        <el-form-item label="Email">
          <el-input v-model="form.email" type="email" placeholder="student@example.com" />
        </el-form-item>

        <el-form-item label="Password">
          <el-input v-model="form.password" type="password" placeholder="Password" show-password />
        </el-form-item>

        <el-button type="primary" native-type="submit" :loading="isSubmitting">Register</el-button>
      </el-form>

      <p class="form-footer">
        Already have an account?
        <router-link to="/login">Login here.</router-link>
      </p>
    </el-card>
  </section>
</template>
