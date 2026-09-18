<script lang="ts" setup>
defineProps<{
  modelValue: string;
  options: readonly { value: string; label: string }[];
  label: string;
  disabled?: boolean
}>();
const emit = defineEmits<{ 'update:modelValue': [value: string] }>();
</script>
<template>
  <div :aria-label="label" class="ui-segmented" role="group">
    <button v-for="option in options" :key="option.value" :aria-pressed="modelValue===option.value" :disabled="disabled"
            type="button" @click="emit('update:modelValue',option.value)">{{ option.label }}
    </button>
  </div>
</template>
