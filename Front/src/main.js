import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router/index.js'
import { useAuthStore } from './stores/auth.js'
import './style.css'
import App from './App.vue'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// Reconciliar sesión real de Firebase antes del primer render protegido.
const authStore = useAuthStore()
authStore.initializeSession().finally(() => {
	app.mount('#app')
})
