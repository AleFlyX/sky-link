<script setup>
import { computed } from 'vue'

defineOptions({
  inheritAttrs: false,
})

const props = defineProps({
  variant: {
    type: String,
    default: 'default',
  },
  size: {
    type: String,
    default: 'default',
  },
  nativeType: {
    type: String,
    default: 'button',
  },
  block: {
    type: Boolean,
    default: false,
  },
  active: {
    type: Boolean,
    default: false,
  },
})

const buttonClass = computed(() => [
  'app-button',
  `app-button--${props.variant}`,
  `app-button--${props.size}`,
  {
    'app-button--block': props.block,
    'app-button--active': props.active,
  },
])
</script>

<template>
  <el-button :native-type="nativeType" :class="buttonClass" v-bind="$attrs">
    <slot />
  </el-button>
</template>

<style scoped>
.app-button.el-button {
  --app-button-bg: var(--color-surface);
  --app-button-border: var(--color-border);
  --app-button-text: var(--color-text);
  --app-button-hover-bg: var(--color-primary-soft);
  --app-button-hover-border: rgba(51, 112, 255, 0.26);
  --app-button-hover-text: var(--color-primary);
  --el-button-bg-color: var(--app-button-bg);
  --el-button-border-color: var(--app-button-border);
  --el-button-text-color: var(--app-button-text);
  --el-button-hover-bg-color: var(--app-button-hover-bg);
  --el-button-hover-border-color: var(--app-button-hover-border);
  --el-button-hover-text-color: var(--app-button-hover-text);
  --el-button-active-bg-color: var(--app-button-hover-bg);
  --el-button-active-border-color: var(--app-button-hover-border);
  --el-button-active-text-color: var(--app-button-hover-text);
  --el-button-disabled-bg-color: #f6f8fc;
  --el-button-disabled-border-color: #e6ebf3;
  --el-button-disabled-text-color: #a1a8b3;
  min-width: 38px;
  height: 38px;
  margin: 0;
  padding: 0 0.8rem;
  border-radius: 12px;
  box-shadow: none;
  font-weight: 600;
  transition:
    border-color 0.2s ease,
    color 0.2s ease,
    background 0.2s ease,
    transform 0.2s ease;
}

.app-button.el-button:hover {
  transform: translateY(-1px);
}

.app-button--block.el-button {
  width: 100%;
}

.app-button--large.el-button {
  height: 46px;
  padding: 0 1.15rem;
  border-radius: 14px;
}

.app-button--small.el-button {
  min-width: 32px;
  height: 32px;
  padding: 0 0.65rem;
  border-radius: 10px;
  font-size: 0.84rem;
}

.app-button--active.el-button,
.app-button--active.el-button:hover,
.app-button--default.el-button:hover {
  --el-button-border-color: var(--app-button-hover-border);
  --el-button-bg-color: var(--app-button-hover-bg);
  --el-button-text-color: var(--app-button-hover-text);
}

.app-button--primary.el-button {
  --app-button-bg: var(--color-primary);
  --app-button-border: var(--color-primary);
  --app-button-text: #ffffff;
  --app-button-hover-bg: var(--color-accent);
  --app-button-hover-border: var(--color-accent);
  --app-button-hover-text: #ffffff;
}

.app-button--success.el-button {
  --app-button-bg: #ebf8ef;
  --app-button-border: #bfe7cb;
  --app-button-text: #1c8b4d;
  --app-button-hover-bg: #dbf4e4;
  --app-button-hover-border: #91d3aa;
  --app-button-hover-text: #146b3a;
}

.app-button--warning.el-button {
  --app-button-bg: #fff5e8;
  --app-button-border: #f6d8a9;
  --app-button-text: #b76c08;
  --app-button-hover-bg: #ffedcf;
  --app-button-hover-border: #e9c27a;
  --app-button-hover-text: #955600;
}

.app-button--danger.el-button {
  --app-button-bg: #ffecec;
  --app-button-border: #f2c3c3;
  --app-button-text: #d64545;
  --app-button-hover-bg: #ffdede;
  --app-button-hover-border: #eaa2a2;
  --app-button-hover-text: #be2f2f;
}
</style>
