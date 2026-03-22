import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginComponent from '../components/LoginComponent.vue'
import DashboardComponent from '../components/DashboardComponent.vue'
import PartidosComponent from '../components/PartidosComponent.vue'
import CrearPartidoComponent from '../components/CrearPartidoComponent.vue'
import DashboardPartidoComponent from '../components/DashboardPartidoComponent.vue'
import AmigosComponent from '../components/AmigosComponent.vue'
import InvitacionesComponent from '../components/InvitacionesComponent.vue'
import PlayerProfileSurveyComponent from '../components/PlayerProfileSurveyComponent.vue'
import MiPerfilComponent from '../components/MiPerfilComponent.vue'
import PerfilPublicoAmigoComponent from '../components/PerfilPublicoAmigoComponent.vue'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: LoginComponent,
    meta: { requiresAuth: false }
  },
  {
    path: '/registro/perfil',
    name: 'PlayerProfileSurvey',
    component: PlayerProfileSurveyComponent,
    meta: { requiresAuth: true }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: DashboardComponent,
    meta: { requiresAuth: true },
    redirect: '/dashboard/partidos',
    children: [
      {
        path: 'partidos',
        name: 'Partidos',
        component: PartidosComponent
      },
      {
        path: 'partidos/:id',
        name: 'DashboardPartido',
        component: DashboardPartidoComponent
      },
      {
        path: 'crear-partido',
        name: 'CrearPartido',
        component: CrearPartidoComponent
      },
      {
        path: 'amigos',
        name: 'Amigos',
        component: AmigosComponent
      },
      {
        path: 'invitaciones',
        name: 'Invitaciones',
        component: InvitacionesComponent
      },
      {
        path: 'mi-perfil',
        name: 'MiPerfil',
        component: MiPerfilComponent
      },
      {
        path: 'amigos/:usuarioId/perfil',
        name: 'PerfilPublicoAmigo',
        component: PerfilPublicoAmigoComponent
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Guard de navegación
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()
  const requiresAuth = to.meta.requiresAuth

  if (requiresAuth && !authStore.isAuthenticated) {
    return next('/login')
  }

  if (!authStore.isAuthenticated) {
    return next()
  }

  if (authStore.hasPlayerProfile === null) {
    await authStore.checkPlayerProfileCompleted()
  }

  if (to.path === '/login') {
    return next(authStore.hasPlayerProfile ? '/dashboard/partidos' : '/registro/perfil')
  }

  if (!authStore.hasPlayerProfile && to.name !== 'PlayerProfileSurvey') {
    return next('/registro/perfil')
  }

  const isEditingSurvey = to.name === 'PlayerProfileSurvey' && to.query?.edit === '1'
  if (authStore.hasPlayerProfile && to.name === 'PlayerProfileSurvey' && !isEditingSurvey) {
    return next('/dashboard/partidos')
  }

  next()
})

export default router
