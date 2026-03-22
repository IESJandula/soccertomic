import { defineStore } from 'pinia'
import { ref } from 'vue'
import partidoService from '../services/partidoService'

export const usePartidoStore = defineStore('partido', () => {
  const partidosPublicos = ref([])
  const misPartidos = ref([])
  const loading = ref(false)
  const error = ref('')

  // Obtener partidos futuros públicos
  const cargarPartidosPublicos = async () => {
    loading.value = true
    error.value = ''
    try {
      const partidos = await partidoService.obtenerPartidosFuturos()
      partidosPublicos.value = partidos.filter(p => p.tipo === 'PUBLICO')
      return partidos
    } catch (err) {
      error.value = err.message || 'Error al cargar partidos públicos'
      return []
    } finally {
      loading.value = false
    }
  }

  // Obtener mis partidos (inscrito)
  const cargarMisPartidos = async () => {
    loading.value = true
    error.value = ''
    try {
      const partidos = await partidoService.obtenerMisPartidos()
      misPartidos.value = partidos
      return partidos
    } catch (err) {
      error.value = err.message || 'Error al cargar mis partidos'
      return []
    } finally {
      loading.value = false
    }
  }

  // Crear nuevo partido
  const crearPartido = async (datos) => {
    loading.value = true
    error.value = ''
    try {
      const partido = await partidoService.crearPartido(datos)
      misPartidos.value.push(partido)
      return { success: true, partido }
    } catch (err) {
      error.value = err.message || 'Error al crear partido'
      return { success: false, message: error.value }
    } finally {
      loading.value = false
    }
  }

  // Cambiar tipo de partido (privado → público)
  const abrirPartidoAlPublico = async (partidoId) => {
    loading.value = true
    error.value = ''
    try {
      const partido = await partidoService.cambiarTipoPartido(partidoId, 'PUBLICO')
      const index = misPartidos.value.findIndex(p => p.id === partidoId)
      if (index !== -1) {
        misPartidos.value[index] = partido
      }
      if (partido?.tipo === 'PUBLICO') {
        const publicIndex = partidosPublicos.value.findIndex(p => p.id === partidoId)
        if (publicIndex === -1) {
          partidosPublicos.value.push(partido)
        } else {
          partidosPublicos.value[publicIndex] = partido
        }
      }
      return { success: true, partido }
    } catch (err) {
      error.value = err.message || 'Error al abrir partido'
      return { success: false, message: error.value }
    } finally {
      loading.value = false
    }
  }

  // Cambiar tipo de partido (público → privado)
  const volverPartidoAPrivado = async (partidoId) => {
    loading.value = true
    error.value = ''
    try {
      const partido = await partidoService.cambiarTipoPartido(partidoId, 'PRIVADO')
      const index = misPartidos.value.findIndex(p => p.id === partidoId)
      if (index !== -1) {
        misPartidos.value[index] = partido
      }
      partidosPublicos.value = partidosPublicos.value.filter(p => p.id !== partidoId)
      return { success: true, partido }
    } catch (err) {
      error.value = err.message || 'Error al volver privado el partido'
      return { success: false, message: error.value }
    } finally {
      loading.value = false
    }
  }

  // Actualizar partido (fecha, hora, lugar)
  const actualizarPartido = async (partidoId, datos) => {
    loading.value = true
    error.value = ''
    try {
      const partido = await partidoService.actualizarPartido(partidoId, datos)
      const index = misPartidos.value.findIndex(p => p.id === partidoId)
      if (index !== -1) {
        misPartidos.value[index] = partido
      }
      return { success: true, partido }
    } catch (err) {
      error.value = err.message || 'Error al actualizar partido'
      return { success: false, message: error.value }
    } finally {
      loading.value = false
    }
  }

  // Limpiar estado
  const limpiar = () => {
    partidosPublicos.value = []
    misPartidos.value = []
    error.value = ''
  }

  return {
    // State
    partidosPublicos,
    misPartidos,
    loading,
    error,

    // Actions
    cargarPartidosPublicos,
    cargarMisPartidos,
    crearPartido,
    abrirPartidoAlPublico,
    volverPartidoAPrivado,
    actualizarPartido,
    limpiar,
  }
})
