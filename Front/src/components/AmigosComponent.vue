<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'
import amistadService from '../services/amistadService'
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
const loading = ref(true)
const buscandoAmigos = ref('')
const procesando = ref(null)

onMounted(async () => {
  try {
    await cargarAmigos()
    await cargarSolicitudesEnviadas()
    await cargarUltimasPersonas()
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
</script>

<template>
  <div class="space-y-4 md:space-y-6">
    <section class="card-surface p-4 md:p-5">
      <div class="space-y-3">
        <BaseInput
          v-model="buscandoAmigos"
          label="Buscar persona"
          placeholder="Escribe un nombre"
          @update:modelValue="buscarAmigos"
        />

        <div v-if="usuariosDisponibles.length > 0" class="space-y-2">
          <article v-for="usuario in usuariosDisponibles" :key="usuario.id" class="border border-slate-200 rounded-xl p-3 flex items-center justify-between gap-3">
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
        </div>
        <p v-else-if="buscandoAmigos" class="text-sm text-slate-500">No hay usuarios disponibles con ese nombre.</p>
      </div>
    </section>

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
        <h3 class="text-lg font-semibold text-slate-800 inline-flex items-center gap-2"><AppIcon name="check" :size="18" />Amistades confirmadas ({{ amigos.length }})</h3>

        <div v-if="amigos.length === 0" class="state-empty mt-3">
          <p class="text-slate-700 font-medium">Todavía no tienes amistades agregadas.</p>
          <p class="text-caption mt-1">Busca personas arriba para enviar solicitudes.</p>
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