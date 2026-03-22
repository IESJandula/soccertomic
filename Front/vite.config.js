import fs from 'node:fs'
import path from 'node:path'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
function loadFrontendEnvFile() {
  const envPath = path.resolve(__dirname, '../variables-entorno/frontend.env')
  if (!fs.existsSync(envPath)) return {}

  const content = fs.readFileSync(envPath, 'utf8')
  const env = {}

  for (const rawLine of content.split('\n')) {
    const line = rawLine.trim()
    if (!line || line.startsWith('#')) continue

    const separatorIndex = line.indexOf('=')
    if (separatorIndex <= 0) continue

    const key = line.slice(0, separatorIndex).trim()
    const value = line.slice(separatorIndex + 1).trim()
    env[key] = value
  }

  return env
}

export default defineConfig(({ mode }) => {
  const viteEnv = loadEnv(mode, process.cwd(), '')
  const fileEnv = loadFrontendEnvFile()

  // variables-entorno/frontend.env has priority over local Front/.env* files
  const mergedEnv = {
    ...viteEnv,
    ...fileEnv,
  }

  for (const [key, value] of Object.entries(mergedEnv)) {
    if (key.startsWith('VITE_')) {
      process.env[key] = value
    }
  }

  return {
    plugins: [vue()],
    server: {
      headers: {
        'Cross-Origin-Opener-Policy': 'same-origin-allow-popups',
      },
    },
    preview: {
      headers: {
        'Cross-Origin-Opener-Policy': 'same-origin-allow-popups',
      },
    },
  }
})
