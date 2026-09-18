<script lang="ts" setup>
import {onMounted, ref} from 'vue';
import {request, downloadFile} from '../api';

export type Attachment = { id: number; originalName: string; sizeBytes: number };
const props = defineProps<{ taskId?: number; files?: Attachment[]; teacher?: boolean; disabled?: boolean }>();
const emit = defineEmits<{ change: [ids: number[]]; busy: [value: boolean] }>();
const staged = ref<Attachment[]>([]), pending = ref<File>(), key = ref(''), busy = ref(false), error = ref(''),
    input = ref<HTMLInputElement>();

function setBusy(value: boolean) {
  busy.value = value;
  emit('busy', value);
}

async function load() {
  if (!props.taskId) return;
  staged.value = (await request<{ items: Attachment[] }>(`/student-portal/tasks/${props.taskId}/attachments`)).items;
  emit('change', staged.value.map(f => f.id));
}

function select(event: Event) {
  pending.value = (event.target as HTMLInputElement).files?.[0];
  key.value = crypto.randomUUID();
  error.value = '';
}

async function upload() {
  if (!props.taskId || !pending.value || busy.value || props.disabled) return;
  error.value = '';
  if (pending.value.size > 2 * 1024 * 1024 || pending.value.size === 0) {
    error.value = '请选择非空且不超过 2 MiB 的文件';
    return;
  }
  setBusy(true);
  try {
    const body = new FormData();
    body.append('file', pending.value);
    body.append('requestKey', key.value);
    await request(`/student-portal/tasks/${props.taskId}/attachments`, 'POST', body);
    await load();
    pending.value = undefined;
    if (input.value) input.value.value = '';
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    setBusy(false);
  }
}

async function remove(file: Attachment) {
  if (busy.value || props.disabled) return;
  setBusy(true);
  error.value = '';
  try {
    await request(`/student-portal/attachments/${file.id}`, 'DELETE');
    await load();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    setBusy(false);
  }
}

async function download(file: Attachment) {
  if (busy.value) return;
  setBusy(true);
  error.value = '';
  try {
    await downloadFile(props.teacher ? `/task-attachments/${file.id}` : `/student-portal/attachments/${file.id}`, file.originalName);
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    setBusy(false);
  }
}

onMounted(async () => {
  if (!props.taskId) return;
  setBusy(true);
  try {
    await load();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    setBusy(false);
  }
});
</script>
<template>
  <section aria-label="任务附件" class="task-attachments">
    <template v-if="taskId"><label>上传成果附件<input ref="input" :disabled="busy || disabled || staged.length>=3" accept=".png,.jpg,.jpeg,.pdf"
                                                      type="file"
                                                      @change="select"/></label>
      <p>PNG、JPEG、PDF；每份最大 2 MiB，每次提交最多 3 份。附件提交后不能删除或替换。</p>
      <button :disabled="busy || disabled || !pending || staged.length>=3" class="button" type="button" @click="upload">
        {{ busy ? '处理中…' : '上传所选文件' }}
      </button>
    </template>
    <ul v-if="(taskId?staged:files)?.length">
      <li v-for="file in (taskId?staged:files)" :key="file.id">
        <button :disabled="busy" class="table-action" type="button" @click="download(file)">
          {{ file.originalName }}（{{ Math.ceil(file.sizeBytes / 1024) }} KiB）
        </button>
        <button v-if="taskId" :disabled="busy || disabled" class="button" type="button" @click="remove(file)">移除
        </button>
      </li>
    </ul>
    <p v-if="error" role="alert">{{ error }}</p></section>
</template>
<style scoped>.task-attachments {
  margin: 12px 0
}

.task-attachments label {
  display: grid;
  gap: 8px
}

.task-attachments p {
  font-size: 12px;
  line-height: 1.8
}

.task-attachments input {
  max-width: 100%;
  min-height: 44px
}

.task-attachments ul {
  padding: 0;
  list-style: none
}

.task-attachments li {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  margin: 12px 0
}

.task-attachments .table-action {
  white-space: normal;
  overflow-wrap: anywhere;
  text-align: left;
  min-height: 44px
}</style>
