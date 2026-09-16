<script setup lang="ts">
import {computed} from 'vue';
import {ChevronLeft,ChevronRight,ChevronsLeft,ChevronsRight} from 'lucide-vue-next';
const props=defineProps<{page:number;pageSize:number;total:number;disabled?:boolean}>();
const emit=defineEmits<{change:[page:number];resize:[size:number]}>();
const pages=computed(()=>Math.max(1,Math.ceil(props.total/props.pageSize)));
</script>
<template><nav class="ui-pagination" aria-label="列表分页"><span>总计：<strong>{{total}}</strong></span><label>每页条数<select :value="pageSize" :disabled="disabled" @change="emit('resize',Number(($event.target as HTMLSelectElement).value))"><option v-for="size in [10,20,50,100]" :key="size" :value="size">{{size}}</option></select></label><span>第 {{page}} / {{pages}} 页</span><div class="ui-pagination-buttons"><button class="button" aria-label="第一页" :disabled="disabled || page<=1" @click="emit('change',1)"><ChevronsLeft/></button><button class="button" aria-label="上一页" :disabled="disabled || page<=1" @click="emit('change',page-1)"><ChevronLeft/></button><button class="button" aria-label="下一页" :disabled="disabled || page>=pages" @click="emit('change',page+1)"><ChevronRight/></button><button class="button" aria-label="最后一页" :disabled="disabled || page>=pages" @click="emit('change',pages)"><ChevronsRight/></button></div></nav></template>
