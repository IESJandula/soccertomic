<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePartidoStore } from '../stores/partido'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'
import partidoService from '../services/partidoService'
import BaseButton from './ui/BaseButton.vue'
import StatusBadge from './ui/StatusBadge.vue'
import AppIcon from './ui/AppIcon.vue'
import { formatDateTimeEs } from '../utils/dateFormat'

const router = useRouter()
const route = useRoute()
const partidoStore = usePartidoStore()
const uiStore = useUiStore()
const authStore = useAuthStore()

const tabActiva = ref('mis-partidos') // 'mis-partidos' | 'publicos'
const misPartidos = ref([])
const partidosPublicos = ref([])
const loading = ref(false)
const procesando = ref(null)
const filtroPublicos = ref('todos')
const loadError = ref('')

const partidosPublicosFiltrados = computed(() => {
  const now = new Date()
  const sorted = [...partidosPublicos.value].sort((a, b) => new Date(a.fecha) - new Date(b.fecha))

  if (filtroPublicos.value === 'proximos') {
    return sorted.filter((p) => new Date(p.fecha) > now)
  }

  return sorted
})

const cargarMisPartidos = async () => {
  loading.value = true
  loadError.value = ''
  try {
    await partidoStore.cargarMisPartidos()
    misPartidos.value = partidoStore.misPartidos || []

    if (partidoStore.error) {
      loadError.value = partidoStore.error
      misPartidos.value = []
      uiStore.showToast({ message: partidoStore.error, type: 'error' })
    }
  } catch (error) {
    loadError.value = error?.message || 'No se pudo cargar tus partidos.'
    misPartidos.value = []
    uiStore.showToast({ message: loadError.value, type: 'error' })
  } finally {
    loading.value = false
  }
}

const cargarPartidosPublicos = async () => {
  loading.value = true
  loadError.value = ''
  try {
    await partidoStore.cargarPartidosPublicos()
    partidosPublicos.value = partidoStore.partidosPublicos || []

    if (partidoStore.error) {
      loadError.value = partidoStore.error
      partidosPublicos.value = []
      uiStore.showToast({ message: partidoStore.error, type: 'error' })
    }
  } catch (error) {
    loadError.value = error?.message || 'No se pudo cargar partidos públicos.'
    partidosPublicos.value = []
    uiStore.showToast({ message: loadError.value, type: 'error' })
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  if (route.query?.cancelado === '1') {
    uiStore.showToast({ message: 'El partido se canceló correctamente.', type: 'info' })
    const nextQuery = { ...route.query }
    delete nextQuery.cancelado
    router.replace({ path: route.path, query: nextQuery })
  }

  await cargarMisPartidos()
})

const cambiarTab = async (tab) => {
  tabActiva.value = tab
  if (tab === 'publicos' && partidosPublicos.value.length === 0) {
    await cargarPartidosPublicos()
  }
}

const irAPartido = (id) => {
  router.push(`/dashboard/partidos/${id}`)
}

const puedeVerDetalle = (partido) => partido?.estado !== 'CANCELADO'

const irACrearPartido = () => {
  router.push('/dashboard/crear-partido')
}

const formatearFecha = (fecha) => {
  return formatDateTimeEs(fecha)
}

const totalJugadores = (partido) => partido.totalJugadores || 0
const cupoTotal = (partido) => (partido.jugadoresPorEquipo || 0) * 2
const disponible = (partido) => totalJugadores(partido) < cupoTotal(partido)

const esOwner = (partido) => {
  if (!authStore.user?.id) return false
  return partido.owner?.id === authStore.user.id
}

const esOrganizador = (partido) => {
  if (!authStore.user?.id) return false
  return (partido.organizadores || []).some((o) => o.usuario?.id === authStore.user.id)
}

const rolIcono = (partido) => {
  if (esOwner(partido)) return { icon: 'crown', label: 'Responsable principal' }
  if (esOrganizador(partido)) return { icon: 'settings', label: 'Organización' }
  return null
}

const statusLabel = (partido) => {
  if (partido.estado === 'CANCELADO') return 'CANCELADO'
  if (!disponible(partido)) return 'COMPLETO'
  return 'ABIERTO'
}

