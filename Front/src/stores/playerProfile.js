import { defineStore } from 'pinia'
import { ref } from 'vue'
import playerProfileService from '../services/playerProfileService'

export const usePlayerProfileStore = defineStore('playerProfile', () => {
  const profile = ref(null)
  const loading = ref(false)
  const error = ref('')

  // === ATRIBUTOS NUMÉRICOS (0–5 escala) ===
  const skillAttributes = [
    { key: 'shooting', label: 'Disparo' },
    { key: 'speed', label: 'Velocidad' },
    { key: 'dribbling', label: 'Regate' },
    { key: 'defense', label: 'Defensa' },
    { key: 'strength', label: 'Físico' },
    { key: 'stamina', label: 'Resistencia' },
    { key: 'aerial', label: 'Juego aéreo' },
  ]

  const playStyleOptions = [
    { value: 'O', label: 'Ofensivo' },
    { value: 'D', label: 'Defensivo' },
    { value: 'A', label: 'Adaptable' },
  ]

  const ageRangeOptions = [
    { value: 'UNDER_18', label: 'Menos de 18' },
    { value: '18_25', label: '18 a 25' },
    { value: '25_35', label: '25 a 35' },
    { value: '35_50', label: '35 a 50' },
    { value: 'OVER_50', label: 'Más de 50' },
  ]

  const skillTierOptions = [
    { value: 'BRONCE', label: 'Bronce', description: 'Nivel inicial', modifier: 0 },
    { value: 'PLATA', label: 'Plata', description: 'Nivel intermedio', modifier: 1 },
    { value: 'ORO', label: 'Oro', description: 'Nivel avanzado', modifier: 2 },
    { value: 'DIAMANTE', label: 'Diamante', description: 'Nivel élite', modifier: 3 },
  ]

  const footOptions = [
    { value: 'RIGHT', label: 'Derecha', emoji: 'DE' },
    { value: 'LEFT', label: 'Izquierda', emoji: 'IZ' },
    { value: 'BOTH', label: 'Ambas', emoji: 'AM' },
  ]

  const availabilityOptions = [
    { value: 'WEEKDAY_MORNING', label: 'Entre semana mañana' },
    { value: 'WEEKDAY_AFTERNOON', label: 'Entre semana tarde' },
    { value: 'WEEKDAY_BOTH', label: 'Entre semana (mañana y tarde)' },
    { value: 'WEEKEND_MORNING', label: 'Findes mañana' },
    { value: 'WEEKEND_AFTERNOON', label: 'Findes tarde' },
    { value: 'WHOLE_WEEK_MORNING', label: 'Toda la semana mañana' },
    { value: 'WHOLE_WEEK_AFTERNOON', label: 'Toda la semana tarde' },
    { value: 'ANYTIME', label: 'Siempre disponible' },
  ]

  // === PERFILES PRESET ===
  const presetProfiles = {
    STRIKER: {
      name: 'Delantero',
      description: 'Ataque | Velocidad | Definición',
      attributes: {
        shooting: 5,
        speed: 5,
        dribbling: 4,
        defense: 2,
        strength: 3,
        stamina: 4,
        aerial: 2,
        playStyle: 'O',
        goalkeeper: false,
        skillTier: 'BRONCE',
      },
    },
    MIDFIELDER: {
      name: 'Mediocampista',
      description: 'Distribución | Resistencia | Equilibrio',
      attributes: {
        shooting: 3,
        speed: 4,
        dribbling: 4,
        defense: 3,
        strength: 3,
        stamina: 5,
        aerial: 3,
        playStyle: 'A',
        goalkeeper: false,
        skillTier: 'BRONCE',
      },
    },
    DEFENDER: {
      name: 'Defensa',
      description: 'Marca | Físico | Juego aéreo',
      attributes: {
        shooting: 2,
        speed: 3,
        dribbling: 2,
        defense: 5,
        strength: 5,
        stamina: 4,
        aerial: 4,
        playStyle: 'D',
        goalkeeper: false,
        skillTier: 'BRONCE',
      },
    },
    GOALKEEPER: {
      name: 'Portero',
      description: 'Reflejos | Juego aéreo | Colocación',
      attributes: {
        shooting: 1,
        speed: 2,
        dribbling: 1,
        defense: 4,
        strength: 4,
        stamina: 4,
        aerial: 5,
        playStyle: 'D',
        goalkeeper: true,
        skillTier: 'BRONCE',
      },
    },
  }

  // === LEGACY (compatibilidad) ===
  const posicionesDisponibles = [
    'ARQUERO',
    'DEFENSA',
    'MEDIOCAMPISTA',
    'DELANTERO',
  ]

  const rasgosDisponibles = [
    'LIDER',
    'RAPIDO',
    'TECNICO',
    'SOLIDARIO',
    'RESISTENTE',
    'INTENSO',
    'ESTRATEGICO',
    'DEFINIDOR',
  ]

  const categoriasClub = [
    'AMATEUR',
    'SEMIPRO',
    'PROFESIONAL',
    'UNIVERSITARIO',
    'BARRIAL',
  ]

  const disponibilidadOpciones = [
    'Lunes a viernes | Mañana',
    'Lunes a viernes | Tarde-noche',
    'Lunes a viernes | Cualquier horario',
    'Fin de semana | Mañana',
    'Fin de semana | Tarde-noche',
    'Fin de semana | Cualquier horario',
    'Toda la semana | Mañana',
    'Toda la semana | Tarde-noche',
    'Toda la semana | Cualquier horario'
  ]

  const cargarMiPerfil = async () => {
    loading.value = true
    error.value = ''
    try {
      const data = await playerProfileService.obtenerMiPerfil()
      profile.value = data
      return data
    } catch (err) {
      if (err?.status === 404) {
        profile.value = null
        return null
      }
      error.value = err?.message || 'No se pudo cargar el perfil'
      throw err
    } finally {
      loading.value = false
    }
  }

  const guardarMiPerfil = async (payload) => {
    loading.value = true
    error.value = ''
    try {
      const data = await playerProfileService.guardarMiPerfil(payload)
      profile.value = data
      return { success: true, data }
    } catch (err) {
      error.value = err?.message || 'No se pudo guardar el perfil'
      return { success: false, message: error.value }
    } finally {
      loading.value = false
    }
  }

  // Aplicar perfil preset a un formulario
  const applyPreset = (presetKey) => {
    if (presetProfiles[presetKey]) {
      return { ...presetProfiles[presetKey].attributes }
    }
    return null
  }

  // Calcular "rating global" (0–5) basado en atributos + modificador de skillTier
  const calculateGlobalRating = (attributes) => {
    if (!attributes) return 0
    const skills = [
      attributes.shooting,
      attributes.speed,
      attributes.dribbling,
      attributes.defense,
      attributes.strength,
      attributes.stamina,
      attributes.aerial,
    ].filter(v => typeof v === 'number')
    if (skills.length === 0) return 0
    
    // Calcular promedio base
    const baseRating = skills.reduce((a, b) => a + b, 0) / skills.length
    
    // Añadir modificador según skillTier
    let tierModifier = 0
    const tierMapping = {
      'BRONCE': 0,
      'PLATA': 1,
      'ORO': 2,
      'DIAMANTE': 3
    }
    if (attributes.skillTier && tierMapping[attributes.skillTier] !== undefined) {
      tierModifier = tierMapping[attributes.skillTier]
    }
    
    const finalRating = baseRating + tierModifier
    return Math.round(finalRating * 10) / 10
  }

  return {
    profile,
    loading,
    error,
    skillAttributes,
    playStyleOptions,
    ageRangeOptions,
    skillTierOptions,
    footOptions,
    availabilityOptions,
    presetProfiles,
    posicionesDisponibles,
    rasgosDisponibles,
    categoriasClub,
    disponibilidadOpciones,
    cargarMiPerfil,
    guardarMiPerfil,
    applyPreset,
    calculateGlobalRating,
  }
})
