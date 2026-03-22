<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'
import amistadService from '../services/amistadService'
import invitacionService from '../services/invitacionService'
import partidoService from '../services/partidoService'
import apiService from '../services/apiService'
import BaseButton from './ui/BaseButton.vue'
import BaseInput from './ui/BaseInput.vue'
import AppIcon from './ui/AppIcon.vue'

const authStore = useAuthStore()
const uiStore = useUiStore()
const router = useRouter()

const amigos = ref([])
const solicitudesEnviadas = ref([])
const usuariosDisponibles = ref([])
const ultimasPersonas = ref([])
const misEquipos = ref([])
const loading = ref(true)
const buscandoAmigos = ref('')
const procesando = ref(null)
const amistadesDesplegadas = ref(true)
const misEquiposDesplegados = ref(true)
const creandoEquipo = ref(false)
const guardandoEquipo = ref(false)
const eliminandoEquipoId = ref(null)
const nombreEquipoRapido = ref('')
const capacidadEquipoRapido = ref(7)
const miembrosEquipoSeleccionados = ref([])

const mostrarDropdownBusqueda = computed(() => buscandoAmigos.value.trim().length > 0)
const maxMiembrosEquipo = computed(() => Math.max(1, Number(capacidadEquipoRapido.value || 7) - 1))

watch(capacidadEquipoRapido, () => {
  if (miembrosEquipoSeleccionados.value.length > maxMiembrosEquipo.value) {
    miembrosEquipoSeleccionados.value = miembrosEquipoSeleccionados.value.slice(0, maxMiembrosEquipo.value)
  }
})

const toggleAmistades = () => {
  amistadesDesplegadas.value = !amistadesDesplegadas.value
}

const toggleMisEquipos = () => {
  misEquiposDesplegados.value = !misEquiposDesplegados.value
}

onMounted(async () => {
  try {
    await cargarAmigos()
    await cargarSolicitudesEnviadas()
    await cargarUltimasPersonas()
    await cargarMisEquipos()
  } finally {
    loading.value = false
  }
})

const cargarAmigos = async () => {
  amigos.value = await amistadService.obtenerMisAmigos()
}

const cargarSolicitudesEnviadas = async () => {
  solicitudesEnviadas.value = await amistadService.obtenerSolicitudesEnviadas()
}

const cargarUltimasPersonas = async () => {
  try {
    const misPartidos = await partidoService.obtenerMisPartidos()
    const jugadoresSet = new Set()
    const amigoIds = amigos.value.map(a => getOtroUsuario(a).id)
    
    misPartidos.forEach(partido => {
      const todosJugadores = [
        ...(partido.equipoA || []),
        ...(partido.equipoB || []),
        ...(partido.jugadoresInscritos || [])
      ]
      
      todosJugadores.forEach(jugador => {
        if (
          jugador.id !== authStore.user?.id && 
          !amigoIds.includes(jugador.id) &&
          !jugadoresSet.has(jugador.id)
        ) {
          jugadoresSet.add(jugador.id)
          ultimasPersonas.value.push(jugador)
        }
      })
    })
    
    ultimasPersonas.value = ultimasPersonas.value.slice(0, 10)
  } catch (error) {
    console.error('Error cargando últimas personas:', error)
  }
}

const cargarMisEquipos = async () => {
  misEquipos.value = await amistadService.obtenerMisEquipos()
}

const getOtroUsuario = (amistad) => {
  return amistad.usuarioA.id === authStore.user?.id ? amistad.usuarioB : amistad.usuarioA
}

const buscarAmigos = async () => {
  if (!buscandoAmigos.value.trim()) {
    usuariosDisponibles.value = []
    return
  }

  try {
    const todos = await apiService.getUsuariosPublicos()
    const amigoIds = amigos.value.map((a) => getOtroUsuario(a).id)
    const solicitudEnviadaIds = solicitudesEnviadas.value.map((s) => s.usuarioB.id)

    usuariosDisponibles.value = todos.filter(
      (u) =>
        u.id !== authStore.user?.id &&
        !amigoIds.includes(u.id) &&
        !solicitudEnviadaIds.includes(u.id) &&
        u.nombre.toLowerCase().includes(buscandoAmigos.value.toLowerCase())
    )
  } catch (error) {
    uiStore.showToast({ message: error.message || 'Error al buscar usuarios.', type: 'error' })
  }
}

