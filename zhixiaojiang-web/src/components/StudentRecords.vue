<script lang="ts" setup>
import PageHeader from './ui/PageHeader.vue';
import StudentAccountPanel from './StudentAccountPanel.vue';
import StudentGrowthReview from './StudentGrowthReview.vue';
import PaginationBar from './ui/PaginationBar.vue';
import EmptyState from './ui/EmptyState.vue';
import {onMounted, ref} from 'vue';
import {request} from '../api';

type Student = { id: number; name: string; studentNo: string; gender: string | null; status: string };
type Detail = { student: Student; timeline: { title: string; detail: string; occurredOn: string; source: string }[] };
const students = ref<Student[]>([]);
const pageSize = ref(20);
const page = ref(1), total = ref(0), query = ref('');
const loading = ref(false), saving = ref(false), error = ref(''), formError = ref('');
const detail = ref<Detail>();
const editor = ref<HTMLDialogElement>();
const mode = ref<'edit' | 'create' | 'archive'>('create');
const draft = ref({id: 0, name: '', studentNo: '', gender: ''});
let generation = 0;

async function load() {
  const current = ++generation;
  loading.value = true;
  error.value = '';
  try {
    const data = await request<{
      items: Student[];
      total: number
    }>(`/students?page=${page.value}&pageSize=${pageSize.value}&q=${encodeURIComponent(query.value.trim())}`);
    if (current !== generation) return;
    students.value = data.items;
    total.value = data.total;
    const lastPage = Math.max(1, Math.ceil(data.total / pageSize.value));
    if (page.value > lastPage) {
      page.value = lastPage;
      await load();
    }
  } catch (e) {
    if (current === generation) error.value = (e as Error).message;
  } finally {
    if (current === generation) loading.value = false;
  }
}

function edit(student?: Student) {
  mode.value = student ? 'edit' : 'create';
  formError.value = '';
  draft.value = student ? {...student, gender: student.gender || ''} : {id: 0, name: '', studentNo: '', gender: ''};
  editor.value?.showModal();
}

function archive(student: Student) {
  mode.value = 'archive';
  formError.value = '';
  draft.value = {...student, gender: student.gender || ''};
  editor.value?.showModal();
}

async function save() {
  if (saving.value) return;
  saving.value = true;
  formError.value = '';
  try {
    if (mode.value === 'archive') await request(`/students/${draft.value.id}/archive`, 'POST');
    else {
      if (!draft.value.name.trim() || !draft.value.studentNo.trim()) throw new Error('请填写姓名和学号');
      await request(mode.value === 'edit' ? `/students/${draft.value.id}` : '/students', mode.value === 'edit' ? 'PUT' : 'POST', {
        name: draft.value.name.trim(),
        studentNo: draft.value.studentNo.trim(),
        gender: draft.value.gender || null
      });
    }
    editor.value?.close();
    detail.value = undefined;
    page.value = 1;
    await load();
  } catch (e) {
    formError.value = (e as Error).message;
  } finally {
    saving.value = false;
  }
}

async function inspect(id: number) {
  error.value = '';
  try {
    detail.value = await request<Detail>(`/students/${id}`);
  } catch (e) {
    error.value = (e as Error).message;
  }
}

function search() {
  page.value = 1;
  load();
}

function turn(next: number) {
  page.value = next;
  detail.value = undefined;
  load();
}

function resize(size: number) {
  pageSize.value = size;
  page.value = 1;
  detail.value = undefined;
  load();
}

onMounted(load);
</script>

