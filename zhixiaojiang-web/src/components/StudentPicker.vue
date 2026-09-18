<script lang="ts" setup>
import {onMounted, ref} from 'vue';
import {request} from '../api';

defineProps<{ modelValue: number; disabled?: boolean }>();
const emit = defineEmits<{ 'update:modelValue': [number] }>();
const query = ref(''), error = ref(''), loading = ref(false);
const students = ref<{ id: number; name: string; studentNo: string }[]>([]);
const total = ref(0);

async function search() {
  loading.value = true;
  error.value = '';
  try {
    const result = await request<{
      items: typeof students.value;
      total: number
    }>(`/students?pageSize=100&q=${encodeURIComponent(query.value.trim())}`);
    students.value = result.items;
    total.value = result.total;
    emit('update:modelValue', 0);
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}

onMounted(search);
</script>
<template>
  <div class="student-picker">
    <form @submit.prevent="search"><input v-model="query" :disabled="disabled" aria-label="筛选学生姓名或学号"
                                          placeholder="姓名或学号"/>
      <button :disabled="loading || disabled" class="button">筛选学生</button>
    </form>
    <label>选择学生<select :disabled="loading || disabled" :value="modelValue"
                           @change="emit('update:modelValue',Number(($event.target as HTMLSelectElement).value))">
      <option :value="0">{{ loading ? '正在读取学生…' : '请选择学生' }}</option>
      <option v-for="student in students" :key="student.id" :value="student.id">{{ student.name }} ·
        {{ student.studentNo }}
      </option>
    </select></label>
    <small v-if="total>100">匹配 {{ total }} 人，仅显示前 100 人，请输入姓名或学号缩小范围。</small>
    <small v-if="!loading && !error && !total">没有符合条件的在籍学生。</small>
    <p v-if="error" role="alert">{{ error }}</p>
  </div>
</template>
<style scoped>
.student-picker {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px
}

.student-picker form, .student-picker label {
  display: flex;
  align-items: center;
  gap: 8px
}

input, select {
  font: inherit;
  background: #fffdf9;
  border: 1px solid var(--border);
  border-radius: 5px;
  padding: 8px;
  max-width: 100%;
  min-width: 0
}

.student-picker small {
  color: var(--muted)
}

.student-picker p {
  color: #b8584c
}

@media (max-width: 600px) {
  .student-picker form, .student-picker label {
    width: 100%
  }

  .student-picker input, .student-picker select {
    flex: 1;
    min-width: 0
  }
}
</style>
