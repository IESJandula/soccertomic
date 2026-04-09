<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import futbolinLogo from '../../assets/FutbolIn_Icono.png'
import AppIcon from './AppIcon.vue'

const props = defineProps({
  pageTitle: {
    type: String,
    default: '',
  },
  pageSubtitle: {
    type: String,
    default: '',
  },
  navItems: {
    type: Array,
    default: () => [],
  },
  breadcrumbs: {
    type: Array,
    default: () => [],
  },
  notificationsCount: {
    type: Number,
    default: 0,
  },
  userInitial: {
    type: String,
    default: '',
  },
})

const route = useRoute()
const router = useRouter()
const mobileItems = computed(() => props.navItems.slice(0, 5))
const breadcrumbsToShow = computed(() => {
  if (props.breadcrumbs?.length) return props.breadcrumbs
  if (!props.pageTitle || !route.name || route.name === 'Partidos') return []
  return [
    { label: 'Inicio', to: '/dashboard/partidos' },
    { label: props.pageTitle },
  ]
})

const normalizePath = (path) => {
  if (!path) return '/'
  return path.endsWith('/') && path.length > 1 ? path.slice(0, -1) : path
}

const isActive = (item) => {
  const currentPath = normalizePath(route.path)
  const itemPath = normalizePath(item.to)

  if (currentPath === itemPath) {
    return true
  }

  return currentPath.startsWith(`${itemPath}/`)
}

const volverAlMenuPrincipal = () => {
  router.push('/dashboard/partidos')
}
</script>

