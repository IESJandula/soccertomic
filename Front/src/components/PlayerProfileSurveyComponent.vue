<script setup>
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { usePlayerProfileStore } from '../stores/playerProfile'
import BaseButton from './ui/BaseButton.vue'
import AttributeSlider from './survey/AttributeSlider.vue'
import PresetSelector from './survey/PresetSelector.vue'
import PlaystyleSelector from './survey/PlaystyleSelector.vue'
import AgeRangeSelector from './survey/ExperienceSelector.vue'
import SkillTierSelector from './survey/SkillTierSelector.vue'
import FootSelector from './FootSelector.vue'
import AvailabilitySelector from './AvailabilitySelector.vue'
import TierIcon from './ui/TierIcon.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const profileStore = usePlayerProfileStore()
const isFutbolistaMode = computed(() => route.query?.mode === 'futbolista')

// Form state
const form = ref({
  goalkeeper: false,
  posicionPreferida: 'MEDIOCAMPISTA',
  shooting: 3,
  speed: 3,
  dribbling: 3,
  defense: 3,
  strength: 3,
  stamina: 3,
  aerial: 3,
  playStyle: 'A',
  skillTier: 'BRONCE',
  ageRange: '18_25',
  piernaBuena: '',
  disponibilidad: [],
})

const uiState = ref({
  step: 1, // 1: preset, 2: skillTier, 3: foot, 4: availability, 5: attributes, 6: confirmation
  selectedPreset: null,
  formError: '',
  successMessage: '',
})

const playTendency = computed(() => {
  // Calcular puntuación ofensiva (shooting, speed, dribbling)
  const offensive = form.value.shooting + form.value.speed + form.value.dribbling
  
  // Calcular puntuación defensiva (defense, strength, aerial)
  const defensive = form.value.defense + form.value.strength + form.value.aerial
  
  // Diferencia para determinar tendencia
  const diff = Math.abs(offensive - defensive)
  
  // Si la diferencia es menor a 3, es adaptable
  if (diff < 3) {
    return { label: 'Adaptable', color: 'text-purple-600' }
  }
  
  // Si es mayor, determinar si es ofensiva o defensiva
  if (offensive > defensive) {
    return { label: 'Ofensiva', color: 'text-red-600' }
  } else {
    return { label: 'Defensiva', color: 'text-blue-600' }
  }
})

const skillTierLabel = computed(() => {
  const tier = profileStore.skillTierOptions.find(t => t.value === form.value.skillTier)
  return tier ? { label: tier.label, value: tier.value } : { label: 'Bronce', value: 'BRONCE' }
})

const ageRangeLabel = computed(() => {
  const option = profileStore.ageRangeOptions.find(item => item.value === form.value.ageRange)
  return option ? option.label : '18 a 25'
})

const playStyleLabel = computed(() => {
  if (form.value.playStyle === 'O') return 'Ofensiva'
  if (form.value.playStyle === 'D') return 'Defensiva'
  return 'Adaptable'
})

const categoriaAtributo = (valor) => {
  const v = Number(valor)
  if (!Number.isFinite(v)) return 'Nulo'
  const labels = {
    0: 'Nulo',
    1: 'Muy bajo',
    2: 'Bajo',
    3: 'Medio',
    4: 'Alto',
    5: 'Muy alto',
  }
  return labels[Math.round(v)] || 'Nulo'
}

const stepDescription = computed(() => {
  const descs = {
    1: 'Empieza con un perfil predefinido según tu posición (puedes ajustar después)',
    2: 'Evalúa tu nivel general de habilidad futbolística',
    3: 'Selecciona tu pierna buena',
    4: 'Elige cuándo prefieres jugar',
    5: 'Ajusta tus habilidades, estilo de juego y rango de edad',
    6: 'Valida tu perfil antes de guardar',
  }
  if (isFutbolistaMode.value) {
    const modoFutbolista = {
      1: descs[1],
      2: descs[2],
      5: 'Ajusta tus habilidades y estilo de juego',
      6: descs[6],
    }
    return modoFutbolista[uiState.value.step]
  }
  return descs[uiState.value.step]
})

const stepSequence = computed(() => isFutbolistaMode.value ? [1, 2, 5, 6] : [1, 2, 3, 4, 5, 6])

const progressSteps = computed(() => stepSequence.value)

