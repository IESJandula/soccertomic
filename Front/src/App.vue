<script setup>
import { onMounted } from 'vue'
import ToastContainer from './components/ui/ToastContainer.vue'
import ConfirmModal from './components/ui/ConfirmModal.vue'

const applyTheme = () => {
  if (typeof document === 'undefined') return
  const theme = localStorage.getItem('theme') || 'light'
  document.documentElement.classList.toggle('dark', theme === 'dark')
}

onMounted(() => {
  applyTheme()
})
</script>

<template>
  <ToastContainer />
  <ConfirmModal />
  <router-view />
</template>

<style scoped>
</style>
