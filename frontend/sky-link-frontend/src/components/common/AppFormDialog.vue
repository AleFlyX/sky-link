<script setup>
import { computed, reactive, watch } from 'vue'
import AppButton from './AppButton.vue'
import AppDialog from './AppDialog.vue'
import AppGeneratedForm from './AppGeneratedForm.vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    required: true,
  },
  title: {
    type: String,
    required: true,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  formData: {
    type: Object,
    default: () => ({}),
  },
  confirmText: {
    type: String,
    default: '保存',
  },
})

const emit = defineEmits(['update:modelValue', 'submit'])

const localForm = reactive({})

const normalizedFields = computed(() =>
  props.fields.map((field) => ({
    type: 'input',
    placeholder: '',
    ...field,
  })),
)

function syncForm() {
  Object.keys(localForm).forEach((key) => {
    delete localForm[key]
  })

  normalizedFields.value.forEach((field) => {
    localForm[field.key] = props.formData[field.key] ?? field.defaultValue ?? ''
  })
}

watch(
  () => [props.modelValue, props.formData],
  () => {
    if (props.modelValue) {
      syncForm()
    }
  },
  { deep: true, immediate: true },
)

function closeDialog() {
  emit('update:modelValue', false)
}

function updateLocalForm(nextForm) {
  Object.keys(localForm).forEach((key) => {
    delete localForm[key]
  })

  Object.assign(localForm, nextForm)
}

function handleSubmit() {
  emit('submit', { ...localForm })
  closeDialog()
}
</script>

<template>
  <AppDialog :model-value="modelValue" :title="title" @close="closeDialog">
    <AppGeneratedForm
      :model-value="localForm"
      :fields="normalizedFields"
      @update:model-value="updateLocalForm"
    />

    <template #footer>
      <div class="app-form-dialog__footer">
        <AppButton @click="closeDialog">取消</AppButton>
        <AppButton variant="primary" @click="handleSubmit">{{ confirmText }}</AppButton>
      </div>
    </template>
  </AppDialog>
</template>

<style scoped>
.app-form-dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}
</style>
