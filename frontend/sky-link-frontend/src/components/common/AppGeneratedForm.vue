<script setup>
import { computed, useId } from 'vue'
import AppInput from './AppInput.vue'

const props = defineProps({
  modelValue: {
    type: Object,
    required: true,
  },
  fields: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue'])
const formId = useId()

const normalizedFields = computed(() =>
  props.fields.map((field) => ({
    type: 'input',
    placeholder: '',
    ...field,
  })),
)

function updateField(key, value) {
  emit('update:modelValue', {
    ...props.modelValue,
    [key]: value,
  })
}

function getFieldId(field) {
  return `${formId}-${String(field.key).replace(/[^a-zA-Z0-9_-]/g, '-')}`
}
</script>

<template>
  <el-form label-position="top" class="app-generated-form">
    <el-form-item
      v-for="field in normalizedFields"
      :key="field.key"
      :label="field.label"
      :for="getFieldId(field)"
      :required="Boolean(field.required)"
      class="app-generated-form__item"
    >
      <el-select
        v-if="field.type === 'select'"
        :id="getFieldId(field)"
        :model-value="modelValue[field.key]"
        :placeholder="field.placeholder || `请选择${field.label}`"
        :disabled="Boolean(field.disabled)"
        :clearable="Boolean(field.clearable)"
        @update:model-value="updateField(field.key, $event)"
      >
        <el-option
          v-for="option in field.options || []"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>

      <el-date-picker
        v-else-if="field.type === 'datetime'"
        :id="getFieldId(field)"
        :model-value="modelValue[field.key]"
        type="datetime"
        :placeholder="field.placeholder || `请选择${field.label}`"
        :disabled="Boolean(field.disabled)"
        :clearable="field.clearable !== false"
        @update:model-value="updateField(field.key, $event)"
      />

      <AppInput
        v-else
        :id="getFieldId(field)"
        :model-value="modelValue[field.key]"
        :type="field.type === 'textarea' ? 'textarea' : 'text'"
        :rows="field.type === 'textarea' ? 4 : undefined"
        :placeholder="field.placeholder || `请输入${field.label}`"
        :disabled="Boolean(field.disabled)"
        :clearable="Boolean(field.clearable)"
        :maxlength="field.maxlength"
        :show-word-limit="Boolean(field.showWordLimit)"
        @update:model-value="updateField(field.key, $event)"
      />

      <p v-if="field.description" class="app-generated-form__description">
        {{ field.description }}
      </p>
    </el-form-item>
  </el-form>
</template>

<style scoped>
.app-generated-form {
  width: 100%;
}

.app-generated-form__item {
  margin-bottom: 1.25rem;
}

.app-generated-form__item:last-child {
  margin-bottom: 0;
}

.app-generated-form__description {
  margin: 0.5rem 0 0;
  color: var(--color-text-muted);
  font-size: 0.8125rem;
  line-height: 1.5;
}

.app-generated-form :deep(.el-form-item__label) {
  height: auto;
  margin-bottom: 0.5rem;
  padding: 0;
  color: var(--color-text);
  font-size: 0.875rem;
  font-weight: 600;
  line-height: 1.5;
}

.app-generated-form :deep(.el-form-item.is-required .el-form-item__label::before) {
  margin-right: 0.25rem;
}

.app-generated-form :deep(.el-form-item__content) {
  display: block;
  line-height: normal;
}

.app-generated-form :deep(.el-select),
.app-generated-form :deep(.el-date-editor) {
  width: 100%;
}

.app-generated-form :deep(.el-select__wrapper),
.app-generated-form :deep(.el-input__wrapper) {
  min-height: 2.75rem;
  padding-right: 0.875rem;
  padding-left: 0.875rem;
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  box-shadow: 0 0 0 1px var(--color-border) inset;
  transition:
    box-shadow 0.2s ease,
    background-color 0.2s ease;
}

.app-generated-form :deep(.el-select__wrapper:hover),
.app-generated-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(51, 112, 255, 0.42) inset;
}

.app-generated-form :deep(.el-select__wrapper.is-focused),
.app-generated-form :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px var(--color-primary) inset,
    0 0 0 3px rgba(51, 112, 255, 0.12);
}

.app-generated-form :deep(.el-input__inner),
.app-generated-form :deep(.el-select__placeholder),
.app-generated-form :deep(.el-select__selected-item) {
  color: var(--color-text);
  font-size: 0.9375rem;
}

.app-generated-form :deep(.el-input__inner::placeholder),
.app-generated-form :deep(.el-textarea__inner::placeholder),
.app-generated-form :deep(.el-select__placeholder.is-transparent) {
  color: #8b95a5;
}

.app-generated-form :deep(.el-textarea__inner) {
  min-height: 7rem !important;
  padding: 0.75rem 0.875rem;
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  box-shadow: 0 0 0 1px var(--color-border) inset;
  color: var(--color-text);
  font-size: 0.9375rem;
  line-height: 1.6;
  resize: vertical;
  transition: box-shadow 0.2s ease;
}

.app-generated-form :deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px rgba(51, 112, 255, 0.42) inset;
}

.app-generated-form :deep(.el-textarea__inner:focus) {
  box-shadow:
    0 0 0 1px var(--color-primary) inset,
    0 0 0 3px rgba(51, 112, 255, 0.12);
}

.app-generated-form :deep(.is-disabled .el-input__wrapper),
.app-generated-form :deep(.is-disabled.el-select__wrapper),
.app-generated-form :deep(.el-textarea.is-disabled .el-textarea__inner) {
  background: var(--color-surface-muted);
  box-shadow: 0 0 0 1px var(--color-border) inset;
  cursor: not-allowed;
  opacity: 0.72;
}

@media (max-width: 600px) {
  .app-generated-form__item {
    margin-bottom: 1rem;
  }

  .app-generated-form :deep(.el-input__inner),
  .app-generated-form :deep(.el-select__placeholder),
  .app-generated-form :deep(.el-select__selected-item),
  .app-generated-form :deep(.el-textarea__inner) {
    font-size: 1rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .app-generated-form :deep(.el-select__wrapper),
  .app-generated-form :deep(.el-input__wrapper),
  .app-generated-form :deep(.el-textarea__inner) {
    transition: none;
  }
}
</style>