const enviarSolicitud = async (usuarioId) => {
  procesando.value = `enviar-${usuarioId}`
  try {
    await amistadService.enviarSolicitud(usuarioId)
    buscandoAmigos.value = ''
    usuariosDisponibles.value = []
    await cargarSolicitudesEnviadas()
    uiStore.showToast({ message: 'Solicitud enviada.', type: 'success' })
  } catch (error) {
    uiStore.showToast({ message: error.message || 'No se pudo enviar la solicitud.', type: 'error' })
  } finally {
    procesando.value = null
  }
}

const eliminarAmistad = async (amistad) => {
  const usuario = getOtroUsuario(amistad)
  const accepted = await uiStore.askConfirm({
    title: 'Eliminar amistad',
    message: `¿Deseas eliminar a ${usuario.nombre} de tus amigos?`,
    confirmLabel: 'Eliminar',
    cancelLabel: 'Cancelar',
    variant: 'danger',
  })

  if (!accepted) return

  procesando.value = `eliminar-${amistad.id}`
  try {
    await amistadService.eliminarAmistad(amistad.id)
    await cargarAmigos()
    uiStore.showToast({ message: 'Amistad eliminada.', type: 'success' })
  } catch (error) {
    uiStore.showToast({ message: error.message || 'No se pudo eliminar la amistad.', type: 'error' })
  } finally {
    procesando.value = null
  }
}

const verPerfilAmigo = (amistad) => {
  const usuario = getOtroUsuario(amistad)
  router.push({
    path: `/dashboard/amigos/${usuario.id}/perfil`,
    query: { nombre: usuario.nombre || '' },
  })
}

const estaSeleccionadoEnEquipo = (usuarioId) => {
  return miembrosEquipoSeleccionados.value.includes(usuarioId)
}

const puedeSeleccionarEnEquipo = (usuarioId) => {
  return estaSeleccionadoEnEquipo(usuarioId) || miembrosEquipoSeleccionados.value.length < maxMiembrosEquipo.value
}

const toggleMiembroEquipo = (amistad) => {
  const usuario = getOtroUsuario(amistad)
  const usuarioId = usuario.id

  if (estaSeleccionadoEnEquipo(usuarioId)) {
    miembrosEquipoSeleccionados.value = miembrosEquipoSeleccionados.value.filter(id => id !== usuarioId)
    return
  }

  if (miembrosEquipoSeleccionados.value.length >= maxMiembrosEquipo.value) {
    uiStore.showToast({ message: `La capacidad seleccionada permite como maximo ${maxMiembrosEquipo.value} amistades.`, type: 'error' })
    return
  }

  miembrosEquipoSeleccionados.value = [...miembrosEquipoSeleccionados.value, usuarioId]
}

const iniciarCreacionEquipo = () => {
  creandoEquipo.value = true
  capacidadEquipoRapido.value = 7
  miembrosEquipoSeleccionados.value = []
}

const cancelarCreacionEquipo = () => {
  creandoEquipo.value = false
  nombreEquipoRapido.value = ''
  capacidadEquipoRapido.value = 7
  miembrosEquipoSeleccionados.value = []
}

const guardarEquipoRapido = async () => {
  if (!nombreEquipoRapido.value.trim()) {
    uiStore.showToast({ message: 'Indica un nombre para el equipo.', type: 'error' })
    return
  }

  if (miembrosEquipoSeleccionados.value.length === 0) {
    uiStore.showToast({ message: 'Selecciona al menos una amistad para crear el equipo.', type: 'error' })
    return
  }

  guardandoEquipo.value = true
  try {
    const equipoCreado = await amistadService.crearEquipoRapido(nombreEquipoRapido.value.trim(), [], Number(capacidadEquipoRapido.value))

    const invitaciones = miembrosEquipoSeleccionados.value.map((usuarioId) =>
      invitacionService.invitarAEquipo(equipoCreado.id, usuarioId)
    )
    await Promise.allSettled(invitaciones)

    cancelarCreacionEquipo()
    await cargarMisEquipos()
    uiStore.showToast({ message: 'Equipo guardado. Se enviaron invitaciones para unirse al equipo.', type: 'success' })
  } catch (error) {
    uiStore.showToast({ message: error.message || 'No se pudo guardar el equipo rapido.', type: 'error' })
  } finally {
    guardandoEquipo.value = false
  }
}

