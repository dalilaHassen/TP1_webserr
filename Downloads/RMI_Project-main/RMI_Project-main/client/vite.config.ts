import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    fs: {
      allow: ['..']  // ← permet accès aux node_modules hors racine
    }
  }
})