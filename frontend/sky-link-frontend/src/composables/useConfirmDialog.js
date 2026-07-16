import { createApp, defineComponent, h, ref, getCurrentInstance } from 'vue'

import AppConfirmDialog from '../components/common/AppConfirmDialog.vue'

const DEFAULT_OPTIONS = {
  title: '提示',
  confirmText: '确定',
  cancelText: '取消',
  width: '560px',
  type: 'warning',
  confirmVariant: '',
  hint: '',
}

function resolveOptions(messageOrOptions, title, options = {}) {
  if (
    messageOrOptions &&
    typeof messageOrOptions === 'object' &&
    !Array.isArray(messageOrOptions)
  ) {
    return {
      ...DEFAULT_OPTIONS,
      ...messageOrOptions,
      message: messageOrOptions.message ?? '',
      title: messageOrOptions.title ?? DEFAULT_OPTIONS.title,
    }
  }

  return {
    ...DEFAULT_OPTIONS,
    ...options,
    message: messageOrOptions ?? '',
    title: title ?? DEFAULT_OPTIONS.title,
  }
}

function mountConfirmDialog(config, appContext) {
  return new Promise((resolve, reject) => {
    if (typeof document === 'undefined') {
      reject(new Error('confirm dialog is only available in the browser'))
      return
    }

    const container = document.createElement('div')
    document.body.appendChild(container)

    let app
    const visible = ref(true)
    const action = ref('')

    const cleanup = () => {
      app.unmount()
      container.remove()
    }

    const Host = defineComponent({
      name: 'AppConfirmDialogHost',
      setup() {
        const markAction = (nextAction) => {
          if (!action.value) {
            action.value = nextAction
          }
        }

        const handleClosed = () => {
          const resolvedAction = action.value || 'close'
          if (resolvedAction === 'confirm') {
            resolve(resolvedAction)
          } else {
            reject(resolvedAction)
          }
          cleanup()
        }

        return () =>
          h(AppConfirmDialog, {
            modelValue: visible.value,
            'onUpdate:modelValue': (nextVisible) => {
              visible.value = nextVisible
              if (!nextVisible && !action.value) {
                action.value = 'close'
              }
            },
            title: config.title,
            message: config.message,
            hint: config.hint,
            type: config.type,
            width: config.width,
            confirmText: config.confirmText,
            cancelText: config.cancelText,
            confirmVariant: config.confirmVariant,
            onConfirm: () => {
              markAction('confirm')
            },
            onCancel: () => {
              markAction('cancel')
            },
            onClose: () => {
              markAction('close')
            },
            onClosed: handleClosed,
          })
      },
    })

    app = createApp(Host)

    if (appContext) {
      app._context = appContext
    }

    app.mount(container)
  })
}

export function confirmDialog(messageOrOptions, title, options = {}, appContext = null) {
  const config = resolveOptions(messageOrOptions, title, options)
  return mountConfirmDialog(config, appContext)
}

export function useConfirmDialog() {
  const instance = getCurrentInstance()
  const appContext = instance?.appContext ?? null

  return {
    confirm(messageOrOptions, title, options = {}) {
      return confirmDialog(messageOrOptions, title, options, appContext)
    },
  }
}
