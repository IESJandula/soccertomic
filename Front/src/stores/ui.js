import { defineStore } from 'pinia'
import { ref } from 'vue'

let toastIdCounter = 0

export const useUiStore = defineStore('ui', () => {
  const toasts = ref([])
  const confirmState = ref({
    open: false,
    title: '',
    message: '',
    confirmLabel: 'Confirmar',
    cancelLabel: 'Cancelar',
    variant: 'danger',
    resolver: null,
  })

  const showToast = ({ message, type = 'info', duration = 3000 }) => {
    const id = ++toastIdCounter
    toasts.value.push({ id, message, type })

    if (duration > 0) {
      setTimeout(() => {
        removeToast(id)
      }, duration)
    }

    return id
  }

  const removeToast = (id) => {
    toasts.value = toasts.value.filter((toast) => toast.id !== id)
  }

  const askConfirm = ({
    title,
    message,
    confirmLabel = 'Confirmar',
    cancelLabel = 'Cancelar',
    variant = 'danger',
  }) => {
    return new Promise((resolve) => {
      confirmState.value = {
        open: true,
        title,
        message,
        confirmLabel,
        cancelLabel,
        variant,
        resolver: resolve,
      }
    })
  }

  const resolveConfirm = (accepted) => {
    if (typeof confirmState.value.resolver === 'function') {
      confirmState.value.resolver(accepted)
    }

    confirmState.value = {
      open: false,
      title: '',
      message: '',
      confirmLabel: 'Confirmar',
      cancelLabel: 'Cancelar',
      variant: 'danger',
      resolver: null,
    }
  }

  return {
    toasts,
    confirmState,
    showToast,
    removeToast,
    askConfirm,
    resolveConfirm,
  }
})
