<script setup>
defineProps({
  modelValue: {
    type: Boolean,
    required: true,
  },
  title: {
    type: String,
    required: true,
  },
  width: {
    type: String,
    default: '520px',
  },
  destroyOnClose: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['update:modelValue', 'close', 'closed'])

function closeDialog() {
  emit('update:modelValue', false)
  emit('close')
}
</script>

<template>
  <el-dialog
    class="app-dialog"
    modal-class="app-dialog-overlay"
    :model-value="modelValue"
    :title="title"
    :width="width"
    :destroy-on-close="destroyOnClose"
    @close="closeDialog"
    @closed="emit('closed')"
  >
    <slot />

    <template v-if="$slots.footer" #footer>
      <slot name="footer" />
    </template>
  </el-dialog>
</template>

<style>
.app-dialog-overlay {
  background: rgba(15, 23, 42, 0.48);
  backdrop-filter: blur(3px);
}

.app-dialog.el-dialog {
  max-width: calc(100vw - 2rem);
  max-height: min(80dvh, 45rem);
  margin: max(1rem, 7dvh) auto;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid rgba(227, 233, 243, 0.92);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow:
    0 1.5rem 4rem rgba(31, 35, 41, 0.18),
    0 0.25rem 1rem rgba(31, 35, 41, 0.08);
}

.app-dialog .el-dialog__header {
  flex: 0 0 auto;
  min-height: 4rem;
  margin: 0;
  padding: 0.75rem 4.25rem 0.75rem 1.5rem;
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--color-border);
}

.app-dialog .el-dialog__title {
  color: var(--color-text);
  font-size: 1.125rem;
  font-weight: 700;
  line-height: 1.4;
}

.app-dialog .el-dialog__headerbtn {
  top: 0.625rem;
  right: 0.75rem;
  width: 2.75rem;
  height: 2.75rem;
  border-radius: var(--radius-sm);
  transition:
    background-color 0.2s ease,
    color 0.2s ease;
}

.app-dialog .el-dialog__headerbtn:hover {
  background: var(--color-surface-muted);
}

.app-dialog .el-dialog__headerbtn:focus-visible {
  outline: 3px solid rgba(51, 112, 255, 0.2);
  outline-offset: -3px;
}

.app-dialog .el-dialog__close {
  color: var(--color-text-muted);
  font-size: 1.25rem;
}

.app-dialog .el-dialog__headerbtn:hover .el-dialog__close {
  color: var(--color-text);
}

.app-dialog .el-dialog__body {
  flex: 1 1 auto;
  min-height: 0;
  padding: 1.5rem;
  overflow-y: auto;
  overscroll-behavior: contain;
  color: var(--color-text);
  scrollbar-color: #cbd5e1 transparent;
  scrollbar-width: thin;
}

.app-dialog .el-dialog__footer {
  flex: 0 0 auto;
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--color-border);
  background: var(--color-surface-muted);
  background: color-mix(in srgb, var(--color-surface-muted) 52%, var(--color-surface));
}

@media (max-width: 600px) {
  .app-dialog.el-dialog {
    max-height: calc(100dvh - 2rem);
    margin: 1rem auto;
  }

  .app-dialog .el-dialog__header {
    min-height: 3.75rem;
    padding-left: 1.25rem;
  }

  .app-dialog .el-dialog__title {
    font-size: 1rem;
  }

  .app-dialog .el-dialog__body {
    padding: 1.25rem;
  }

  .app-dialog .el-dialog__footer {
    padding: 0.875rem 1.25rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .app-dialog .el-dialog__headerbtn {
    transition: none;
  }
}
</style>
