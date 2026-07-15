<script setup>
import AppFormDialog from '../../../components/common/AppFormDialog.vue'

defineProps({
  modelValue: {
    type: Boolean,
    required: true,
  },
})

const emit = defineEmits(['update:modelValue', 'submit'])

const fields = [
  { key: 'groupName', label: '群聊名称', required: true },
  { key: 'notice', label: '群公告', type: 'textarea' },
]

const formData = {
  groupName: '',
  notice: '',
}
</script>

<template>
  <AppFormDialog
    :model-value="modelValue"
    title="创建群聊"
    confirm-text="创建群聊"
    :fields="fields"
    :form-data="formData"
    @update:model-value="emit('update:modelValue', $event)"
    @submit="emit('submit', $event)"
  />
</template>
