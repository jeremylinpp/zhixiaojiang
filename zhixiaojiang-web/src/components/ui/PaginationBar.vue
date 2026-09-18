<script lang="ts" setup>
import {computed} from 'vue';
import {ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight} from 'lucide-vue-next';

const props = defineProps<{ page: number; pageSize: number; total: number; disabled?: boolean }>();
const emit = defineEmits<{ change: [page: number]; resize: [size: number] }>();
const pages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)));
</script>
<template>
  <nav aria-label="列表分页" class="ui-pagination"><span>总计：<strong>{{ total }}</strong></span><label>每页条数<select
      :disabled="disabled" :value="pageSize"
      @change="emit('resize',Number(($event.target as HTMLSelectElement).value))">
    <option v-for="size in [10,20,50,100]" :key="size" :value="size">{{ size }}</option>
  </select></label><span>第 {{ page }} / {{ pages }} 页</span>
    <div class="ui-pagination-buttons">
      <button :disabled="disabled || page<=1" aria-label="第一页" class="button" @click="emit('change',1)">
        <ChevronsLeft/>
      </button>
      <button :disabled="disabled || page<=1" aria-label="上一页" class="button" @click="emit('change',page-1)">
        <ChevronLeft/>
      </button>
      <button :disabled="disabled || page>=pages" aria-label="下一页" class="button" @click="emit('change',page+1)">
        <ChevronRight/>
      </button>
      <button :disabled="disabled || page>=pages" aria-label="最后一页" class="button" @click="emit('change',pages)">
        <ChevronsRight/>
      </button>
    </div>
  </nav>
</template>
