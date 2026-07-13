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
})
</script>

<template>
  <div class="app-table">
    <table v-if="rows.length">
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

    <div v-else class="app-table__empty">{{ emptyText }}</div>
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
  padding: 2rem 1rem;
  border: 1px dashed var(--color-border);
  border-radius: 16px;
  color: var(--color-text-muted);
  text-align: center;
}
</style>
