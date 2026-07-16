<script setup>
import { computed } from 'vue'
import AppButton from './AppButton.vue'

const props = defineProps({
  page: {
    type: Number,
    required: true,
  },
  pageSize: {
    type: Number,
    default: 5,
  },
  total: {
    type: Number,
    required: true,
  },
})

const emit = defineEmits(['update:page'])

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))
const total = computed(()=>props.total)
const pages = computed(() =>
  Array.from({ length: totalPages.value }, (_, index) => index + 1),
)

function setPage(page) {
  if (page < 1 || page > totalPages.value || page === props.page) {
    return
  }
  emit('update:page', page)
}
</script>

<template>
  <div class="app-pagination">
    <div class="app-pagination__summary">
      共 {{ total }} 条，第 {{ page }} / {{ totalPages }} 页
    </div>

    <div class="app-pagination__actions">
      <AppButton :disabled="page <= 1" @click="setPage(page - 1)">
        上一页
      </AppButton>

      <AppButton
        v-for="pageNumber in pages"
        :key="pageNumber"
        :active="pageNumber === page"
        :disabled="pageNumber === page"
        @click="setPage(pageNumber)"
      >
        {{ pageNumber }}
      </AppButton>

      <AppButton :disabled="page >= totalPages" @click="setPage(page + 1)">
        下一页
      </AppButton>
    </div>
  </div>
</template>

<style scoped>
.app-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--color-border);
}

.app-pagination__summary {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.app-pagination__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

@media (max-width: 720px) {
  .app-pagination {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
