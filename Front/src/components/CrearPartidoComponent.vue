<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { usePartidoStore } from '../stores/partido'
import { useUiStore } from '../stores/ui'
import BaseButton from './ui/BaseButton.vue'
import BaseInput from './ui/BaseInput.vue'

const router = useRouter()
const partidoStore = usePartidoStore()
const uiStore = useUiStore()

// Valores por defecto: Universidad de Jaén, 22:00, día siguiente, 7vs7, privado
const tomorrow = new Date()
tomorrow.setDate(tomorrow.getDate() + 1)

const form = ref({
  fecha: tomorrow.toISOString().slice(0, 10),
  hora: '22:00',
  lugar: 'Universidad de Jaén',
  jugadoresPorEquipo: 7,
  tipo: 'PRIVADO',
  colorEquipoA: 'Blanco',
  colorEquipoB: 'Negro',
})

const loading = ref(false)
const errors = ref({
  fecha: '',
  hora: '',
  lugar: '',
  jugadoresPorEquipo: '',
})

const jugadoresLabel = computed(() => {
  const porEquipo = Number(form.value.jugadoresPorEquipo)
  if (!Number.isInteger(porEquipo) || porEquipo < 1) {
    return 'Jugadores por equipo'
  }

  return `Jugadores por equipo (${porEquipo * 2} en total)`
})

const validateForm = () => {
  errors.value = { fecha: '', hora: '', lugar: '', jugadoresPorEquipo: '' }

  if (!form.value.fecha) errors.value.fecha = 'Selecciona una fecha.'
  if (!form.value.hora) errors.value.hora = 'Selecciona una hora.'
  if (!form.value.lugar?.trim()) errors.value.lugar = 'El lugar es obligatorio.'

  const jugadores = Number(form.value.jugadoresPorEquipo)
  if (!Number.isInteger(jugadores) || jugadores < 1 || jugadores > 11) {
    errors.value.jugadoresPorEquipo = 'Ingresa un número entre 1 y 11.'
  }

  return Object.values(errors.value).every((value) => !value)
}

const handleSubmit = async () => {
  if (!validateForm()) {
    uiStore.showToast({ message: 'Corrige los campos marcados.', type: 'warning' })
    return
  }

  loading.value = true

  try {
    const fechaHora = new Date(`${form.value.fecha}T${form.value.hora}:00`)

    const payload = {
      fecha: fechaHora.toISOString(),
      lugar: form.value.lugar.trim(),
      jugadoresPorEquipo: Number(form.value.jugadoresPorEquipo),
      duracionMinutos: 60,
      tipo: form.value.tipo,
      colorEquipoA: form.value.colorEquipoA,
      colorEquipoB: form.value.colorEquipoB,
    }

    const result = await partidoStore.crearPartido(payload)
    if (!result.success) {
      uiStore.showToast({ message: result.message || 'No se pudo crear el partido.', type: 'error' })
      return
    }

    uiStore.showToast({ message: 'Partido creado correctamente.', type: 'success' })
    // Redirigir a partidos
    router.push('/dashboard/partidos')
  } catch (error) {
    uiStore.showToast({ message: error?.message || 'Error inesperado al crear partido.', type: 'error' })
  } finally {
    loading.value = false
  }
}

const handleCancel = () => {
  router.back()
}
</script>

<template>
  <div class="max-w-2xl mx-auto space-y-3">
    <section class="card-surface p-4 sm:p-5">
      <form class="space-y-3" @submit.prevent="handleSubmit">
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <BaseInput v-model="form.fecha" type="date" label="Fecha" :error="errors.fecha" :disabled="loading" />
          <BaseInput v-model="form.hora" type="time" label="Hora" :error="errors.hora" :disabled="loading" />
        </div>

        <BaseInput
          v-model="form.lugar"
          label="Lugar"
          :error="errors.lugar"
          :disabled="loading"
        />

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <BaseInput
            v-model="form.jugadoresPorEquipo"
            type="number"
            :label="jugadoresLabel"
            :error="errors.jugadoresPorEquipo"
            :disabled="loading"
          />

          <label class="block">
            <span class="block text-xs font-medium text-slate-700 mb-0.5">Tipo de partido</span>
            <select
              v-model="form.tipo"
              :disabled="loading"
              class="w-full h-10 px-4 rounded-xl border border-slate-300 bg-white text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="PRIVADO">Privado (invitación)</option>
              <option value="PUBLICO">Público (abierto)</option>
            </select>
          </label>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <label class="block">
            <span class="block text-xs font-medium text-slate-700 mb-0.5">Color Equipo A</span>
            <select
              v-model="form.colorEquipoA"
              :disabled="loading"
              class="w-full h-10 px-4 rounded-xl border border-slate-300 bg-white text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="Blanco">Blanco</option>
              <option value="Negro">Negro</option>
              <option value="Rojo">Rojo</option>
              <option value="Azul">Azul</option>
              <option value="Verde">Verde</option>
              <option value="Amarillo">Amarillo</option>
              <option value="Naranja">Naranja</option>
              <option value="Morado">Morado</option>
            </select>
          </label>

          <label class="block">
            <span class="block text-xs font-medium text-slate-700 mb-0.5">Color Equipo B</span>
            <select
              v-model="form.colorEquipoB"
              :disabled="loading"
              class="w-full h-10 px-4 rounded-xl border border-slate-300 bg-white text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="Oscuro">Oscuro</option>
              <option value="Blanco">Blanco</option>
              <option value="Negro">Negro</option>
              <option value="Rojo">Rojo</option>
              <option value="Azul">Azul</option>
              <option value="Verde">Verde</option>
              <option value="Amarillo">Amarillo</option>
              <option value="Naranja">Naranja</option>
              <option value="Morado">Morado</option>
            </select>
          </label>
        </div>

        <div class="grid grid-cols-2 gap-3 pt-1">
          <BaseButton variant="secondary" block :disabled="loading" @click="handleCancel">Cancelar</BaseButton>
          <BaseButton variant="primary" block :loading="loading" :disabled="loading">Crear partido</BaseButton>
        </div>
      </form>
    </section>
  </div>
</template>
