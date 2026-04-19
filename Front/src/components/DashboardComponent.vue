<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'
import { useNotificationsStore } from '../stores/notifications'
import AppShell from './ui/AppShell.vue'
import AppIcon from './ui/AppIcon.vue'

const router = useRouter()
const authStore = useAuthStore()
const uiStore = useUiStore()
const notificationsStore = useNotificationsStore()
const lastNotificationsFetch = ref(0)
const NOTIFICATIONS_TTL_MS = 30000

const navItems = [
  { name: 'Partidos', to: '/dashboard/partidos', label: 'Partidos', shortLabel: 'Partidos', icon: 'soccer' },
  { name: 'CrearPartido', to: '/dashboard/crear-partido', label: 'Nuevo partido', shortLabel: 'Crear', icon: 'plus' },
  { name: 'Amigos', to: '/dashboard/amigos', label: 'Mis amistades', shortLabel: 'Amistades', icon: 'users' },
  { name: 'Invitaciones', to: '/dashboard/invitaciones', label: 'Invitaciones', shortLabel: 'Inbox', icon: 'bell' },
  { name: 'MiPerfil', to: '/dashboard/mi-perfil', label: 'Mi perfil', shortLabel: 'Perfil', icon: 'user' },
]

const handleLogout = async () => {
  const accepted = await uiStore.askConfirm({
    title: 'Cerrar sesión',
    message: '¿Estás seguro de que deseas cerrar sesión?',
    confirmLabel: 'Cerrar sesión',
    cancelLabel: 'Cancelar',
    variant: 'info',
  })

  if (!accepted) return

  await authStore.logout()
  uiStore.showToast({ message: 'Sesión cerrada', type: 'info' })
  router.push('/login')
}

const irAInvitaciones = () => {
  router.push('/dashboard/invitaciones')
}

const pageTitle = computed(() => {
  const routeName = router.currentRoute.value.name
  if (routeName === 'PerfilPublicoAmigo') {
    const nombre = String(router.currentRoute.value.query?.nombre || '').trim()
    return nombre ? `Perfil de ${nombre}` : 'Perfil de usuario'
  }
  const titles = {
    Partidos: 'Partidos',
    DashboardPartido: 'Detalle del partido',
    CrearPartido: 'Crear partido',
    Amigos: 'Mis amistades',
    Invitaciones: 'Notificaciones',
    MiPerfil: 'Mi perfil',
  }
  return titles[routeName] || 'Dashboard'
})

const pageSubtitle = computed(() => {
  const routeName = router.currentRoute.value.name
  if (routeName === 'PerfilPublicoAmigo') {
    const nombre = String(router.currentRoute.value.query?.nombre || '').trim()
    return nombre ? `Consulta el perfil de ${nombre} en solo lectura` : 'Consulta el perfil de usuario en solo lectura'
  }
  const subtitles = {
    Partidos: 'Gestiona y únete a tus partidos',
    DashboardPartido: 'Consulta y gestiona este partido',
    CrearPartido: 'Configura un nuevo partido',
    Amigos: 'Gestiona tus amistades y contactos',
    Invitaciones: 'Revisa solicitudes y notificaciones pendientes',
    MiPerfil: 'Gestiona tu perfil de jugador',
  }
  return subtitles[routeName] || 'Gestiona tu actividad y partidos.'
})

const breadcrumbs = computed(() => {
  const routeName = router.currentRoute.value.name
  if (routeName === 'Partidos') return []

  const baseLabel = ['DashboardPartido', 'CrearPartido'].includes(routeName) ? 'Partidos' : 'Inicio'
  const baseTo = '/dashboard/partidos'

  return [
    { label: baseLabel, to: baseTo },
    { label: pageTitle.value },
  ]
})

const refreshNotifications = async (force = false) => {
  const now = Date.now()
  if (!force && now - lastNotificationsFetch.value < NOTIFICATIONS_TTL_MS) return
  lastNotificationsFetch.value = now
  await notificationsStore.cargarNotificaciones()
}

onMounted(async () => {
  await refreshNotifications(true)
})

watch(
  () => router.currentRoute.value.fullPath,
  () => {
    // Recargar notificaciones al cambiar de ruta
    refreshNotifications()
  }
)
</script>

<template>
  <AppShell :page-title="pageTitle" :page-subtitle="pageSubtitle" :nav-items="navItems" :breadcrumbs="breadcrumbs" :notifications-count="notificationsStore.totalNotificaciones" :user-initial="authStore.user?.name?.charAt(0)?.toUpperCase() || ''">
    <template #header-right>
      <div class="flex items-center gap-2">
        <!-- Icono de mensajes/notificaciones (solo desktop) -->
        <button
          @click="irAInvitaciones"
          class="hidden md:flex relative items-center justify-center w-11 h-11 rounded-xl active:bg-slate-100 transition"
          aria-label="Ver notificaciones"
        >
          <AppIcon name="bell" :size="20" class="text-slate-700" />
          <span
            v-if="notificationsStore.totalNotificaciones > 0"
            class="absolute -top-1 -right-1 min-w-[20px] h-5 px-1 bg-red-600 text-white text-xs font-bold rounded-full flex items-center justify-center"
          >
            {{ notificationsStore.totalNotificaciones }}
          </span>
        </button>

        <!-- Botón de cerrar sesión -->
        <button
          type="button"
          @click="handleLogout"
          class="inline-flex items-center gap-2 bg-transparent border-0 text-white px-1 py-1 text-sm font-medium hover:text-slate-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[color:var(--color-focus)] rounded-md"
          aria-label="Cerrar sesión"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            class="h-4 w-4"
            aria-hidden="true"
          >
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
            <path d="M16 17l5-5-5-5" />
            <path d="M21 12H9" />
          </svg>
          <span>Cerrar sesión</span>
        </button>
      </div>
    </template>

    <router-view />
  </AppShell>
</template>