// Métodos
const loadExistingProfile = async () => {
  const perfil = await profileStore.cargarMiPerfil()
  if (perfil?.attributes) {
    form.value = { ...perfil.attributes }
    // Asegurar que skillTier existe (para perfiles antiguos)
    if (!form.value.skillTier) {
      form.value.skillTier = 'BRONCE'
    }
    if (!form.value.ageRange && form.value.experienceLevel) {
      form.value.ageRange = '18_25'
    } else if (!form.value.ageRange) {
      form.value.ageRange = '18_25'
    }
    if (!form.value.posicionPreferida) {
      form.value.posicionPreferida = form.value.goalkeeper ? 'PORTERO' : 'MEDIOCAMPISTA'
    }
    // Asegurar que disponibilidad es un array
    if (typeof form.value.disponibilidad === 'string') {
      form.value.disponibilidad = form.value.disponibilidad.split(',')
    } else if (!form.value.disponibilidad) {
      form.value.disponibilidad = []
    }
  }
}

const applyPreset = (presetKey) => {
  const preset = profileStore.presetProfiles[presetKey]
  if (preset) {
    // Guardar el skillTier actual antes de aplicar el preset
    const currentSkillTier = form.value.skillTier
    const currentPierna = form.value.piernaBuena
    const currentDisponibilidad = form.value.disponibilidad
    
    // Aplicar atributos del preset
    const presetAttrs = { ...preset.attributes }
    Object.keys(presetAttrs).forEach(key => {
      form.value[key] = presetAttrs[key]
    })
    
    // Restaurar valores que el usuario ya configuró
    form.value.skillTier = currentSkillTier || 'BRONCE'
    form.value.piernaBuena = currentPierna
    form.value.disponibilidad = currentDisponibilidad

    if (presetKey === 'GOALKEEPER') {
      form.value.posicionPreferida = 'PORTERO'
      form.value.goalkeeper = true
    } else if (presetKey === 'DEFENDER') {
      form.value.posicionPreferida = 'DEFENSA'
      form.value.goalkeeper = false
    } else if (presetKey === 'MIDFIELDER') {
      form.value.posicionPreferida = 'MEDIOCAMPISTA'
      form.value.goalkeeper = false
    } else if (presetKey === 'STRIKER') {
      form.value.posicionPreferida = 'DELANTERO'
      form.value.goalkeeper = false
    }
    
    uiState.value.selectedPreset = presetKey
  }
}

const nextStep = () => {
  const idx = stepSequence.value.indexOf(uiState.value.step)
  if (idx >= 0 && idx < stepSequence.value.length - 1) {
    uiState.value.step = stepSequence.value[idx + 1]
    uiState.value.formError = ''
  }
}

const prevStep = () => {
  const idx = stepSequence.value.indexOf(uiState.value.step)
  if (idx > 0) {
    uiState.value.step = stepSequence.value[idx - 1]
    uiState.value.formError = ''
  }
}

const validateProfile = () => {
  // Validar rango numérico
  const skills = ['shooting', 'speed', 'dribbling', 'defense', 'strength', 'stamina', 'aerial']
  for (const skill of skills) {
    if (typeof form.value[skill] !== 'number' || form.value[skill] < 0 || form.value[skill] > 5) {
      return `${skill}: valor debe estar entre 0–5.`
    }
  }

  // PlayStyle debe ser válido
  if (!['O', 'D', 'A'].includes(form.value.playStyle)) {
    return 'Estilo de juego inválido.'
  }

  if (!['DELANTERO', 'MEDIOCAMPISTA', 'DEFENSA', 'PORTERO'].includes(form.value.posicionPreferida)) {
    return 'Posición preferida inválida.'
  }

  // SkillTier debe ser válido
  if (!['BRONCE', 'PLATA', 'ORO', 'DIAMANTE'].includes(form.value.skillTier)) {
    return 'Nivel de habilidad inválido.'
  }

  // Rango de edad válido
  if (!['UNDER_18', '18_25', '25_35', '35_50', 'OVER_50'].includes(form.value.ageRange)) {
    return 'Rango de edad inválido.'
  }

  if (!isFutbolistaMode.value) {
    if (!form.value.piernaBuena) {
      return 'Por favor selecciona tu pierna buena.'
    }

    if (!form.value.disponibilidad || form.value.disponibilidad.length === 0) {
      return 'Por favor selecciona al menos una disponibilidad.'
    }
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
      goalkeeper: form.value.posicionPreferida === 'PORTERO',
      posicionPreferida: form.value.posicionPreferida,
      shooting: form.value.shooting,
      speed: form.value.speed,
      dribbling: form.value.dribbling,
      defense: form.value.defense,
      strength: form.value.strength,
      stamina: form.value.stamina,
      aerial: form.value.aerial,
      playStyle: form.value.playStyle,
      skillTier: form.value.skillTier,
      ageRange: form.value.ageRange,
      piernaBuena: form.value.piernaBuena,
      disponibilidad: form.value.disponibilidad,
    },
  }

  try {
    const result = await profileStore.guardarMiPerfil(payload)
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
    console.error('Error guardando perfil:', error)
    const errorMsg = error?.message || 'Error de conexión al guardar el perfil'
    const details = error?.data?.message ? ` - ${error.data.message}` : ''
    uiState.value.formError = `${errorMsg}${details}`
  }
}

