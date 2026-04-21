import { initializeApp, getApp, getApps } from 'firebase/app'
import {
  browserLocalPersistence,
  createUserWithEmailAndPassword,
  getAuth,
  GoogleAuthProvider,
  onAuthStateChanged,
  setPersistence,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut,
  updateProfile,
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

const waitForAuthState = (auth, timeoutMs) => {
  return new Promise((resolve) => {
    let settled = false

    const timeoutId = setTimeout(() => {
      if (settled) return
      settled = true
      resolve(auth.currentUser)
    }, timeoutMs)

    const unsubscribe = onAuthStateChanged(auth, (firebaseUser) => {
      if (settled) return
      settled = true
      clearTimeout(timeoutId)
      unsubscribe()
      resolve(firebaseUser)
    })
  })
}

export const getActiveFirebaseSession = async ({ forceRefresh = true, timeoutMs = 4000 } = {}) => {
  const auth = await getFirebaseAuth()
  const firebaseUser = auth.currentUser || await waitForAuthState(auth, timeoutMs)

  if (!firebaseUser) {
    return null
  }

  const idToken = await firebaseUser.getIdToken(forceRefresh)

  return {
    idToken,
    email: firebaseUser.email || '',
    displayName: firebaseUser.displayName || '',
  }
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

export const loginWithEmailPassword = async (email, password) => {
  const auth = await getFirebaseAuth()
  const credential = await signInWithEmailAndPassword(auth, email, password)
  const idToken = await credential.user.getIdToken(true)

  return {
    idToken,
    email: credential.user.email || email || '',
    displayName: credential.user.displayName || '',
  }
}

export const registerWithEmailPassword = async ({ email, password, displayName }) => {
  const auth = await getFirebaseAuth()
  const normalizedName = String(displayName || '').trim()

  const credential = await createUserWithEmailAndPassword(auth, email, password)

  if (normalizedName) {
    await updateProfile(credential.user, { displayName: normalizedName })
  }

  const idToken = await credential.user.getIdToken(true)

  return {
    idToken,
    email: credential.user.email || email || '',
    displayName: normalizedName || credential.user.displayName || '',
  }
}

export const logoutFirebaseSession = async () => {
  if (getApps().length === 0) {
    return
  }

  await signOut(getAuth(getApp()))
}
