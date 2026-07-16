<script setup>
import { computed } from 'vue'

const props = defineProps({
  variant: {
    type: String,
    default: 'default',
  },
  title: {
    type: String,
    default: '',
  },
  subtitle: {
    type: String,
    default: '',
  },
  padding: {
    type: String,
    default: 'md',
  },
  interactive: {
    type: Boolean,
    default: false,
  },
  bodyClass: {
    type: [String, Array, Object],
    default: '',
  },
  headerClass: {
    type: [String, Array, Object],
    default: '',
  },
  footerClass: {
    type: [String, Array, Object],
    default: '',
  },
})

const hasHeader = computed(() => Boolean(props.title || props.subtitle))
</script>

<template>
  <el-card
    shadow="never"
    :body-style="{ padding: '0' }"
    :class="[
      'app-card',
      `app-card--${variant}`,
      `app-card--padding-${padding}`,
      { 'app-card--interactive': interactive },
    ]"
  >
    <div v-if="hasHeader || $slots.header" :class="['app-card__header', headerClass]">
      <slot name="header" :title="title" :subtitle="subtitle">
        <div class="app-card__title-wrap">
          <h3 v-if="title" class="app-card__title">{{ title }}</h3>
          <!-- <p v-if="subtitle" class="app-card__subtitle">{{ subtitle }}</p> -->
        </div>
      </slot>
    </div>

    <div :class="['app-card__body', bodyClass]">
      <slot />
    </div>

    <div v-if="$slots.footer" :class="['app-card__footer', footerClass]">
      <slot name="footer" />
    </div>
  </el-card>
</template>

<style scoped>
.app-card {
  overflow: hidden;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
  transition:
    transform 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.app-card--soft {
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.app-card--hero {
  background:
    radial-gradient(circle at top right, rgba(51, 112, 255, 0.12), transparent 32%),
    linear-gradient(135deg, rgba(51, 112, 255, 0.08), rgba(51, 112, 255, 0.02)),
    var(--color-surface);
  border-color: rgba(51, 112, 255, 0.12);
}

.app-card--elevated {
  box-shadow: 0 20px 48px rgba(31, 35, 41, 0.09);
}

.app-card--ghost {
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(10px);
}

.app-card--interactive:hover {
  transform: translateY(-2px);
  border-color: rgba(51, 112, 255, 0.22);
  box-shadow: 0 16px 28px rgba(31, 35, 41, 0.08);
}

.app-card__header,
.app-card__body,
.app-card__footer {
  padding-inline: 1.5rem;
}

.app-card--padding-sm .app-card__header,
.app-card--padding-sm .app-card__body,
.app-card--padding-sm .app-card__footer {
  padding-inline: 1rem;
}

.app-card--padding-lg .app-card__header,
.app-card--padding-lg .app-card__body,
.app-card--padding-lg .app-card__footer {
  padding-inline: 2rem;
}

.app-card__header {
  padding-top: 1.35rem;
}

.app-card__body {
  padding-top: 1.35rem;
  padding-bottom: 1.35rem;
}

.app-card__header + .app-card__body {
  padding-top: 0.6rem;
}

.app-card__footer {
  padding-top: 0.25rem;
  padding-bottom: 1.25rem;
}

.app-card__title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--color-text);
}

.app-card__subtitle {
  margin: 0.35rem 0 0;
  color: var(--color-text-muted);
  font-size: 0.92rem;
  line-height: 1.6;
}
</style>
