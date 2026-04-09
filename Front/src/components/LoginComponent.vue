<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'
import { loginWithGooglePopup } from '../services/firebaseClient'
import BaseButton from './ui/BaseButton.vue'
import AppIcon from './ui/AppIcon.vue'
import futbolinLogo from '../assets/FutbolIn_Icono.png'

const router = useRouter()
const authStore = useAuthStore()
const uiStore = useUiStore()

const loading = ref(false)
const popupNoticeVisible = ref(false)
let popupReminderTimeoutId = null

const titulo = computed(() => 'Inicia sesión')
const subtitulo = computed(() => 'Accede de forma segura con tu cuenta de Google.')

const parseFirebaseAuthError = (error) => {
  const code = String(error?.code || '')

  if (code === 'auth/popup-closed-by-user') {
    return {
      type: 'info',
      message: 'Has cerrado la ventana de Google. Cuando quieras, vuelve a pulsar "Continuar con Google".',
    }
  }

  if (code === 'auth/popup-blocked') {
    return {
      type: 'warning',
      message: 'Tu navegador ha bloqueado la ventana emergente. Permite popups para continuar.',
    }
  }

  return {
    type: 'error',
    message: error?.message || 'No se pudo iniciar sesión con Firebase',
  }
}

const clearPopupReminder = () => {
  if (popupReminderTimeoutId) {
    clearTimeout(popupReminderTimeoutId)
    popupReminderTimeoutId = null
  }
}

const handleAuth = async () => {
  loading.value = true
  popupNoticeVisible.value = false
  clearPopupReminder()

  popupReminderTimeoutId = setTimeout(() => {
    if (!loading.value) return
    popupNoticeVisible.value = true
  }, 4500)

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
    const feedback = parseFirebaseAuthError(error)
    uiStore.showToast({ message: feedback.message, type: feedback.type })
  } finally {
    clearPopupReminder()
    popupNoticeVisible.value = false
    loading.value = false
  }
}
</script>

<template>
  <div class="login-screen min-h-dvh px-4 py-6 sm:py-10 flex items-center justify-center relative overflow-hidden">
    <div class="login-bg" aria-hidden="true">
      <div class="logo-orb orb-a">
        <img :src="futbolinLogo" alt="" class="orb-logo" />
      </div>
      <div class="logo-orb orb-b">
        <img :src="futbolinLogo" alt="" class="orb-logo" />
      </div>
      <div class="logo-orb orb-c">
        <img :src="futbolinLogo" alt="" class="orb-logo" />
      </div>
      <div class="ambient-glow glow-a"></div>
      <div class="ambient-glow glow-b"></div>
      <div class="ambient-grid"></div>
    </div>

    <div class="w-full max-w-lg relative z-10">
      <div class="card-surface login-panel p-8 sm:p-10">
        <div class="text-center mb-8">
          <div class="inline-flex items-center gap-4 mx-auto rounded-2xl bg-[color:rgba(102,63,77,0.28)] border border-[color:var(--color-border)] px-5 py-4 shadow-sm login-brand-chip">
            <div class="w-16 h-16 flex items-center justify-center shrink-0">
              <img :src="futbolinLogo" alt="FUTBOLIN" class="w-full h-full object-contain object-[50%_50%] scale-[1.55]" />
            </div>
            <div class="text-left">
              <h1 class="text-3xl sm:text-4xl font-extrabold tracking-tight leading-none whitespace-nowrap">
                <span class="text-slate-100">FUTBOL</span><span class="text-[color:var(--color-secondary)]">IN</span>
              </h1>
              <p class="text-[11px] text-slate-300 mt-1">Cortita y al pie, organiza tu partido en segundos</p>
            </div>
          </div>
          <p class="text-slate-200 text-sm mt-1">{{ titulo }}</p>
          <p class="text-slate-300 text-xs mt-1">{{ subtitulo }}</p>
        </div>

        <form class="space-y-4" @submit.prevent="handleAuth">
          <BaseButton block variant="primary" :loading="loading" :disabled="loading" type="submit">
            Continuar con Google
          </BaseButton>
          <p v-if="loading" class="text-xs text-slate-300 text-center">
            Esperando confirmacion de Google en la ventana emergente...
          </p>
          <p v-if="loading && popupNoticeVisible" class="text-xs text-[color:var(--color-secondary)] text-center">
            El popup de Google ya se ha abierto. Completa la autorizacion en esa ventana para continuar.
          </p>
        </form>

        <div class="mt-8 bg-[color:rgba(151,240,125,0.14)] border border-[color:rgba(151,240,125,0.34)] rounded-xl px-4 py-3">
          <p class="text-xs font-semibold text-[color:var(--color-secondary)] inline-flex items-center gap-1.5">
            <AppIcon name="check" :size="14" />
            Acceso seguro
          </p>
          <p class="text-xs text-slate-200 mt-1">La autenticación se gestiona con Google mediante Firebase.</p>
        </div>
      </div>

      <p class="text-center text-slate-400 mt-4 text-xs">© 2026 FUTBOLIN</p>
    </div>
  </div>
