import { initializeApp, getApp, getApps } from 'firebase/app'
import {
  browserLocalPersistence,
  getAuth,
  GoogleAuthProvider,
  setPersistence,
  signInWithPopup,
  signOut,
} from 'firebase/auth'

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
}

let persistenceConfigured = false

const missingFirebaseConfig = () => {
  const required = [
    'VITE_FIREBASE_API_KEY',
    'VITE_FIREBASE_AUTH_DOMAIN',
    'VITE_FIREBASE_PROJECT_ID',
    'VITE_FIREBASE_APP_ID',
  ]

  return required.filter((key) => !import.meta.env[key])
}

const getFirebaseApp = () => {
  const missing = missingFirebaseConfig()
  if (missing.length > 0) {
    throw new Error(`Faltan variables Firebase: ${missing.join(', ')}`)
  }

  if (getApps().length > 0) {
    return getApp()
  }

  return initializeApp(firebaseConfig)
}

const getFirebaseAuth = async () => {
  const auth = getAuth(getFirebaseApp())
  if (!persistenceConfigured) {
    await setPersistence(auth, browserLocalPersistence)
    persistenceConfigured = true
  }
  return auth
}

export const loginWithGooglePopup = async () => {
  const auth = await getFirebaseAuth()
  const provider = new GoogleAuthProvider()
  provider.setCustomParameters({ prompt: 'select_account' })

  const credential = await signInWithPopup(auth, provider)
  const idToken = await credential.user.getIdToken(true)

  return {
    idToken,
    email: credential.user.email || '',
    displayName: credential.user.displayName || '',
  }
}

export const logoutFirebaseSession = async () => {
  if (getApps().length === 0) {
    return
  }

  await signOut(getAuth(getApp()))
}
