<script setup lang="ts">
defineProps<{modelValue:string;options:readonly {value:string;label:string}[];label:string;disabled?:boolean}>();
const emit=defineEmits<{'update:modelValue':[value:string]}>();
</script>
<template><div class="ui-segmented" role="group" :aria-label="label"><button v-for="option in options" :key="option.value" type="button" :aria-pressed="modelValue===option.value" :disabled="disabled" @click="emit('update:modelValue',option.value)">{{option.label}}</button></div></template>
