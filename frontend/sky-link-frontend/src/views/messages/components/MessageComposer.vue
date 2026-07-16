<script setup>
import { computed } from 'vue'
import { ElIcon, ElPopover } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import AppButton from '../../../components/common/AppButton.vue'
import AppInput from '../../../components/common/AppInput.vue'

const props = defineProps({
  activeSession: {
    type: Object,
    default: null,
  },
  draft: {
    type: String,
    default: '',
  },
  emojiOptions: {
    type: Array,
    default: () => [],
  },
  emojiPopoverVisible: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:draft', 'update:emojiPopoverVisible', 'send-text', 'send-emoji'])

const draftProxy = computed({
  get: () => props.draft,
  set: (value) => emit('update:draft', value),
})

const emojiPopoverVisibleProxy = computed({
  get: () => props.emojiPopoverVisible,
  set: (value) => emit('update:emojiPopoverVisible', value),
})
</script>

<template>
  <div class="message-composer">
    <div class="message-composer__input">
      <AppInput
        v-model="draftProxy"
        type="textarea"
        :rows="2"
        resize="none"
        placeholder="输入消息，Ctrl+Enter 发送"
        @keyup.ctrl.enter="emit('send-text')"
      />
    </div>
    <div class="message-composer__actions">
      <ElPopover
        v-model:visible="emojiPopoverVisibleProxy"
        placement="top-end"
        trigger="click"
        :width="280"
      >
        <template #reference>
          <button
            type="button"
            class="message-composer__emoji-button"
            :disabled="!activeSession"
            aria-label="选择 emoji"
          >
            <ElIcon><MagicStick /></ElIcon>
          </button>
        </template>
        <div class="emoji-picker">
          <button
            v-for="emoji in emojiOptions"
            :key="emoji"
            type="button"
            class="emoji-picker__item"
            @click="emit('send-emoji', emoji)"
          >
            {{ emoji }}
          </button>
        </div>
      </ElPopover>
      <AppButton
        variant="primary"
        :disabled="!draft.trim() || !activeSession"
        @click="emit('send-text')"
      >
        发送
      </AppButton>
    </div>
  </div>
</template>

<style scoped>
.message-composer {
  display: flex;
  align-items: end;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  border-top: 1px solid var(--color-border);
}

.message-composer__input {
  flex: 1;
}

.message-composer__actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.message-composer__emoji-button {
  display: inline-grid;
  place-items: center;
  width: 2.5rem;
  height: 2.5rem;
  border: 1px solid var(--color-border);
  border-radius: 0.75rem;
  background: var(--color-surface);
  color: var(--color-text);
  cursor: pointer;
  transition: 0.18s ease;
}

.message-composer__emoji-button:hover:not(:disabled) {
  border-color: rgba(51, 112, 255, 0.28);
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.message-composer__emoji-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.emoji-picker {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 0.35rem;
}

.emoji-picker__item {
  display: grid;
  place-items: center;
  aspect-ratio: 1;
  border: 1px solid transparent;
  border-radius: 0.65rem;
  background: transparent;
  font-size: 1.35rem;
  cursor: pointer;
  transition: 0.18s ease;
}

.emoji-picker__item:hover {
  border-color: rgba(51, 112, 255, 0.18);
  background: var(--color-primary-soft);
}

@media (max-width: 760px) {
  .message-composer {
    align-items: stretch;
    flex-direction: column;
  }

  .message-composer__actions {
    justify-content: flex-end;
  }
}
</style>
