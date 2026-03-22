<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
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
</script>

<template>
  <div class="min-h-dvh bg-slate-50 pb-20 md:pb-0">
    <a
      href="#main-content"
      class="sr-only focus:not-sr-only focus:fixed focus:top-3 focus:left-3 focus:z-[60] focus:bg-white focus:border focus:border-blue-600 focus:text-blue-700 focus:px-3 focus:py-2 focus:rounded-lg"
    >
      Saltar al contenido principal
    </a>

    <header class="sticky top-0 z-50 bg-white border-b border-slate-200 shadow-[0_1px_2px_rgba(15,23,42,0.06)]">
      <div class="max-w-7xl mx-auto px-3 sm:px-6 lg:px-8 h-14 flex items-center justify-between">
        <div class="flex items-center gap-2">
          <div class="w-8 h-8 bg-blue-700 rounded-full text-white flex items-center justify-center">
            <AppIcon name="soccer" :size="16" />
          </div>
          <div>
            <p class="font-semibold text-slate-800 text-sm sm:text-base">SeJuega!</p>
            <p class="hidden sm:block text-[11px] text-slate-500 leading-tight">Cortita y al pie, organiza tu partido en segundos</p>
          </div>
        </div>
        <slot name="header-right" />
      </div>

      <nav class="hidden md:block border-t border-slate-100 bg-blue-700 w-full" aria-label="Navegación principal">
        <div class="px-4 sm:px-6 lg:px-8 py-2 flex gap-0 overflow-x-auto">
          <router-link
            v-for="item in navItems"
            :key="item.name"
            :to="item.to"
            :aria-current="isActive(item) ? 'page' : undefined"
            class="flex-1 h-11 inline-flex items-center gap-2 justify-center text-sm font-medium transition whitespace-nowrap !text-white"
            :class="[
              isActive(item) ? 'bg-blue-600' : 'hover:bg-blue-600',
              'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-300 focus-visible:ring-offset-2'
            ]"
          >
            <AppIcon v-if="item.icon" :name="item.icon" :size="16" class="!text-white" />
            {{ item.label }}
          </router-link>
        </div>
      </nav>
    </header>

    <main id="main-content" class="max-w-7xl mx-auto px-3 sm:px-6 lg:px-8 py-4 md:py-8" tabindex="-1">
      <nav v-if="breadcrumbsToShow.length" class="text-xs text-slate-500 mb-2" aria-label="Breadcrumb">
        <span v-for="(crumb, index) in breadcrumbsToShow" :key="`${crumb.label}-${index}`">
          <router-link v-if="crumb.to" :to="crumb.to" class="hover:text-slate-700">{{ crumb.label }}</router-link>
          <span v-else class="text-slate-700" :aria-current="index === breadcrumbsToShow.length - 1 ? 'page' : undefined">{{ crumb.label }}</span>
          <span v-if="index < breadcrumbsToShow.length - 1" class="mx-1">/</span>
        </span>
      </nav>
      <div class="mb-4 md:mb-8">
        <h1 class="text-xl md:text-3xl font-bold text-slate-800 leading-tight">{{ pageTitle }}</h1>
        <p class="text-xs md:text-base text-slate-600 mt-1" v-if="pageSubtitle">{{ pageSubtitle }}</p>
      </div>
      <slot />
    </main>

    <nav class="fixed md:hidden inset-x-0 bottom-0 border-t border-slate-200 bg-white z-50 shadow-[0_-2px_10px_rgba(15,23,42,0.06)]" aria-label="Navegación móvil">
      <ul class="grid grid-cols-5 h-16">
        <li v-for="item in mobileItems" :key="item.name" class="h-full">
          <router-link
            :to="item.to"
            :aria-current="isActive(item) ? 'page' : undefined"
            class="h-full w-full inline-flex flex-col items-center justify-center text-xs font-medium focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-inset relative transition-all"
            :class="[
              isActive(item) 
                ? 'text-blue-700 bg-blue-50' 
                : 'text-slate-500 active:text-slate-700 active:bg-slate-50'
            ]"
          >
            <!-- Barra indicadora superior para tab activo -->
            <span 
              v-if="isActive(item)"
              class="absolute top-0 left-0 right-0 h-1 bg-blue-700 rounded-b-full"
              aria-hidden="true"
            ></span>
            
            <!-- Círculo con inicial para perfil, emoji para otros -->
            <span v-if="item.name === 'MiPerfil'" class="relative w-7 h-7 rounded-full bg-blue-700 text-white font-semibold flex items-center justify-center text-sm">
              {{ userInitial }}
            </span>
            <span v-else class="relative inline-flex items-center justify-center">
              <AppIcon :name="item.icon" :size="18" />
              <span
                v-if="item.name === 'Invitaciones' && notificationsCount > 0"
                class="absolute -top-1 -right-3 min-w-[18px] h-[18px] px-1 bg-red-600 text-white text-[10px] font-bold rounded-full flex items-center justify-center"
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