const colorMap = {
  'Blanco': { bg: 'bg-slate-100', text: 'text-slate-700' },
  'Negro': { bg: 'bg-gray-800', text: 'text-gray-100' },
  'Oscuro': { bg: 'bg-gray-800', text: 'text-gray-100' },
  'Rojo': { bg: 'bg-red-500', text: 'text-white' },
  'Azul': { bg: 'bg-blue-500', text: 'text-white' },
  'Verde': { bg: 'bg-emerald-500', text: 'text-white' },
  'Amarillo': { bg: 'bg-yellow-400', text: 'text-gray-900' },
  'Naranja': { bg: 'bg-orange-500', text: 'text-white' },
  'Morado': { bg: 'bg-purple-500', text: 'text-white' },
}

const getTeamColor = (colorName) => {
  return colorMap[colorName] || colorMap['Blanco']
}

const abrirPublico = async (partido) => {
  procesando.value = `abrir-${partido.id}`
  const result = await partidoStore.abrirPartidoAlPublico(partido.id)
  if (result.success) {
    uiStore.showToast({ message: 'Partido abierto al público.', type: 'success' })
    await cargarMisPartidos()
    if (result.partido?.tipo === 'PUBLICO') {
      const index = partidosPublicos.value.findIndex(p => p.id === result.partido.id)
      if (index === -1) {
        partidosPublicos.value.push(result.partido)
      } else {
        partidosPublicos.value[index] = result.partido
      }
    }
  } else {
    uiStore.showToast({ message: result.message || 'No se pudo actualizar el partido.', type: 'error' })
  }
  procesando.value = null
}

const volverPrivado = async (partido) => {
  procesando.value = `privado-${partido.id}`
  const result = await partidoStore.volverPartidoAPrivado(partido.id)
  if (result.success) {
    uiStore.showToast({ message: 'Partido vuelto a privado.', type: 'success' })
    await cargarMisPartidos()
  } else {
    uiStore.showToast({ message: result.message || 'No se pudo actualizar el partido.', type: 'error' })
  }
  procesando.value = null
}

const eliminarPartido = async (partido) => {
  const accepted = await uiStore.askConfirm({
    title: 'Eliminar partido',
    message: 'Esta acción no se puede deshacer. ¿Deseas continuar?',
    confirmLabel: 'Eliminar',
    cancelLabel: 'Cancelar',
    variant: 'danger',
  })

  if (!accepted) return

  procesando.value = `eliminar-${partido.id}`
  try {
    await partidoService.eliminarPartido(partido.id)
    uiStore.showToast({ message: 'Partido eliminado correctamente.', type: 'success' })
    await cargarMisPartidos()
  } catch (error) {
    uiStore.showToast({ message: error.message || 'Error al eliminar partido.', type: 'error' })
  } finally {
    procesando.value = null
  }
}
</script>

