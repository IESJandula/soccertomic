<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'
import { loginWithGooglePopup } from '../services/firebaseClient'
import BaseButton from './ui/BaseButton.vue'
import AppIcon from './ui/AppIcon.vue'

const router = useRouter()
const authStore = useAuthStore()
const uiStore = useUiStore()

const loading = ref(false)

const titulo = computed(() => 'Inicia sesión')
const subtitulo = computed(() => 'Accede de forma segura con tu cuenta de Google.')

const handleAuth = async () => {
  loading.value = true

  try {
    const firebaseSession = await loginWithGooglePopup()
    const result = await authStore.loginWithFirebaseToken(firebaseSession)

    if (!result.success) {
      uiStore.showToast({ message: result.message || 'No se pudo completar la acción', type: 'error' })
      return
    }

    uiStore.showToast({ message: 'Sesión iniciada', type: 'success' })

    router.push(result.profileCompleted ? '/dashboard/partidos' : '/registro/perfil')
  } catch (error) {
    uiStore.showToast({ message: error?.message || 'No se pudo iniciar sesión con Firebase', type: 'error' })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-dvh px-4 py-6 sm:py-10 flex items-center justify-center">
    <div class="w-full max-w-lg">
      <div class="card-surface p-8 sm:p-10">
        <div class="text-center mb-8">
          <div class="w-14 h-14 mx-auto rounded-full bg-blue-700 text-white flex items-center justify-center">
            <AppIcon name="soccer" :size="26" />
          </div>
          <h1 class="text-2xl font-bold text-slate-800 mt-3">SeJuega!</h1>
          <p class="text-xs text-slate-500 mt-1">Cortita y al pie, organiza tu partido en segundos</p>
          <p class="text-slate-600 text-sm mt-1">{{ titulo }}</p>
          <p class="text-caption mt-1">{{ subtitulo }}</p>
        </div>

        <form class="space-y-4" @submit.prevent="handleAuth">
          <BaseButton block variant="primary" :loading="loading" :disabled="loading" type="submit">
            Continuar con Google
          </BaseButton>
        </form>

        <div class="mt-8 bg-blue-50 border border-blue-200 rounded-xl px-4 py-3">
          <p class="text-xs font-semibold text-blue-800 inline-flex items-center gap-1.5">
            <AppIcon name="check" :size="14" />
            Acceso seguro
          </p>
          <p class="text-xs text-blue-700 mt-1">La autenticación se gestiona con Google mediante Firebase.</p>
        </div>
      </div>

      <p class="text-center text-slate-500 mt-4 text-xs">© 2026 SeJuega!</p>
    </div>
  </div>
</template>
