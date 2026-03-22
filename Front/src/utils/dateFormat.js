export const formatDateTimeEs = (value) => {
  if (!value) return '-'

  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return '-'

  const weekday = new Intl.DateTimeFormat('es-ES', { weekday: 'long' }).format(date)
  const day = new Intl.DateTimeFormat('es-ES', { day: 'numeric' }).format(date)
  const month = new Intl.DateTimeFormat('es-ES', { month: 'long' }).format(date)
  const hour = new Intl.DateTimeFormat('es-ES', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)

  const text = `${weekday} ${day} de ${month} a las ${hour}`
  return text.charAt(0).toUpperCase() + text.slice(1)
}
