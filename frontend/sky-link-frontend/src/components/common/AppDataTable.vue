<script setup>
defineProps({
  columns: {
    type: Array,
    required: true,
  },
  rows: {
    type: Array,
    default: () => [],
  },
  rowKey: {
    type: String,
    default: 'id',
  },
  emptyText: {
    type: String,
    default: '暂无数据',
  },
  loading: {
    type: Boolean,
    default: false,
  },
  error: {
    type: String,
    default: '',
  },
})

defineEmits(['retry'])
</script>

<template>
  <div class="app-table">
    <el-skeleton v-if="loading" :rows="4" animated />

    <el-alert
      v-else-if="error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
      class="app-table__error"
    >
      <template #default>
        <button type="button" class="app-table__retry" @click="$emit('retry')">重新加载</button>
      </template>
    </el-alert>

    <table v-else-if="rows.length">
      <thead>
        <tr>
          <th
            v-for="column in columns"
            :key="column.key"
            :style="{ width: column.width || 'auto', textAlign: column.align || 'left' }"
          >
            {{ column.label }}
          </th>
        </tr>
      </thead>

      <tbody>
        <tr v-for="row in rows" :key="row[rowKey]">
          <td
            v-for="column in columns"
            :key="column.key"
            :style="{ textAlign: column.align || 'left' }"
          >
            <slot
              :name="column.slot || column.key"
              :row="row"
              :value="row[column.key]"
              :column="column"
            >
              {{ column.formatter ? column.formatter(row[column.key], row) : row[column.key] }}
            </slot>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-else class="app-table__empty">
      <span>{{ emptyText }}</span>
      <small>调整筛选条件后再试试</small>
    </div>
  </div>
</template>

<style scoped>
.app-table {
  width: 100%;
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  padding: 1rem 0.75rem;
  border-bottom: 1px solid var(--color-border);
  vertical-align: middle;
}

th {
  color: var(--color-text-muted);
  font-size: 0.84rem;
  font-weight: 700;
}

td {
  color: var(--color-text);
  font-size: 0.94rem;
}

tbody tr:hover {
  background: rgba(51, 112, 255, 0.03);
}

.app-table__empty {
  display: grid;
  gap: 0.35rem;
  padding: 2rem 1rem;
  border: 1px dashed var(--color-border);
  border-radius: 16px;
  color: var(--color-text-muted);
  text-align: center;
}

.app-table__empty small {
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.app-table__error {
  margin: 0.25rem 0;
}

.app-table__retry {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--color-primary);
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}
</style>
