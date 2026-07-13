<script setup>
import { computed } from 'vue'

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
</script>

<template>
  <el-form label-position="top" class="app-generated-form">
    <el-form-item
      v-for="field in normalizedFields"
      :key="field.key"
      :label="field.label"
      class="app-generated-form__item"
    >
      <el-select
        v-if="field.type === 'select'"
        :model-value="modelValue[field.key]"
        :placeholder="field.placeholder || `请选择${field.label}`"
        @update:model-value="updateField(field.key, $event)"
      >
        <el-option
          v-for="option in field.options || []"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>

      <el-input
        v-else
        :model-value="modelValue[field.key]"
        :type="field.type === 'textarea' ? 'textarea' : 'text'"
        :rows="field.type === 'textarea' ? 4 : undefined"
        :placeholder="field.placeholder || `请输入${field.label}`"
        @update:model-value="updateField(field.key, $event)"
      />
    </el-form-item>
  </el-form>
</template>

<style scoped>
.app-generated-form {
  padding-top: 0.25rem;
}

.app-generated-form__item {
  margin-bottom: 0.9rem;
}
</style>