<template>
  <section class="page-scroll workspace-page">
    <PageHeader description="维护学生基本资料，查看已录入的成长记录。" eyebrow="班主任工作台" title="学生档案">
      <button class="button primary" @click="edit()">新增学生</button>
    </PageHeader>
    <form class="workspace-toolbar" @submit.prevent="search"><label class="inline-search"><input v-model="query"
                                                                                                 aria-label="按姓名或学号搜索"
                                                                                                 placeholder="输入姓名或学号"/></label>
      <button :disabled="loading" class="button">搜索</button>
      <span class="result-count">共 {{ total }} 名在籍学生</span></form>
    <p v-if="error" class="record-error" role="alert">{{ error }}
      <button class="button" @click="load">重试</button>
    </p>
    <p v-if="loading" role="status">正在读取档案…</p>
    <section v-else-if="!error" class="data-card panel">
      <table>
        <thead>
        <tr>
          <th>姓名</th>
          <th>学号</th>
          <th>性别</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="student in students" :key="student.id">
          <td>{{ student.name }}</td>
          <td>{{ student.studentNo }}</td>
          <td>{{ student.gender || '未填写' }}</td>
          <td>在籍</td>
          <td class="record-actions">
            <button class="table-action" @click="inspect(student.id)">查看详情</button>
            <button class="table-action" @click="edit(student)">编辑</button>
            <button class="table-action" @click="archive(student)">归档</button>
          </td>
        </tr>
        <tr v-if="!students.length">
          <td class="empty-cell" colspan="5">
            <EmptyState :description="query.trim() ? '请修改姓名或学号后重新搜索。' : '点击新增学生，开始建立班级档案。'"
                        :title="query.trim() ? '没有符合条件的学生' : '暂无学生档案'"/>
          </td>
        </tr>
        </tbody>
      </table>
    </section>
    <PaginationBar :disabled="loading || !!error" :page="page" :page-size="pageSize" :total="total" @change="turn"
                   @resize="resize"/>
    <StudentAccountPanel v-if="detail" :key="detail.student.id" :student-id="detail.student.id"/>
    <StudentGrowthReview v-if="detail" :key="detail.student.id" :student-id="detail.student.id"/>
    <section v-if="detail" class="panel record-detail">
      <header class="workspace-toolbar"><h2>{{ detail.student.name }} · 成长记录</h2>
        <button class="button" @click="detail = undefined">收起</button>
      </header>
      <p v-if="!detail.timeline.length" class="muted">尚未录入成长记录。</p>
      <article v-for="(record,index) in detail.timeline" :key="index" class="record-event"><strong>{{
          record.title
        }}</strong>
        <p>{{ record.detail || '无补充说明' }}</p><small>{{ record.occurredOn }} · {{ record.source }}</small></article>
    </section>
    <dialog ref="editor" @cancel="saving && $event.preventDefault()">
      <header class="dialog-heading"><h2>{{
          mode === 'archive' ? '归档学生' : mode === 'edit' ? '编辑学生' : '新增学生'
        }}</h2>
        <button :disabled="saving" class="button" type="button" @click="editor?.close()">关闭</button>
      </header>
      <form class="action-form" @submit.prevent="save">
        <template v-if="mode === 'archive'"><p>确认归档“{{
            draft.name
          }}”？该学生将从在籍列表移除，历史成长、积分和帮扶记录会保留。</p></template>
        <template v-else><label>姓名<input v-model="draft.name" maxlength="64" required/></label><label>学号<input
            v-model="draft.studentNo" maxlength="32" required/></label><label>性别<select v-model="draft.gender">
          <option value="">未填写</option>
          <option>男</option>
          <option>女</option>
        </select></label></template>
        <p v-if="formError" class="record-error" role="alert">{{ formError }}</p>
        <button :disabled="saving" class="button primary dialog-done">
          {{ saving ? '正在保存…' : mode === 'archive' ? '确认归档' : '保存' }}
        </button>
      </form>
    </dialog>
  </section>
</template>

<style scoped>
.record-actions button + button {
  margin-left: 16px
}

.record-detail {
  padding: 20px
}

.record-event {
  padding: 12px 0;
  border-top: 1px solid var(--border)
}

.record-event p {
  margin: 8px 0
}

.record-event small {
  color: var(--muted)
}

.record-error {
  color: #b8584c;
  margin: 12px 0
}

select {
  font: inherit;
  padding: 10px;
  border: 1px solid var(--border);
  background: #fff;
  border-radius: 6px
}
</style>
