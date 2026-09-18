<script lang="ts" setup>
defineProps<{ items: { label: string; value: string | number; description?: string }[] }>();
</script>
<template>
  <div class="ui-metrics panel">
    <div v-for="item in items" :key="item.label" class="ui-metric">
      <span>{{ item.label }}</span><strong>{{ item.value }}</strong><small
        v-if="item.description">{{ item.description }}</small></div>
  </div>
</template>
