import { fileURLToPath, URL } from 'node:url'
import { cwd } from 'node:process'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, cwd(), '')
  const elementPlusResolver = ElementPlusResolver({
    importStyle: mode === 'test' ? false : 'css',
  })

  return {
    base: env.BASE_URL || '/',
    plugins: [
      vue(),
      vueDevTools(),
      AutoImport({
        dts: false,
        resolvers: [elementPlusResolver],
      }),
      Components({
        dts: false,
        resolvers: [elementPlusResolver],
      }),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      host:'0.0.0.0',
      port: 5573,
      proxy: {
        '/api/v1': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          // rewrite: (path) => path.replace(/^\/api\/v1/, ''),
        },
        '/ws': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          ws: true,
        },
      },
    },
  }
})