const eliminarEquipoRapido = async (equipo) => {
  const accepted = await uiStore.askConfirm({
    title: 'Eliminar equipo',
    message: `¿Deseas eliminar ${equipo.nombre}?`,
    confirmLabel: 'Eliminar',
    cancelLabel: 'Cancelar',
    variant: 'danger',
  })

  if (!accepted) return

  eliminandoEquipoId.value = equipo.id
  try {
    await amistadService.eliminarEquipoRapido(equipo.id)
    await cargarMisEquipos()
    uiStore.showToast({ message: 'Equipo eliminado.', type: 'success' })
  } catch (error) {
    uiStore.showToast({ message: error.message || 'No se pudo eliminar el equipo.', type: 'error' })
  } finally {
    eliminandoEquipoId.value = null
  }
}
</script>

<template>
  <div class="space-y-4 md:space-y-6">
    <section v-if="loading" class="state-loading text-sm text-slate-600">
      Cargando...
    </section>

    <template v-else>
      <section v-if="solicitudesEnviadas.length > 0" class="card-surface p-4 md:p-5">
        <h3 class="text-lg font-semibold text-slate-800 inline-flex items-center gap-2"><AppIcon name="send" :size="18" />Solicitudes enviadas ({{ solicitudesEnviadas.length }})</h3>
        <div class="space-y-2 mt-3">
          <article v-for="solicitud in solicitudesEnviadas" :key="solicitud.id" class="border border-slate-200 rounded-xl p-3">
            <p class="font-semibold text-slate-800">{{ solicitud.usuarioB.nombre }}</p>
            <p class="text-xs text-slate-500">Esperando respuesta de {{ solicitud.usuarioB.email }}</p>
          </article>
        </div>
      </section>

      <section class="card-surface p-4 md:p-5">
        <div class="flex items-start justify-between gap-3 flex-wrap">
          <button
            type="button"
            class="inline-flex items-center gap-2 text-lg font-semibold text-slate-800"
            @click="toggleAmistades"
          >
            <AppIcon name="check" :size="18" />
            <span>Mis amistades ({{ amigos.length }})</span>
            <svg
              class="w-4 h-4 text-slate-500 transition-transform"
              :class="amistadesDesplegadas ? 'rotate-180' : ''"
              viewBox="0 0 20 20"
              fill="currentColor"
              aria-hidden="true"
            >
              <path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.168l3.71-3.938a.75.75 0 111.08 1.04l-4.25 4.51a.75.75 0 01-1.08 0l-4.25-4.51a.75.75 0 01.02-1.06z" clip-rule="evenodd"/>
            </svg>
          </button>
          <div class="relative w-full md:w-80">
            <span class="sr-only">Buscar persona</span>
            <AppIcon name="search" :size="16" class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
            <input
              v-model="buscandoAmigos"
              type="text"
              placeholder="Buscar persona"
              class="w-full h-11 pl-10 pr-3 rounded-xl border border-slate-300 bg-white text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-1 focus:border-blue-500"
              @input="buscarAmigos"
            />
            <div
              v-if="mostrarDropdownBusqueda"
              class="absolute top-full mt-1.5 left-0 right-0 z-20 bg-white border border-slate-200 rounded-xl shadow-lg max-h-72 overflow-auto"
            >
              <article
                v-for="usuario in usuariosDisponibles"
                :key="usuario.id"
                class="p-3 border-b last:border-b-0 border-slate-100 flex items-center justify-between gap-3"
              >
                <div class="min-w-0">
                  <p class="font-semibold text-slate-800 truncate">{{ usuario.nombre }}</p>
                  <p class="text-xs text-slate-500 truncate">{{ usuario.email }}</p>
                </div>
                <BaseButton
                  size="sm"
                  :loading="procesando === `enviar-${usuario.id}`"
                  @click="enviarSolicitud(usuario.id)"
                >
                  Enviar solicitud
                </BaseButton>
              </article>
              <p v-if="usuariosDisponibles.length === 0" class="text-sm text-slate-500 p-3">
                No hay usuarios disponibles con ese nombre.
              </p>
            </div>
          </div>
        </div>

        <div v-if="amistadesDesplegadas">
          <div v-if="amigos.length === 0" class="state-empty mt-3">
            <p class="text-slate-700 font-medium">Todavía no tienes amistades agregadas.</p>
            <p class="text-caption mt-1">Usa el buscador para enviar solicitudes.</p>
          </div>

          <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-3 mt-3">
            <article v-for="amistad in amigos" :key="amistad.id" class="border border-slate-200 rounded-xl p-3 flex items-center justify-between gap-3">
              <div class="min-w-0">
                <p class="font-semibold text-slate-800 truncate">{{ getOtroUsuario(amistad).nombre }}</p>
                <p class="text-xs text-slate-500 truncate">{{ getOtroUsuario(amistad).email }}</p>
              </div>
              <div class="flex items-center gap-2">
                <BaseButton
                  size="sm"
                  variant="secondary"
                  @click="verPerfilAmigo(amistad)"
                >
                  Ver perfil
                </BaseButton>
                <BaseButton
                  size="sm"
                  variant="danger"
                  :loading="procesando === `eliminar-${amistad.id}`"
                  @click="eliminarAmistad(amistad)"
                >
                  Eliminar
                </BaseButton>
              </div>
            </article>
          </div>
        </div>
      </section>

      <section class="card-surface p-4 md:p-5 space-y-3">
        <div class="flex items-start justify-between gap-3 flex-wrap">
          <button
            type="button"
            class="inline-flex items-center gap-2 text-lg font-semibold text-slate-800"
            @click="toggleMisEquipos"
          >
            <AppIcon name="users" :size="18" />
            <span>Mis equipos</span>
            <svg
              class="w-4 h-4 text-slate-500 transition-transform"
              :class="misEquiposDesplegados ? 'rotate-180' : ''"
              viewBox="0 0 20 20"
              fill="currentColor"
              aria-hidden="true"
            >
              <path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.168l3.71-3.938a.75.75 0 111.08 1.04l-4.25 4.51a.75.75 0 01-1.08 0l-4.25-4.51a.75.75 0 01.02-1.06z" clip-rule="evenodd"/>
            </svg>
          </button>

          <div class="flex items-center gap-2">
            <span v-if="creandoEquipo" class="text-xs font-semibold text-blue-700 bg-blue-50 border border-blue-200 px-2.5 py-1 rounded-full">
              Seleccionados: {{ miembrosEquipoSeleccionados.length }}/{{ maxMiembrosEquipo }}
            </span>
            <BaseButton
              size="sm"
              variant="secondary"
              :disabled="!misEquiposDesplegados"
              @click="iniciarCreacionEquipo"
            >
              Nuevo equipo
            </BaseButton>
          </div>
        </div>

        <div v-if="misEquiposDesplegados" class="space-y-3">
          <div v-if="creandoEquipo" class="space-y-2">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-2">
              <label class="block">
                <span class="block text-sm font-semibold text-slate-700 mb-1.5">Capacidad del equipo</span>
                <select
                  v-model.number="capacidadEquipoRapido"
                  class="w-full h-12 px-4 rounded-xl border border-slate-300 bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-1 focus:border-blue-500"
                >
                  <option :value="2">2 (tú + 1 amistad)</option>
                  <option :value="3">3 (tú + 2 amistades)</option>
                  <option :value="4">4 (tú + 3 amistades)</option>
                  <option :value="5">5 (tú + 4 amistades)</option>
                  <option :value="6">6 (tú + 5 amistades)</option>
                  <option :value="7">7 (tú + 6 amistades)</option>
                </select>
              </label>
            </div>

            <BaseInput
              v-model="nombreEquipoRapido"
              label="Nombre del equipo"
              placeholder="Ej: Los del jueves"
            />
            <div v-if="amigos.length === 0" class="text-sm text-slate-500">
              Necesitas amistades confirmadas para crear un equipo rapido.
            </div>
            <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-2">
              <button
                v-for="amistad in amigos"
                :key="`equipo-${amistad.id}`"
                type="button"
                class="border rounded-xl p-3 text-left transition"
                :class="estaSeleccionadoEnEquipo(getOtroUsuario(amistad).id)
                  ? 'border-blue-400 bg-blue-50'
                  : (puedeSeleccionarEnEquipo(getOtroUsuario(amistad).id)
                      ? 'border-slate-200 hover:border-slate-300 bg-white'
                      : 'border-slate-200 bg-slate-50 opacity-60 cursor-not-allowed')"
                :disabled="!puedeSeleccionarEnEquipo(getOtroUsuario(amistad).id)"
                @click="toggleMiembroEquipo(amistad)"
              >
                <p class="font-semibold text-slate-800 truncate">{{ getOtroUsuario(amistad).nombre }}</p>
                <p class="text-xs text-slate-500 truncate">{{ getOtroUsuario(amistad).email }}</p>
              </button>
            </div>

            <div class="flex justify-end gap-2">
              <BaseButton
                size="sm"
                variant="secondary"
                @click="cancelarCreacionEquipo"
              >
                Cancelar
              </BaseButton>
              <BaseButton
                size="sm"
                :loading="guardandoEquipo"
                :disabled="guardandoEquipo || amigos.length === 0"
                @click="guardarEquipoRapido"
              >
                Guardar equipo
              </BaseButton>
            </div>
          </div>

          <div v-if="misEquipos.length === 0" class="state-empty">
            <p class="text-slate-700 font-medium">Todavia no guardaste equipos rapidos.</p>
            <p class="text-caption mt-1">Usa "Nuevo equipo" para crear uno con tu lista de amistades.</p>
          </div>

          <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <article v-for="equipo in misEquipos" :key="equipo.id" class="border border-slate-200 rounded-xl p-3 space-y-2">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <p class="font-semibold text-slate-800 truncate">{{ equipo.nombre }}</p>
                  <p class="text-xs text-slate-500">Integrantes: {{ equipo.totalIntegrantes }}/{{ equipo.capacidad || 7 }}</p>
                </div>
                <BaseButton
                  size="sm"
                  variant="danger"
                  :loading="eliminandoEquipoId === equipo.id"
                  @click="eliminarEquipoRapido(equipo)"
                >
                  Eliminar
                </BaseButton>
              </div>

              <div class="text-xs text-slate-600">
                <p class="font-semibold text-slate-700 mb-1">Miembros</p>
                <p>
                  Tú
                  <template v-if="equipo.miembros?.length">,
                    {{ equipo.miembros.map(m => m.nombre).join(', ') }}
                  </template>
                </p>
              </div>
            </article>
          </div>
        </div>
      </section>

      <section v-if="ultimasPersonas.length > 0" class="card-surface p-4 md:p-5">
        <h3 class="text-lg font-semibold text-slate-800 inline-flex items-center gap-2"><AppIcon name="users" :size="18" />Personas recientes ({{ ultimasPersonas.length }})</h3>
        <p class="text-xs text-slate-600 mt-1">Personas con las que has compartido partido (aún no son amistades)</p>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-3 mt-3">
          <article v-for="persona in ultimasPersonas" :key="persona.id" class="border border-slate-200 rounded-xl p-3 flex items-center justify-between gap-3">
            <div class="min-w-0">
              <p class="font-semibold text-slate-800 truncate">{{ persona.nombre }}</p>
              <p class="text-xs text-slate-500 truncate">{{ persona.email }}</p>
            </div>
            <BaseButton
              size="sm"
              :loading="procesando === `enviar-${persona.id}`"
              @click="enviarSolicitud(persona.id)"
            >
              Agregar
            </BaseButton>
          </article>
        </div>
      </section>
    </template>
  </div>
</template>