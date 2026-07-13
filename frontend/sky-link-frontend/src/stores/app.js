import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    appName: 'SkyLink',
    initialized: true,
  }),
})