<template>
  <div class="space-y-4 md:space-y-6">
    <!-- Header con tabs -->
    <section class="card-surface p-4 md:p-5">
      <!-- Tabs -->
      <div class="flex gap-1 border-b border-slate-200 -mx-4 px-4 md:mx-0 md:px-0 overflow-x-auto">
        <button
          @click="cambiarTab('mis-partidos')"
          :class="[
            'px-4 py-2.5 text-sm font-medium transition-all border-b-2 rounded-t-lg whitespace-nowrap',
            tabActiva === 'mis-partidos'
              ? 'border-blue-600 text-blue-600 bg-blue-50'
              : 'border-transparent text-slate-600 hover:text-slate-800 hover:bg-slate-50'
          ]"
          :aria-current="tabActiva === 'mis-partidos' ? 'page' : undefined"
        >
            <span class="flex items-center gap-1.5">
            <AppIcon name="soccer" :size="15" />
            <span>Mis partidos</span>
            <span 
              v-if="misPartidos.length > 0" 
              :class="[
                'ml-1.5 px-1.5 py-0.5 text-xs rounded-full',
                tabActiva === 'mis-partidos' ? 'bg-blue-600 text-white' : 'bg-slate-200 text-slate-600'
              ]"
            >
              {{ misPartidos.length }}
            </span>
          </span>
        </button>
        <button
          @click="cambiarTab('publicos')"
          :class="[
            'px-4 py-2.5 text-sm font-medium transition-all border-b-2 rounded-t-lg whitespace-nowrap',
            tabActiva === 'publicos'
              ? 'border-blue-600 text-blue-600 bg-blue-50'
              : 'border-transparent text-slate-600 hover:text-slate-800 hover:bg-slate-50'
          ]"
          :aria-current="tabActiva === 'publicos' ? 'page' : undefined"
        >
          <span class="flex items-center gap-1.5">
            <AppIcon name="users" :size="15" />
            <span>Partidos públicos</span>
            <span 
              v-if="partidosPublicosFiltrados.length > 0 && tabActiva === 'publicos'" 
              :class="[
                'ml-1.5 px-1.5 py-0.5 text-xs rounded-full',
                tabActiva === 'publicos' ? 'bg-blue-600 text-white' : 'bg-slate-200 text-slate-600'
              ]"
            >
              {{ partidosPublicosFiltrados.length }}
            </span>
          </span>
        </button>
      </div>

      <!-- Filtros para partidos públicos -->
      <div v-if="tabActiva === 'publicos'" class="flex gap-2 mt-4">
        <BaseButton :variant="filtroPublicos === 'todos' ? 'primary' : 'secondary'" size="sm" @click="filtroPublicos = 'todos'">Todos</BaseButton>
        <BaseButton :variant="filtroPublicos === 'proximos' ? 'primary' : 'secondary'" size="sm" @click="filtroPublicos = 'proximos'">Próximos</BaseButton>
      </div>
    </section>

    <!-- Vista: Mis Partidos -->
    <section v-if="tabActiva === 'mis-partidos'">
      <section v-if="loading" class="state-loading text-sm text-slate-600">
        Cargando tus partidos...
      </section>

      <section v-else-if="misPartidos.length === 0" class="state-empty">
        <p class="text-slate-700 font-medium">Aún no tienes partidos activos.</p>
        <p class="text-caption mt-1">{{ loadError || 'Puedes unirte a uno público o crear tu propio partido.' }}</p>
        <div class="mt-3 flex flex-wrap gap-2 justify-center">
          <BaseButton variant="secondary" @click="cambiarTab('publicos')">Ver partidos públicos</BaseButton>
          <BaseButton variant="primary" @click="irACrearPartido">Crear partido</BaseButton>
        </div>
      </section>

      <section v-else class="grid grid-cols-1 md:grid-cols-2 gap-3 md:gap-4">
        <article
          v-for="partido in misPartidos"
          :key="partido.id"
          :class="[
            'card-surface overflow-hidden flex flex-col relative',
            partido.estado === 'CANCELADO'
              ? 'cancelled-aura'
              : (esOrganizador(partido) ? 'organizer-pulse' : '')
          ]"
        >
          <div
            v-if="rolIcono(partido)"
            class="role-badge"
            :title="rolIcono(partido).label"
          >
            <AppIcon :name="rolIcono(partido).icon" :size="14" />
          </div>
          <!-- Sección dividida con colores de equipos -->
          <div class="flex h-24">
            <div :class="[getTeamColor(partido.colorEquipoA).bg, getTeamColor(partido.colorEquipoA).text, 'flex-1 flex items-center justify-center font-bold text-lg']">
              {{ partido.colorEquipoA }}
            </div>
            <div class="w-10 shrink-0 flex items-center justify-center gap-0.5 bg-blue-600 text-sm font-extrabold shadow-sm">
              <span class="text-white">V</span>
              <span class="text-white">S</span>
            </div>
            <div :class="[getTeamColor(partido.colorEquipoB).bg, getTeamColor(partido.colorEquipoB).text, 'flex-1 flex items-center justify-center font-bold text-lg']">
              {{ partido.colorEquipoB }}
            </div>
          </div>

          <!-- Información del partido -->
          <div class="p-4 flex flex-col gap-3">
            <div class="flex items-start justify-between gap-2">
              <div>
                <p class="text-sm font-semibold text-slate-800">{{ formatearFecha(partido.fecha) }}</p>
                <p class="text-sm text-slate-600 inline-flex items-center gap-1.5"><AppIcon name="pin" :size="14" />{{ partido.lugar }}</p>
              </div>
              <StatusBadge :status="partido.estado" />
            </div>

            <div class="flex items-center gap-2 text-xs text-slate-600">
              <StatusBadge :status="partido.tipo" />
              <span class="inline-flex items-center gap-1.5"><AppIcon name="users" :size="14" />{{ totalJugadores(partido) }}/{{ cupoTotal(partido) }} personas</span>
            </div>

            <p class="text-sm text-slate-600">Organiza: {{ partido.owner?.nombre || '—' }}</p>

            <div class="grid grid-cols-2 gap-2">
              <BaseButton variant="secondary" block :disabled="!puedeVerDetalle(partido)" @click="irAPartido(partido.id)">
                {{ puedeVerDetalle(partido) ? 'Ver' : 'Cancelado' }}
              </BaseButton>

              <BaseButton
                v-if="esOrganizador(partido) && partido.tipo === 'PRIVADO' && partido.estado !== 'FINALIZADO' && partido.estado !== 'CANCELADO' && totalJugadores(partido) < cupoTotal(partido)"
                variant="primary"
                block
                :loading="procesando === `abrir-${partido.id}`"
                @click="abrirPublico(partido)"
              >
                Abrir público
              </BaseButton>

              <BaseButton
                v-else-if="esOrganizador(partido) && partido.tipo === 'PUBLICO'"
                variant="secondary"
                block
                :loading="procesando === `privado-${partido.id}`"
                @click="volverPrivado(partido)"
              >
                Volver privado
              </BaseButton>

              <BaseButton
                v-else-if="totalJugadores(partido) === 0"
                variant="danger"
                block
                :loading="procesando === `eliminar-${partido.id}`"
                @click="eliminarPartido(partido)"
              >
                Eliminar
              </BaseButton>

              <BaseButton v-else variant="ghost" block disabled>
                —
              </BaseButton>
            </div>
          </div>
        </article>
      </section>
    </section>

    <!-- Vista: Partidos públicos -->
    <section v-if="tabActiva === 'publicos'">
      <section v-if="loading" class="state-loading text-sm text-slate-600">
        Cargando partidos públicos...
      </section>

      <section v-else-if="partidosPublicosFiltrados.length === 0" class="state-empty">
        <p class="text-slate-700 font-medium">No hay partidos públicos para este filtro.</p>
        <p class="text-caption mt-1">Prueba cambiando el filtro o creando un nuevo partido.</p>
        <div class="mt-3">
          <BaseButton variant="secondary" @click="irACrearPartido">Crear partido</BaseButton>
        </div>
      </section>

      <section v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3 md:gap-4">
        <article
          v-for="partido in partidosPublicosFiltrados"
          :key="partido.id"
          :class="[
            'card-surface overflow-hidden flex flex-col relative',
            partido.estado === 'CANCELADO'
              ? 'cancelled-aura'
              : (esOrganizador(partido) ? 'organizer-pulse' : '')
          ]"
        >
          <div
            v-if="rolIcono(partido)"
            class="role-badge"
            :title="rolIcono(partido).label"
          >
            <AppIcon :name="rolIcono(partido).icon" :size="14" />
          </div>
          <!-- Sección dividida con colores de equipos -->
          <div class="flex h-24">
            <div :class="[getTeamColor(partido.colorEquipoA).bg, getTeamColor(partido.colorEquipoA).text, 'flex-1 flex items-center justify-center font-bold text-lg']">
              {{ partido.colorEquipoA }}
            </div>
            <div class="w-10 shrink-0 flex items-center justify-center gap-0.5 bg-blue-600 text-sm font-extrabold shadow-sm">
              <span class="text-white">V</span>
              <span class="text-white">S</span>
            </div>
            <div :class="[getTeamColor(partido.colorEquipoB).bg, getTeamColor(partido.colorEquipoB).text, 'flex-1 flex items-center justify-center font-bold text-lg']">
              {{ partido.colorEquipoB }}
            </div>
          </div>

          <!-- Información del partido -->
          <div class="p-4 flex flex-col gap-3">
            <div class="flex items-start justify-between gap-2">
              <div>
                <p class="text-sm font-semibold text-slate-800">{{ formatearFecha(partido.fecha) }}</p>
                <p class="text-sm text-slate-600 inline-flex items-center gap-1.5"><AppIcon name="pin" :size="14" />{{ partido.lugar }}</p>
              </div>
              <StatusBadge :status="statusLabel(partido)" />
            </div>

            <div class="text-center bg-slate-50 rounded-xl p-3">
              <p class="text-xs text-slate-500 mb-1">Jugadores</p>
              <p class="text-2xl font-bold text-slate-700">{{ totalJugadores(partido) }}/{{ cupoTotal(partido) }}</p>
            </div>

            <p class="text-sm text-slate-600">Organiza: {{ partido.owner?.nombre || '—' }}</p>

            <div class="grid grid-cols-2 gap-2 mt-auto">
              <BaseButton variant="secondary" block :disabled="!puedeVerDetalle(partido)" @click="irAPartido(partido.id)">
                {{ puedeVerDetalle(partido) ? 'Ver' : 'Cancelado' }}
              </BaseButton>
              <BaseButton
                v-if="esOrganizador(partido) && partido.tipo === 'PUBLICO'"
                variant="secondary"
                block
                :loading="procesando === `privado-${partido.id}`"
                @click="volverPrivado(partido)"
              >
                Volver privado
              </BaseButton>
              <BaseButton
                v-else
                :variant="(disponible(partido) && puedeVerDetalle(partido)) ? 'primary' : 'ghost'"
                :disabled="!disponible(partido) || !puedeVerDetalle(partido)"
                block
                @click="irAPartido(partido.id)"
              >
                {{ !puedeVerDetalle(partido) ? 'Cancelado' : (disponible(partido) ? 'Unirme' : 'Completo') }}
              </BaseButton>
            </div>
          </div>
        </article>
      </section>
    </section>
  </div>
