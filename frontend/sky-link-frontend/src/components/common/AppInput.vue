<script setup>
import { computed, useAttrs } from 'vue'

defineOptions({
  inheritAttrs: false,
})

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: '',
  },
  size: {
    type: String,
    default: 'default',
  },
  type: {
    type: String,
    default: 'text',
  },
})

const emit = defineEmits(['update:modelValue'])
const attrs = useAttrs()

const inputClass = computed(() => [
  'app-input',
  `app-input--${props.size}`,
  {
    'app-input--textarea': props.type === 'textarea',
    'app-input--password': props.type === 'password',
  },
])
</script>

<template>
  <el-input
    :model-value="modelValue"
    :size="size"
    :type="type"
    :class="[attrs.class, inputClass]"
    :style="attrs.style"
    v-bind="attrs"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <template v-if="$slots.prefix" #prefix>
      <slot name="prefix" />
    </template>
    <template v-if="$slots.suffix" #suffix>
      <slot name="suffix" />
    </template>
    <template v-if="$slots.prepend" #prepend>
      <slot name="prepend" />
    </template>
    <template v-if="$slots.append" #append>
      <slot name="append" />
    </template>
  </el-input>
</template>

<style scoped>
.app-input {
  width: 100%;
  margin: 0;
  --el-input-bg-color: var(--color-surface);
  --el-input-border-color: var(--color-border);
  --el-input-hover-border-color: rgba(51, 112, 255, 0.42);
  --el-input-focus-border-color: var(--color-primary);
  --el-input-text-color: var(--color-text);
  --el-input-placeholder-color: #8b95a5;
}

.app-input :deep(.el-input__wrapper),
.app-input :deep(.el-textarea__inner) {
  min-height: 2.75rem;
  padding-right: 0.875rem;
  padding-left: 0.875rem;
  border-radius: 12px;
  background: var(--color-surface);
  box-shadow: 0 0 0 1px var(--color-border) inset;
  transition:
    box-shadow 0.2s ease,
    background-color 0.2s ease;
}

.app-input :deep(.el-input__wrapper:hover),
.app-input :deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px rgba(51, 112, 255, 0.42) inset;
}

.app-input :deep(.el-input__wrapper.is-focus),
.app-input :deep(.el-textarea__inner:focus) {
  box-shadow:
    0 0 0 1px var(--color-primary) inset,
    0 0 0 3px rgba(51, 112, 255, 0.12);
}

.app-input :deep(.el-input__inner),
.app-input :deep(.el-textarea__inner) {
  color: var(--color-text);
  font-size: 0.9375rem;
  line-height: 1.5;
}

.app-input :deep(.el-input__inner::placeholder),
.app-input :deep(.el-textarea__inner::placeholder) {
  color: #8b95a5;
}

.app-input--large :deep(.el-input__wrapper),
.app-input--large :deep(.el-textarea__inner) {
  min-height: 2.875rem;
  border-radius: 14px;
}

.app-input--small :deep(.el-input__wrapper),
.app-input--small :deep(.el-textarea__inner) {
  min-height: 2.25rem;
  border-radius: 10px;
  font-size: 0.875rem;
}

.app-input--textarea :deep(.el-textarea__inner) {
  min-height: 7rem;
  padding: 0.75rem 0.875rem;
  resize: vertical;
  line-height: 1.6;
}

.app-input--password :deep(.el-input__suffix) {
  color: var(--color-text-muted);
}

.app-input.is-disabled :deep(.el-input__wrapper),
.app-input.is-disabled :deep(.el-textarea__inner) {
  background: var(--color-surface-muted);
  box-shadow: 0 0 0 1px var(--color-border) inset;
  cursor: not-allowed;
  opacity: 0.72;
}

@media (max-width: 600px) {
  .app-input :deep(.el-input__inner),
  .app-input :deep(.el-textarea__inner) {
    font-size: 1rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .app-input :deep(.el-input__wrapper),
  .app-input :deep(.el-textarea__inner) {
    transition: none;
  }
}
</style>
