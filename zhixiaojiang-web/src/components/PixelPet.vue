<script lang="ts" setup>
import {ref} from 'vue';

const excited = ref(false);
const eyeX = ref(0);

function follow(event: PointerEvent) {
  const bounds = (event.currentTarget as HTMLElement).getBoundingClientRect();
  eyeX.value = Math.max(-2, Math.min(2, (event.clientX - bounds.left - bounds.width / 2) / 12));
}
</script>

<template>
  <button :aria-pressed="excited" :class="{ excited }" aria-label="和智小匠打个招呼"
          class="pixel-mascot" title="你好呀，一起记录今天的成长！" @click="excited = !excited"
          @pointerleave="eyeX = 0" @pointermove="follow">
    <svg aria-hidden="true" viewBox="0 0 76 56">
      <g class="pet-body">
        <path d="M11 4h54v15h10v9H65v17H11V28H1v-9h10z" fill="#e18b70"/>
        <g :transform="`translate(${eyeX} 0)`">
          <path class="pet-eyes" d="M21 13h5v10h-5zm29 0h5v10h-5z" fill="#221b17"/>
        </g>
        <path d="M17 38h7v10h-7zm13 0h7v10h-7zm13 0h7v10h-7zm13 0h7v10h-7z" fill="#fffaf4"/>
        <path d="M17 48h46v4H17z" fill="#827c72"/>
      </g>
    </svg>
  </button>
</template>

<style scoped>
.pixel-mascot {
  padding: 0;
  background: none;
  border: 0;
  border-radius: 6px;
  cursor: pointer
}

.pixel-mascot svg {
  overflow: visible
}

.pet-body {
  transform-origin: 38px 48px;
  animation: pet-idle 2.8s ease-in-out infinite
}

.pet-eyes {
  transform-box: fill-box;
  transform-origin: center;
  animation: pet-blink 5.2s infinite
}

.pixel-mascot:hover .pet-body, .pixel-mascot:focus-visible .pet-body, .pixel-mascot.excited .pet-body {
  animation: pet-greet .65s ease-in-out infinite
}

@keyframes pet-idle {
  0%, 100% {
    transform: translateY(0)
  }
  50% {
    transform: translateY(-3px)
  }
}

@keyframes pet-blink {
  0%, 43%, 47%, 100% {
    transform: scaleY(1)
  }
  45% {
    transform: scaleY(.12)
  }
}

@keyframes pet-greet {
  0%, 100% {
    transform: translateY(0) rotate(-5deg)
  }
  50% {
    transform: translateY(-7px) rotate(5deg)
  }
}

@media (prefers-reduced-motion: reduce) {
  .pet-body, .pet-eyes {
    animation: none !important
  }

  .pixel-mascot:hover {
    background: #f5e8df
  }
}
</style>