</template>

<style scoped>
@keyframes navyPulse {
  0% {
    box-shadow:
      0 0 0 1px rgba(37, 99, 235, 0.35),
      0 0 0 0 rgba(37, 99, 235, 0.22),
      0 8px 20px rgba(37, 99, 235, 0.14);
  }
  60% {
    box-shadow:
      0 0 0 1px rgba(37, 99, 235, 0.3),
      0 0 0 10px rgba(37, 99, 235, 0),
      0 8px 20px rgba(37, 99, 235, 0.08);
  }
  100% {
    box-shadow:
      0 0 0 1px rgba(37, 99, 235, 0.35),
      0 0 0 0 rgba(37, 99, 235, 0),
      0 8px 20px rgba(37, 99, 235, 0.14);
  }
}

@keyframes cancelPulse {
  0% {
    box-shadow:
      0 0 0 1px rgba(220, 38, 38, 0.45),
      0 0 0 0 rgba(220, 38, 38, 0.22),
      0 8px 20px rgba(220, 38, 38, 0.16);
  }
  60% {
    box-shadow:
      0 0 0 1px rgba(220, 38, 38, 0.35),
      0 0 0 10px rgba(220, 38, 38, 0),
      0 8px 20px rgba(220, 38, 38, 0.1);
  }
  100% {
    box-shadow:
      0 0 0 1px rgba(220, 38, 38, 0.45),
      0 0 0 0 rgba(220, 38, 38, 0),
      0 8px 20px rgba(220, 38, 38, 0.16);
  }
}

.organizer-pulse {
  border: 1px solid #2563eb;
  box-shadow:
    0 0 0 1px rgba(37, 99, 235, 0.35),
    0 8px 20px rgba(37, 99, 235, 0.14);
  animation: navyPulse 2.2s ease-in-out infinite;
}

.cancelled-aura {
  border: 1px solid #dc2626;
  box-shadow:
    0 0 0 1px rgba(220, 38, 38, 0.45),
    0 8px 20px rgba(220, 38, 38, 0.16);
  animation: cancelPulse 2.2s ease-in-out infinite;
}

.role-badge {
  position: absolute;
  top: 0.6rem;
  left: 0.6rem;
  background: #1d4ed8;
  color: #fff;
  padding: 0.2rem 0.35rem;
  border-radius: 0.5rem;
  line-height: 1;
  box-shadow: 0 1px 2px rgba(37, 99, 235, 0.2);
}
</style>