</template>

<style scoped>
.login-screen {
  background:
    radial-gradient(1200px 560px at 10% -10%, rgba(151, 240, 125, 0.18), transparent 58%),
    radial-gradient(900px 460px at 85% 120%, rgba(56, 189, 248, 0.16), transparent 66%),
    linear-gradient(160deg, #02040b 0%, #040913 52%, #020308 100%);
}

.login-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.logo-orb {
  position: absolute;
  border-radius: 9999px;
  opacity: 0.17;
  background: rgba(2, 6, 23, 0.24);
  backdrop-filter: blur(3px);
}

.orb-logo {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: 50% 50%;
}

.orb-a {
  width: 320px;
  height: 320px;
  left: -58px;
  top: 8%;
  animation: floatDriftA 14s ease-in-out infinite;
}

.orb-b {
  width: 220px;
  height: 220px;
  right: -38px;
  top: 18%;
  animation: floatDriftB 16s ease-in-out infinite;
}

.orb-c {
  width: 270px;
  height: 270px;
  right: 12%;
  bottom: -72px;
  animation: floatDriftC 18s ease-in-out infinite;
}

.ambient-glow {
  position: absolute;
  border-radius: 9999px;
  filter: blur(44px);
  opacity: 0.42;
}

.glow-a {
  width: 260px;
  height: 260px;
  left: 16%;
  top: 58%;
  background: rgba(151, 240, 125, 0.38);
  animation: pulseGlow 8s ease-in-out infinite;
}

.glow-b {
  width: 220px;
  height: 220px;
  right: 18%;
  top: 24%;
  background: rgba(56, 189, 248, 0.32);
  animation: pulseGlow 9.5s ease-in-out infinite reverse;
}

.ambient-grid {
  position: absolute;
  inset: 0;
  opacity: 0.14;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.14) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.14) 1px, transparent 1px),
    radial-gradient(circle at 15% 20%, rgba(248, 250, 252, 0.32) 0 1px, transparent 2px),
    radial-gradient(circle at 80% 18%, rgba(248, 250, 252, 0.28) 0 1px, transparent 2px),
    radial-gradient(circle at 72% 72%, rgba(248, 250, 252, 0.24) 0 1px, transparent 2px),
    radial-gradient(circle at 30% 78%, rgba(248, 250, 252, 0.2) 0 1px, transparent 2px);
  background-size: 28px 28px, 28px 28px, 220px 220px, 260px 260px, 280px 280px, 240px 240px;
  mask-image: radial-gradient(circle at center, black 42%, transparent 92%);
}

.login-panel {
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(8, 14, 27, 0.82);
  box-shadow:
    0 20px 44px rgba(2, 6, 23, 0.56),
    0 0 0 1px rgba(151, 240, 125, 0.16);
  backdrop-filter: blur(6px);
}

.login-brand-chip {
  background: rgba(2, 6, 23, 0.42);
  border-color: rgba(148, 163, 184, 0.22);
  box-shadow:
    0 8px 18px rgba(2, 6, 23, 0.34),
    0 0 0 1px rgba(151, 240, 125, 0.18);
}

@keyframes floatDriftA {
  0%,
  100% {
    transform: translateY(0) translateX(0) rotate(0deg);
  }
  50% {
    transform: translateY(-18px) translateX(14px) rotate(8deg);
  }
}

@keyframes floatDriftB {
  0%,
  100% {
    transform: translateY(0) translateX(0) rotate(0deg);
  }
  50% {
    transform: translateY(14px) translateX(-12px) rotate(-7deg);
  }
}

@keyframes floatDriftC {
  0%,
  100% {
    transform: translateY(0) translateX(0) rotate(0deg);
  }
  50% {
    transform: translateY(-14px) translateX(-10px) rotate(6deg);
  }
}

@keyframes pulseGlow {
  0%,
  100% {
    opacity: 0.24;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(1.08);
  }
}

@media (max-width: 640px) {
  .orb-a {
    width: 230px;
    height: 230px;
    left: -70px;
    top: 10%;
  }

  .orb-b {
    width: 170px;
    height: 170px;
    right: -56px;
    top: 20%;
  }

  .orb-c {
    width: 190px;
    height: 190px;
    right: -20px;
    bottom: -56px;
  }
}
</style>
