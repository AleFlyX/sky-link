<script setup>
import { computed } from 'vue'
import {
  CircleCheckFilled,
  CircleCloseFilled,
  InfoFilled,
  QuestionFilled,
  WarningFilled,
} from '@element-plus/icons-vue'

import AppButton from './AppButton.vue'
import AppDialog from './AppDialog.vue'

defineOptions({
  inheritAttrs: false,
})

const props = defineProps({
  modelValue: {
    type: Boolean,
    required: true,
  },
  title: {
    type: String,
    default: '提示',
  },
  message: {
    type: [String, Number],
    default: '',
  },
  hint: {
    type: String,
    default: '',
  },
  type: {
    type: String,
    default: 'warning',
  },
  width: {
    type: String,
    default: '560px',
  },
  confirmText: {
    type: String,
    default: '确定',
  },
  cancelText: {
    type: String,
    default: '取消',
  },
  confirmVariant: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel', 'close', 'closed'])

const TYPE_META = {
  info: {
    icon: InfoFilled,
    accent: '#3b82f6',
    accentSoft: '#eef4ff',
    accentBorder: '#d6e4ff',
    buttonVariant: 'primary',
  },
  success: {
    icon: CircleCheckFilled,
    accent: '#16a34a',
    accentSoft: '#eefaf2',
    accentBorder: '#ccefd8',
    buttonVariant: 'success',
  },
  warning: {
    icon: WarningFilled,
    accent: '#d97706',
    accentSoft: '#fff7eb',
    accentBorder: '#f6ddba',
    buttonVariant: 'warning',
  },
  danger: {
    icon: CircleCloseFilled,
    accent: '#dc2626',
    accentSoft: '#fff1f1',
    accentBorder: '#f6cdcd',
    buttonVariant: 'danger',
  },
  default: {
    icon: QuestionFilled,
    accent: '#5b6472',
    accentSoft: '#f3f6fa',
    accentBorder: '#dce4ee',
    buttonVariant: 'primary',
  },
}

const theme = computed(() => TYPE_META[props.type] || TYPE_META.default)

const dialogModel = computed({
  get: () => props.modelValue,
  set: (next) => emit('update:modelValue', next),
})

const dialogStyle = computed(() => ({
  '--app-confirm-accent': theme.value.accent,
  '--app-confirm-accent-soft': theme.value.accentSoft,
  '--app-confirm-accent-border': theme.value.accentBorder,
}))

const iconComponent = computed(() => theme.value.icon)
const confirmVariant = computed(() => props.confirmVariant || theme.value.buttonVariant)

function requestClose() {
  dialogModel.value = false
}

function handleConfirm() {
  emit('confirm')
  requestClose()
}

function handleCancel() {
  emit('cancel')
  requestClose()
}
</script>

<template>
  <AppDialog
    v-model="dialogModel"
    class="app-confirm-dialog"
    :style="dialogStyle"
    :title="title"
    :width="width"
    @close="emit('close')"
    @closed="emit('closed')"
  >
    <div class="app-confirm-dialog__body">
      <div class="app-confirm-dialog__icon" aria-hidden="true">
        <component :is="iconComponent" />
      </div>

      <div class="app-confirm-dialog__content">
        <h3 class="app-confirm-dialog__title">{{ title }}</h3>
        <p class="app-confirm-dialog__message">
          {{ message }}
        </p>
        <p v-if="hint" class="app-confirm-dialog__hint">
          {{ hint }}
        </p>
      </div>
    </div>

    <template #footer>
      <div class="app-confirm-dialog__footer">
        <AppButton class="app-confirm-dialog__cancel" @click="handleCancel">
          {{ cancelText }}
        </AppButton>
        <AppButton
          :variant="confirmVariant"
          class="app-confirm-dialog__confirm"
          @click="handleConfirm"
        >
          {{ confirmText }}
        </AppButton>
      </div>
    </template>
  </AppDialog>
</template>

<style scoped>
.app-confirm-dialog :deep(.el-dialog__header) {
  padding: 0.9rem 1.25rem 0.85rem 1.35rem;
}

.app-confirm-dialog :deep(.el-dialog__body) {
  padding: 1rem 1.35rem 1rem;
}

.app-confirm-dialog__body {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 1rem;
  align-items: start;
}

.app-confirm-dialog__icon {
  display: grid;
  place-items: center;
  width: 3rem;
  height: 3rem;
  border-radius: 0.95rem;
  background: var(--app-confirm-accent-soft);
  color: var(--app-confirm-accent);
  font-size: 1.65rem;
  box-shadow: inset 0 0 0 1px var(--app-confirm-accent-border);
}

.app-confirm-dialog__content {
  min-width: 0;
  padding-top: 0.05rem;
}

.app-confirm-dialog__title {
  margin: 0;
  color: var(--color-text);
  font-size: 1.05rem;
  font-weight: 700;
  line-height: 1.35;
}

.app-confirm-dialog__message {
  margin: 0.45rem 0 0;
  color: var(--color-text-muted);
  font-size: 0.94rem;
  line-height: 1.68;
  white-space: pre-wrap;
  word-break: break-word;
}

.app-confirm-dialog__hint {
  margin: 0.75rem 0 0;
  padding-left: 0.85rem;
  border-left: 3px solid var(--app-confirm-accent-border);
  color: var(--color-text-muted);
  font-size: 0.88rem;
  line-height: 1.6;
}

.app-confirm-dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.65rem;
}

.app-confirm-dialog__cancel,
.app-confirm-dialog__confirm {
  min-width: 6rem;
}

@media (max-width: 600px) {
  .app-confirm-dialog :deep(.el-dialog__body) {
    padding: 0.95rem 1.1rem 0.85rem;
  }

  .app-confirm-dialog__body {
    grid-template-columns: 1fr;
  }

  .app-confirm-dialog__icon {
    width: 2.75rem;
    height: 2.75rem;
    border-radius: 0.9rem;
  }

  .app-confirm-dialog__footer {
    flex-direction: column-reverse;
  }

  .app-confirm-dialog__cancel,
  .app-confirm-dialog__confirm {
    width: 100%;
  }
}
</style>
