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
  { key: 'friendUserId', label: '好友用户 ID', placeholder: '例如 1002', required: true },
  { key: 'message', label: '申请附言', placeholder: '介绍一下你自己', type: 'textarea' },
]

const formData = {
  friendUserId: '',
  message: '',
}
</script>

<template>
  <AppFormDialog
    :model-value="modelValue"
    title="添加好友"
    confirm-text="发送申请"
    :fields="fields"
    :form-data="formData"
    @update:model-value="emit('update:modelValue', $event)"
    @submit="emit('submit', $event)"
  />
</template>