onMounted(() => {
  loadExistingProfile()
})
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-b from-slate-50 to-slate-100 p-4">
    <div class="w-full max-w-2xl space-y-3">
      <!-- Header -->
      <section class="card-surface p-4">
        <h2 class="text-xl font-bold text-slate-900">Encuesta de perfil futbolístico</h2>
        <p class="text-sm text-slate-600 mt-1">{{ stepDescription }}</p>
      </section>

      <!-- Progress: 6 steps visible as pills -->
      <div class="flex gap-1">
        <div
          v-for="(step, index) in progressSteps"
          :key="step"
          :class="[
            'flex-1 h-2 rounded-full transition-all',
            stepSequence.indexOf(uiState.step) >= index ? 'bg-blue-500' : 'bg-slate-200',
          ]"
          :aria-label="`Paso ${step}`"
        />
      </div>

      <!-- Content Area -->
      <section class="card-surface p-4 space-y-3">
        <!-- Step 1: Presets -->
        <template v-if="uiState.step === 1">
          <PresetSelector
            :presets="profileStore.presetProfiles"
            :selected="uiState.selectedPreset"
            @apply-preset="applyPreset"
          />
        </template>

        <!-- Step 2: Skill Tier -->
        <template v-if="uiState.step === 2">
          <SkillTierSelector
            :model-value="form.skillTier"
            :options="profileStore.skillTierOptions"
            @update:model-value="form.skillTier = $event"
          />
        </template>

        <!-- Step 3: Good Foot -->
        <template v-if="uiState.step === 3 && !isFutbolistaMode">
          <FootSelector
            :model-value="form.piernaBuena"
            @update:model-value="form.piernaBuena = $event"
          />
        </template>

        <!-- Step 4: Availability -->
        <template v-if="uiState.step === 4 && !isFutbolistaMode">
          <AvailabilitySelector
            :model-value="form.disponibilidad"
            :options="profileStore.disponibilidadOpciones"
            @update:model-value="form.disponibilidad = $event"
          />
        </template>

        <!-- Step 5: Numeric Attributes -->
        <template v-if="uiState.step === 5">
          <div class="space-y-3">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
              <AttributeSlider
                v-for="attr in profileStore.skillAttributes"
                :key="attr.key"
                :model-value="form[attr.key]"
                :label="attr.label"
                @update:model-value="form[attr.key] = $event"
              />
            </div>

            <!-- PlayStyle & Age range in smaller space -->
            <div class="pt-2 border-t border-slate-200 space-y-2">
              <PlaystyleSelector
                :model-value="form.playStyle"
                :options="profileStore.playStyleOptions"
                @update:model-value="form.playStyle = $event"
              />
              <AgeRangeSelector
                v-if="!isFutbolistaMode"
                :model-value="form.ageRange"
                :options="profileStore.ageRangeOptions"
                @update:model-value="form.ageRange = $event"
              />
            </div>
          </div>
        </template>

        <!-- Step 6: Confirmation -->
        <template v-if="uiState.step === 6">
          <div class="space-y-3">
            <!-- Nivel y Tendencia Cards -->
            <div class="grid grid-cols-2 gap-2">
              <div class="bg-gradient-to-br from-amber-50 to-amber-100 border border-amber-200 rounded-lg p-3 text-center">
                <p class="text-xs text-amber-700 font-semibold mb-1">Nivel</p>
                <div class="flex items-center justify-center gap-1.5 text-2xl font-bold text-amber-900">
                  <TierIcon :tier="skillTierLabel.value" :size="24" />
                  <span>{{ skillTierLabel.label }}</span>
                </div>
              </div>
              <div class="bg-gradient-to-br from-blue-50 to-blue-100 border border-blue-200 rounded-lg p-3 text-center">
                <p class="text-xs text-blue-700 font-semibold mb-1">Tendencia</p>
                <p :class="['text-2xl font-bold', playTendency.color]">
                  {{ playTendency.label }}
                </p>
              </div>
            </div>

            <!-- Profile Summary -->
            <div class="bg-slate-50 rounded-lg p-3">
              <div class="grid grid-cols-3 gap-2 text-center text-xs">
                <div class="bg-white rounded p-2">
                  <p class="font-semibold text-slate-900">{{ categoriaAtributo(form.shooting) }}</p>
                  <p class="text-slate-600">Disparo</p>
                </div>
                <div class="bg-white rounded p-2">
                  <p class="font-semibold text-slate-900">{{ categoriaAtributo(form.speed) }}</p>
                  <p class="text-slate-600">Velocidad</p>
                </div>
                <div class="bg-white rounded p-2">
                  <p class="font-semibold text-slate-900">{{ categoriaAtributo(form.dribbling) }}</p>
                  <p class="text-slate-600">Regate</p>
                </div>
                <div class="bg-white rounded p-2">
                  <p class="font-semibold text-slate-900">{{ categoriaAtributo(form.defense) }}</p>
                  <p class="text-slate-600">Defensa</p>
                </div>
                <div class="bg-white rounded p-2">
                  <p class="font-semibold text-slate-900">{{ categoriaAtributo(form.strength) }}</p>
                  <p class="text-slate-600">Físico</p>
                </div>
                <div class="bg-white rounded p-2">
                  <p class="font-semibold text-slate-900">{{ categoriaAtributo(form.stamina) }}</p>
                  <p class="text-slate-600">Resistencia</p>
                </div>
                <div class="bg-white rounded p-2">
                  <p class="font-semibold text-slate-900">{{ categoriaAtributo(form.aerial) }}</p>
                  <p class="text-slate-600">Aéreo</p>
                </div>
                <div class="bg-white rounded p-2">
                  <p class="font-semibold text-slate-900">{{ form.posicionPreferida === 'PORTERO' ? 'Portero' : form.posicionPreferida === 'DEFENSA' ? 'Defensa' : form.posicionPreferida === 'DELANTERO' ? 'Delantero' : 'Mediocampista' }}</p>
                  <p class="text-slate-600">Posición preferida</p>
                </div>
                <div class="bg-white rounded p-2">
                  <p class="font-semibold text-slate-900">{{ playStyleLabel }}</p>
                  <p class="text-slate-600">Estilo</p>
                </div>
              </div>
            </div>

            <!-- Details summary -->
            <div v-if="!isFutbolistaMode" class="bg-slate-50 rounded-lg p-3 text-sm space-y-1.5">
              <div class="flex justify-between">
                <span class="text-slate-600">Pierna buena:</span>
                <span class="font-semibold text-slate-900">{{ form.piernaBuena }}</span>
              </div>
              <div class="flex justify-between">
                <span class="text-slate-600">Disponibilidad:</span>
                <span class="font-semibold text-slate-900">
                  {{ form.disponibilidad.length > 0 ? form.disponibilidad.join(', ') : 'No seleccionada' }}
                </span>
              </div>
              <div class="flex justify-between">
                <span class="text-slate-600">Rango de edad:</span>
                <span class="font-semibold text-slate-900">{{ ageRangeLabel }}</span>
              </div>
            </div>

            <p class="text-xs text-slate-600">
              Estos datos se usarán para balancear automáticamente los equipos. Puedes editarlos después desde tu perfil.
            </p>
          </div>
        </template>

        <!-- Errors -->
        <div v-if="uiState.formError" class="bg-red-50 border border-red-200 text-red-700 px-3 py-2 rounded-lg text-sm">
          {{ uiState.formError }}
        </div>
        <div v-if="uiState.successMessage" class="bg-emerald-50 border border-emerald-200 text-emerald-700 px-3 py-2 rounded-lg text-sm">
          {{ uiState.successMessage }}
        </div>
      </section>

      <!-- Navigation buttons -->
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

        <template v-if="uiState.step < 6">
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