<template>
  <div class="min-h-dvh bg-[color:var(--color-bg)] pb-20 md:pb-0">
    <a
      href="#main-content"
      class="sr-only focus:not-sr-only focus:fixed focus:top-3 focus:left-3 focus:z-[60] focus:bg-[color:var(--color-surface)] focus:border focus:border-[color:var(--color-border)] focus:text-slate-100 focus:px-3 focus:py-2 focus:rounded-lg"
    >
      Saltar al contenido principal
    </a>

    <header class="sticky top-0 z-50 bg-[color:var(--color-surface)] border-b border-[color:var(--color-border)] shadow-[0_12px_24px_rgba(12,0,5,0.28)]">
      <div class="max-w-[1800px] w-full mx-auto px-4 sm:px-6 lg:px-8 xl:px-10 2xl:px-12 h-16 flex items-center justify-between gap-4">
        <button
          type="button"
          class="flex items-center gap-3 min-w-0 text-left rounded-lg px-1 py-0.5 hover:bg-[color:rgba(151,240,125,0.08)] transition"
          @click="volverAlMenuPrincipal"
          aria-label="Volver al menú principal"
        >
          <div class="w-12 h-12 sm:w-14 sm:h-14 flex items-center justify-center shrink-0">
            <img :src="futbolinLogo" alt="FUTBOLIN" class="w-full h-full object-contain object-[50%_50%] scale-[1.45]" />
          </div>
          <div class="min-w-0">
            <p class="font-extrabold text-base sm:text-lg leading-none tracking-tight flex items-baseline gap-0.5 whitespace-nowrap">
              <span class="text-slate-100">FUTBOL</span><span class="text-[color:var(--color-secondary)]">IN</span>
            </p>
            <p class="hidden sm:block text-[11px] text-slate-300 leading-tight mt-1">Cortita y al pie, organiza tu partido en segundos</p>
          </div>
        </button>
        <slot name="header-right" />
      </div>

      <nav class="hidden md:block border-t border-[color:rgba(151,240,125,0.2)] bg-[color:var(--color-bg-elevated)] w-full" aria-label="Navegación principal">
        <div class="max-w-[1800px] w-full mx-auto px-4 sm:px-6 lg:px-8 xl:px-10 2xl:px-12 py-2 flex gap-0 overflow-x-auto">
          <router-link
            v-for="item in navItems"
            :key="item.name"
            :to="item.to"
            :aria-current="isActive(item) ? 'page' : undefined"
            class="flex-1 h-11 inline-flex items-center gap-2 justify-center text-sm font-medium transition whitespace-nowrap text-slate-100"
            :class="[
              isActive(item) ? 'theme-primary-surface' : 'hover:bg-[color:rgba(151,240,125,0.08)]',
              'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-200 focus-visible:ring-offset-2 focus-visible:ring-offset-[color:var(--color-bg-elevated)]'
            ]"
          >
            <AppIcon v-if="item.icon" :name="item.icon" :size="16" class="text-slate-100" />
            {{ item.label }}
          </router-link>
        </div>
      </nav>
    </header>

    <main id="main-content" class="max-w-[1800px] w-full mx-auto px-4 sm:px-6 lg:px-8 xl:px-10 2xl:px-12 py-4 md:py-8" tabindex="-1">
      <nav v-if="breadcrumbsToShow.length" class="text-xs text-slate-300 mb-2" aria-label="Breadcrumb">
        <span v-for="(crumb, index) in breadcrumbsToShow" :key="`${crumb.label}-${index}`">
          <router-link v-if="crumb.to" :to="crumb.to" class="hover:text-slate-100">{{ crumb.label }}</router-link>
          <span v-else class="text-slate-100" :aria-current="index === breadcrumbsToShow.length - 1 ? 'page' : undefined">{{ crumb.label }}</span>
          <span v-if="index < breadcrumbsToShow.length - 1" class="mx-1">/</span>
        </span>
      </nav>
      <div class="mb-4 md:mb-8">
        <h1 class="text-xl md:text-3xl font-bold text-slate-100 leading-tight">{{ pageTitle }}</h1>
        <p class="text-xs md:text-base text-slate-300 mt-1" v-if="pageSubtitle">{{ pageSubtitle }}</p>
      </div>
      <slot />
    </main>

    <nav class="fixed md:hidden inset-x-0 bottom-0 border-t border-[color:rgba(151,240,125,0.2)] bg-[color:var(--color-surface)] z-50 shadow-[0_-8px_20px_rgba(12,0,5,0.35)]" aria-label="Navegación móvil">
      <ul class="grid grid-cols-5 h-16">
        <li v-for="item in mobileItems" :key="item.name" class="h-full">
          <router-link
            :to="item.to"
            :aria-current="isActive(item) ? 'page' : undefined"
            class="h-full w-full inline-flex flex-col items-center justify-center text-xs font-medium focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-200 focus-visible:ring-inset relative transition-all"
            :class="[
              isActive(item) 
                ? 'theme-primary-surface' 
                : 'text-slate-300 active:text-slate-100 active:bg-[color:rgba(151,240,125,0.08)]'
            ]"
          >
            <span 
              v-if="isActive(item)"
              class="absolute top-0 left-0 right-0 h-1 bg-[color:var(--color-secondary)] rounded-b-full"
              aria-hidden="true"
            ></span>
            <span v-if="item.name === 'MiPerfil'" class="relative w-7 h-7 rounded-full theme-primary-surface font-semibold flex items-center justify-center text-sm">
              {{ userInitial }}
            </span>
            <span v-else class="relative inline-flex items-center justify-center">
              <AppIcon :name="item.icon" :size="18" />
              <span
                v-if="item.name === 'Invitaciones' && notificationsCount > 0"
                class="absolute -top-1 -right-3 min-w-[18px] h-[18px] px-1 theme-danger-surface text-[10px] font-bold rounded-full flex items-center justify-center"
              >
                {{ notificationsCount }}
              </span>
            </span>
            <span class="mt-1" :class="isActive(item) ? 'font-semibold' : 'font-medium'">{{ item.shortLabel || item.label }}</span>
          </router-link>
        </li>
      </ul>
    </nav>
  </div>
</template>
