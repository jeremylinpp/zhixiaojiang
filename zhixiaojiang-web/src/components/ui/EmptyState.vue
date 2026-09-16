<script setup lang="ts">
import {Database} from 'lucide-vue-next';
defineProps<{title:string;description?:string}>();
</script>
<template><div class="ui-empty" role="status"><span class="icon-tile"><Database aria-hidden="true"/></span><strong>{{title}}</strong><p v-if="description">{{description}}</p><div v-if="$slots.default" class="ui-empty-actions"><slot/></div></div></template>
