<script setup>
import { onMounted, ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { usePlayerProfileStore } from '../stores/playerProfile'
import apiService from '../services/apiService'
import BaseButton from './ui/BaseButton.vue'
import PlaystyleSelector from './survey/PlaystyleSelector.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const profileStore = usePlayerProfileStore()

const form = ref({
  playStyle: 'A',
  posicionPreferida: 'MEDIOCAMPISTA',
  selfAssessment: 3,
})

const uiState = ref({
  step: 1,
  formError: '',
  successMessage: '',
})

const stepSequence = [1, 2, 3]

const stepDescription = computed(() => {
  const descs = {
    1: 'Elige tu tendencia de juego inicial.',
    2: 'Define tu posición preferida y autovaloración.',
    3: 'Confirma tu perfil inicial. La autovaloración solo ajusta el arranque.',
  }
  return descs[uiState.value.step]
})

const isGoalkeeperTrend = computed(() => form.value.playStyle === 'G')
const canChooseGoalkeeperPosition = computed(() => ['G', 'A'].includes(form.value.playStyle))

const selfAssessmentLabel = computed(() => {
  const labels = {
    1: 'Muy baja',
    2: 'Baja',
    3: 'Media',
    4: 'Alta',
    5: 'Muy alta',
  }
  return labels[form.value.selfAssessment] || 'Media'
})

const tendenciaLabel = computed(() => {
  if (form.value.playStyle === 'G') return 'Portero'
  if (form.value.playStyle === 'O') return 'Ofensivo'
  if (form.value.playStyle === 'D') return 'Defensivo'
  return 'Adaptable'
})

const posicionLabel = computed(() => {
  if (form.value.posicionPreferida === 'PORTERO') return 'Portero'
  if (form.value.posicionPreferida === 'DEFENSA') return 'Defensa'
  if (form.value.posicionPreferida === 'DELANTERO') return 'Delantero'
  return 'Mediocampista'
})

const loadExistingProfile = async () => {
  const [perfil, resumen] = await Promise.all([
    profileStore.cargarMiPerfil(),
    apiService.getUsuarioResumen().catch(() => null),
  ])

  if (perfil?.attributes?.playStyle) {
    form.value.playStyle = perfil.attributes.playStyle
  }
  if (perfil?.attributes?.posicionPreferida) {
    form.value.posicionPreferida = perfil.attributes.posicionPreferida
  }
  if (Number.isFinite(Number(perfil?.attributes?.selfAssessment))) {
    form.value.selfAssessment = Number(perfil.attributes.selfAssessment)
  }
}

watch(
  () => form.value.playStyle,
  (playStyle) => {
    if (playStyle === 'G') {
      form.value.posicionPreferida = 'PORTERO'
      return
    }

    if (!canChooseGoalkeeperPosition.value && form.value.posicionPreferida === 'PORTERO') {
      form.value.posicionPreferida = 'MEDIOCAMPISTA'
    }
  },
  { immediate: true }
)

const nextStep = () => {
  const idx = stepSequence.indexOf(uiState.value.step)
  if (idx >= 0 && idx < stepSequence.length - 1) {
    uiState.value.step = stepSequence[idx + 1]
    uiState.value.formError = ''
  }
}

const prevStep = () => {
  const idx = stepSequence.indexOf(uiState.value.step)
  if (idx > 0) {
    uiState.value.step = stepSequence[idx - 1]
    uiState.value.formError = ''
  }
}

const validateProfile = () => {
  if (!['O', 'D', 'A', 'G'].includes(form.value.playStyle)) {
    return 'Tendencia inválida.'
  }
  if (!['DELANTERO', 'MEDIOCAMPISTA', 'DEFENSA', 'PORTERO'].includes(form.value.posicionPreferida)) {
    return 'Posición preferida inválida.'
  }
  if (![1, 2, 3, 4, 5].includes(Number(form.value.selfAssessment))) {
    return 'Autovaloración inválida.'
  }
  return ''
}

const guardarPerfil = async () => {
  uiState.value.formError = ''
  uiState.value.successMessage = ''

  const validationError = validateProfile()
  if (validationError) {
    uiState.value.formError = validationError
    return
  }

  const payload = {
    attributes: {
      goalkeeper: form.value.posicionPreferida === 'PORTERO' || form.value.playStyle === 'G',
      posicionPreferida: form.value.posicionPreferida,
      playStyle: form.value.playStyle,
      selfAssessment: Number(form.value.selfAssessment),
    },
  }

  try {
    const [result] = await Promise.all([
      profileStore.guardarMiPerfil(payload),
      apiService.upsertPerfil(authStore.user?.displayName || authStore.user?.name || '', authStore.user?.email, authStore.user?.displayName || authStore.user?.name || ''),
    ])

    if (!result.success) {
      uiState.value.formError = result.message || 'No se pudo guardar el perfil.'
      return
    }

    authStore.markPlayerProfileCompleted()
    uiState.value.successMessage = 'Perfil guardado.'
    setTimeout(() => {
      if (route.query?.edit === '1') {
        router.push('/dashboard/mi-perfil')
        return
      }
      router.push('/dashboard/partidos')
    }, 600)
  } catch (error) {
    const errorMsg = error?.message || 'Error de conexión al guardar el perfil'
    uiState.value.formError = errorMsg
  }
}

onMounted(() => {
  loadExistingProfile()
})
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-b from-slate-50 to-slate-100 p-4">
    <div class="w-full max-w-2xl space-y-3">
      <section class="card-surface p-4">
        <h2 class="text-xl font-bold text-slate-900">Encuesta de perfil inicial</h2>
        <p class="text-sm text-slate-600 mt-1">{{ stepDescription }}</p>
      </section>

      <div class="flex gap-1">
        <div
          v-for="(step, index) in stepSequence"
          :key="step"
          :class="[
            'flex-1 h-2 rounded-full transition-all',
            stepSequence.indexOf(uiState.step) >= index ? 'bg-blue-500' : 'bg-slate-200',
          ]"
          :aria-label="`Paso ${step}`"
        />
      </div>

      <section class="card-surface p-4 space-y-3">
        <template v-if="uiState.step === 1">
          <PlaystyleSelector
            :model-value="form.playStyle"
            :options="profileStore.playStyleOptions"
            @update:model-value="form.playStyle = $event"
          />
        </template>

        <template v-if="uiState.step === 2">
          <div class="space-y-3">
            <div>
              <label class="block text-sm font-semibold text-slate-900 mb-1.5">Posición preferida (opcional)</label>
              <select
                v-model="form.posicionPreferida"
                :disabled="isGoalkeeperTrend"
                class="w-full px-3 py-2 rounded-lg border border-slate-300 bg-white text-sm disabled:cursor-not-allowed disabled:opacity-80"
              >
                <option value="DELANTERO">Delantero</option>
                <option value="MEDIOCAMPISTA">Mediocampista</option>
                <option value="DEFENSA">Defensa</option>
                <option v-if="canChooseGoalkeeperPosition" value="PORTERO">Portero</option>
              </select>
              <p v-if="isGoalkeeperTrend" class="mt-1 text-xs text-slate-500">Tendencia Portero seleccionada: posición preferida bloqueada en Portero.</p>
            </div>

            <div>
              <div class="rounded-lg border border-slate-200 bg-slate-50 p-2.5">
                <div class="flex items-center justify-between mb-2">
                  <label class="text-sm font-semibold text-slate-900">Autovaloración inicial</label>
                  <span class="text-xs font-bold text-slate-700 bg-slate-200 px-2 py-0.5 rounded-full">
                    {{ form.selfAssessment }}/5
                  </span>
                </div>
                <input
                  v-model="form.selfAssessment"
                  type="range"
                  min="1"
                  max="5"
                  step="1"
                  class="w-full cursor-pointer accent-slate-900"
                  :aria-label="`Autovaloración inicial: ${form.selfAssessment} de 5`"
                />
                <div class="mt-2 flex items-center justify-between text-xs text-slate-500">
                  <span>1 · Muy baja</span>
                  <span>5 · Muy alta</span>
                </div>
                <div class="mt-2 rounded-lg bg-slate-100 px-3 py-2 text-center">
                  <p class="text-lg font-bold text-slate-800">{{ form.selfAssessment }} · {{ selfAssessmentLabel }}</p>
                </div>
              </div>
            </div>
          </div>
        </template>

        <template v-if="uiState.step === 3">
          <div class="space-y-3">
            <div class="bg-slate-50 rounded-lg p-3 text-sm">
              <div class="flex justify-between">
                <span class="text-slate-600">Tendencia:</span>
                <span class="font-semibold text-slate-900">{{ tendenciaLabel }}</span>
              </div>
              <div class="flex justify-between mt-1.5">
                <span class="text-slate-600">Posición preferida:</span>
                <span class="font-semibold text-slate-900">{{ posicionLabel }}</span>
              </div>
              <div class="flex justify-between mt-1.5">
                <span class="text-slate-600">Autovaloración:</span>
                <span class="font-semibold text-slate-900">{{ form.selfAssessment }} · {{ selfAssessmentLabel }}</span>
              </div>
            </div>

          </div>
        </template>

        <div v-if="uiState.formError" class="bg-red-50 border border-red-200 text-red-700 px-3 py-2 rounded-lg text-sm">
          {{ uiState.formError }}
        </div>
        <div v-if="uiState.successMessage" class="bg-emerald-50 border border-emerald-200 text-emerald-700 px-3 py-2 rounded-lg text-sm">
          {{ uiState.successMessage }}
        </div>
      </section>

      <div class="flex gap-2 justify-between">
        <BaseButton
          v-if="uiState.step > 1"
          type="button"
          variant="secondary"
          size="sm"
          @click="prevStep"
        >
          Atrás
        </BaseButton>
        <div v-else />

        <template v-if="uiState.step < 3">
          <BaseButton
            type="button"
            variant="primary"
            size="sm"
            @click="nextStep"
          >
            Siguiente
          </BaseButton>
        </template>
        <template v-else>
          <BaseButton
            type="button"
            variant="primary"
            size="sm"
            :loading="profileStore.loading"
            @click="guardarPerfil"
          >
            Guardar perfil
          </BaseButton>
        </template>
      </div>
    </div>
  </div>
</template>
