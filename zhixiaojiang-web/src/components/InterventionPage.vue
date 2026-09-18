<script lang="ts" setup>
import PageHeader from './ui/PageHeader.vue';
import StudentPlans from './StudentPlans.vue';
import SegmentedControl from './ui/SegmentedControl.vue';
import EmptyState from './ui/EmptyState.vue';
import {onMounted, ref} from 'vue';
import StudentPicker from './StudentPicker.vue';
import {request} from '../api';

type Plan = {
  id: number;
  studentId: number;
  warningId: number | null;
  studentName: string;
  title: string;
  status: string;
  suggestions: string[];
  teacherNote: string | null;
  reviewAt: string | null;
  createdAt?: string
};
type Record_ = {
  id: number;
  action: string;
  status: string;
  occurredOn: string;
  result: string | null;
  createdBy: number
};
type Item = {
  id: number;
  title: string;
  status: string;
  teacherNote: string | null;
  studentName: string;
  reviewAt: string | null
};

const STATUS: Record<string, string> = {
  DRAFT: '草稿',
  CONFIRMED: '已确认',
  IN_PROGRESS: '执行中',
  COMPLETED: '已完成',
  CLOSED: '已关闭'
};
const TONE: Record<string, string> = {
  草稿: 'warning',
  已确认: 'warning',
  执行中: 'success',
  已完成: 'success',
  已关闭: 'success'
};
const FILTERS = [{value: 'ALL', label: '全部'}, {value: 'DRAFT', label: '草稿'}, {
  value: 'CONFIRMED',
  label: '已确认'
}, {value: 'IN_PROGRESS', label: '执行中'}, {value: 'COMPLETED', label: '已完成'}, {value: 'CLOSED', label: '已关闭'}];
const NEXT: Record<string, [string, string][]> = {
  DRAFT: [['CONFIRMED', '审核通过并确认'], ['CLOSED', '关闭草案']],
  CONFIRMED: [['IN_PROGRESS', '开始执行'], ['CLOSED', '关闭方案']],
  IN_PROGRESS: [['COMPLETED', '标记完成'], ['CLOSED', '关闭方案']],
};

const items = ref<Item[]>([]), filter = ref('ALL'), query = ref('');
const loading = ref(false), error = ref(''), notice = ref(''), busy = ref(false), formError = ref('');
const selected = ref<Plan>(), records = ref<Record_[]>([]), detailBusy = ref(false);
const editTitle = ref(''), editNote = ref(''), editReview = ref('');
const recordAction = ref(''), recordResult = ref('');
const creating = ref(false), newStudentId = ref(0), newTitle = ref(''), newReview = ref(''),
    newSuggestions = ref('班主任个别谈话\n两周后成长复评');
let generation = 0;

const today = new Date().toLocaleDateString('en-CA');
const defaultReview = new Date(Date.now() + 14 * 86400000).toLocaleDateString('en-CA');

async function load() {
  const current = ++generation;
  loading.value = true;
  error.value = '';
  try {
    const data = await request<{ items: Item[] }>('/interventions');
    if (current !== generation) return;
    items.value = data.items;
  } catch (e) {
    if (current === generation) error.value = (e as Error).message;
  } finally {
    if (current === generation) loading.value = false;
  }
}

async function inspect(id: number) {
  detailBusy.value = true;
  error.value = '';
  formError.value = '';
  recordAction.value = '';
  recordResult.value = '';
  try {
    const data = await request<{ plan: Plan; records: Record_[] }>(`/interventions/${id}`);
    selected.value = data.plan;
    records.value = data.records;
    editTitle.value = data.plan.title;
    editNote.value = data.plan.teacherNote || '';
    editReview.value = data.plan.reviewAt || '';
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    detailBusy.value = false;
  }
}

async function refresh(keepSelection = true) {
  const kept = keepSelection && selected.value ? selected.value.id : 0;
  await load();
  if (kept) await inspect(kept); else selected.value = undefined;
}

async function transition(next: string) {
  if (!selected.value || busy.value) return;
  busy.value = true;
  formError.value = '';
  try {
    await request(`/interventions/${selected.value.id}/transition`, 'POST', {status: next});
    notice.value = `方案状态已更新为「${STATUS[next] || next}」。`;
    await refresh();
  } catch (e) {
    formError.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}

async function savePlan() {
  if (!selected.value || busy.value) return;
  busy.value = true;
  formError.value = '';
  try {
    await request(`/interventions/${selected.value.id}`, 'PUT', {
      title: editTitle.value.trim() || undefined,
      teacherNote: editNote.value.trim() || undefined,
      reviewAt: editReview.value || undefined
    });
    notice.value = '方案内容已保存。';
    await refresh();
  } catch (e) {
    formError.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}

async function addRecord() {
  if (!selected.value || busy.value) return;
  if (!recordAction.value.trim()) {
    formError.value = '请填写执行动作';
    return;
  }
  busy.value = true;
  formError.value = '';
  try {
    await request(`/interventions/${selected.value.id}/records`, 'POST', {
      action: recordAction.value.trim(),
      result: recordResult.value.trim() || undefined
    });
    notice.value = '执行过程已记录。';
    recordAction.value = '';
    recordResult.value = '';
    await refresh();
  } catch (e) {
    formError.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}

async function saveReview() {
  if (!selected.value || busy.value) return;
  busy.value = true;
  formError.value = '';
  try {
    await request(`/interventions/${selected.value.id}/review`, 'POST', {
      reviewAt: editReview.value || undefined,
      teacherNote: editNote.value.trim() || undefined
    });
    notice.value = '已记录阶段复评。';
    await refresh();
  } catch (e) {
    formError.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}

async function createPlan() {
  if (busy.value) return;
  if (!newStudentId.value) {
    formError.value = '请先选择学生';
    return;
  }
  busy.value = true;
  formError.value = '';
  try {
    const suggestions = newSuggestions.value.split('\n').map(row => row.trim()).filter(Boolean);
    const created = await request<{ id: number }>('/interventions', 'POST', {
      studentId: newStudentId.value,
      title: newTitle.value.trim() || '阶段成长支持方案',
      suggestions,
      reviewAt: newReview.value || undefined
    });
    notice.value = `已创建帮扶草案 #${created.id}，状态为草稿，需审核确认后才能执行。`;
    creating.value = false;
    newTitle.value = '';
    newReview.value = '';
    await refresh(false);
  } catch (e) {
    formError.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}

function visible(item: Item) {
  if (filter.value !== 'ALL' && item.status !== filter.value) return false;
  const keyword = query.value.trim();
  return !keyword || item.title.includes(keyword) || (item.studentName || '').includes(keyword);
}

onMounted(() => {
  newReview.value = defaultReview;
  load();
});
</script>

<template>
  <section class="page-scroll workspace-page">
    <PageHeader description="AI 与规则只提供建议，方案需教师确认后才能进入执行中。" eyebrow="教师审核与执行过程"
                title="一人一策">
      <button class="button primary" @click="creating = !creating">{{ creating ? '收起新建' : '新建帮扶方案' }}</button>
    </PageHeader>

    <section v-if="creating" class="panel plan-create">
      <h2>新建帮扶草案</h2>
      <StudentPicker v-model="newStudentId" :disabled="busy"/>
      <form class="action-form" @submit.prevent="createPlan">
        <label>方案标题<input v-model="newTitle" maxlength="160" placeholder="例如：数学学习支持方案"/></label>
        <label>下次复评日期<input v-model="newReview" :min="today" type="date"/></label>
        <label>帮扶措施（每行一条）<textarea v-model="newSuggestions" maxlength="1000" rows="3"/></label>
        <button :disabled="busy || !newStudentId" class="button primary dialog-done">{{
            busy ? '创建中…' : '创建草案'
          }}
        </button>
      </form>
    </section>

    <div class="plan-filters">
      <SegmentedControl v-model="filter" :options="FILTERS" label="帮扶状态"/>
      <input v-model="query" aria-label="按学生或方案标题搜索" class="plan-search" placeholder="学生或方案标题"/>
      <span class="result-count">{{ items.filter(visible).length }} / {{ items.length }} 个方案</span>
    </div>

    <p v-if="notice" class="plan-notice" role="status">{{ notice }}</p>
    <p v-if="error" class="plan-error" role="alert">{{ error }}
      <button class="button" @click="load">重试</button>
    </p>
    <p v-if="loading" role="status">正在读取方案…</p>

    <section v-else-if="!error" class="data-card panel">
      <table>
        <thead>
        <tr>
          <th>学生</th>
          <th>方案</th>
          <th>状态</th>
          <th>下次复评</th>
          <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="item in items.filter(visible)" :key="item.id">
          <td>{{ item.studentName }}</td>
          <td>{{ item.title }}<small v-if="item.teacherNote">{{ item.teacherNote }}</small></td>
          <td><span :class="TONE[STATUS[item.status]] || 'warning'"
                    class="status-chip">{{ STATUS[item.status] || item.status }}</span></td>
          <td>{{ item.reviewAt || '待安排' }}</td>
          <td>
            <button class="table-action" @click="inspect(item.id)">审核与记录</button>
          </td>
        </tr>
        <tr v-if="!items.filter(visible).length">
          <td class="empty-cell" colspan="5">
            <EmptyState :description="items.length ? '请切换状态或修改搜索条件。' : '点击新建帮扶方案，为学生建立支持计划。'"
                        :title="items.length ? '没有符合条件的方案' : '暂无帮扶方案'"/>
          </td>
        </tr>
        </tbody>
      </table>
    </section>

    <StudentPlans v-if="selected" :key="`${selected.id}-${selected.status}`" :plan-status="selected.status"
                  :teacher-plan-id="selected.id"/>
    <section v-if="selected" class="panel plan-detail">
      <header class="plan-heading">
        <div>
          <h2>{{ selected.studentName }} · {{ selected.title }}</h2>
          <p class="muted">方案 #{{ selected.id }} · {{ STATUS[selected.status] || selected.status }}<span
              v-if="selected.warningId"> · 来源预警 #{{ selected.warningId }}</span></p>
        </div>
        <button :disabled="detailBusy" class="button" @click="selected = undefined">收起</button>
      </header>

      <div class="plan-columns">
        <div>
          <h3>帮扶措施</h3>
          <ul class="plan-suggestions">
            <li v-for="row in selected.suggestions" :key="row">{{ row }}</li>
          </ul>
          <p v-if="!selected.suggestions.length" class="muted">未记录措施建议。</p>

          <h3>执行过程</h3>
          <article v-for="row in records" :key="row.id" class="plan-record">
            <strong>{{ row.action }}</strong>
            <p>{{ row.result || '未填写结果' }}</p>
            <small>{{ row.occurredOn }} · 记录人 #{{ row.createdBy }}</small>
          </article>
          <p v-if="!records.length" class="muted">尚未记录执行过程。</p>
          <form class="action-form" @submit.prevent="addRecord">
            <label>执行动作<input v-model="recordAction" maxlength="300" placeholder="例如：班主任个别谈话"/></label>
            <label>执行结果<textarea v-model="recordResult" maxlength="500" rows="2"/></label>
            <button :disabled="busy" class="button">记录执行过程</button>
          </form>
        </div>

        <div>
          <h3>方案状态</h3>
          <div class="plan-actions">
            <button v-for="[next, text] in (NEXT[selected.status] || [])" :key="next" :disabled="busy"
                    class="button primary" @click="transition(next)">{{ text }}
            </button>
            <span v-if="!NEXT[selected.status]" class="muted">该状态已结束，如需继续帮扶请新建方案。</span>
          </div>

          <h3>修订与复评</h3>
          <form class="action-form" @submit.prevent="savePlan">
            <label>方案标题<input v-model="editTitle" maxlength="160" required/></label>
            <label>教师备注<textarea v-model="editNote" maxlength="1000" rows="3"/></label>
            <label>下次复评日期<input v-model="editReview" type="date"/></label>
            <div class="plan-form-actions">
              <button :disabled="busy" class="button">保存修改</button>
              <button :disabled="busy" class="button" type="button" @click="saveReview">记录阶段复评</button>
            </div>
          </form>
        </div>
      </div>
      <p v-if="formError" class="plan-error" role="alert">{{ formError }}</p>
    </section>
  </section>
</template>

<style scoped>
.plan-create {
  padding: 20px;
  margin-bottom: 18px
}

.plan-create h2 {
  font-size: 18px;
  margin-bottom: 14px
}

.plan-filters {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px
}

.plan-search {
  font: inherit;
  margin-left: auto;
  border: 1px solid var(--border);
  border-radius: 5px;
  background: #fffdf9;
  padding: 7px 9px;
  min-width: 200px
}

.plan-detail {
  padding: 20px;
  margin-top: 18px
}

.plan-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px
}

.plan-heading h2 {
  font-size: 20px
}

.plan-columns {
  display: grid;
  grid-template-columns:1fr 1fr;
  gap: 24px;
  margin-top: 16px
}

.plan-columns h3 {
  font-size: 15px;
  margin: 0 0 10px
}

.plan-suggestions {
  margin: 0 0 18px;
  padding-left: 20px;
  line-height: 1.9;
  font-size: 14px
}

.plan-record {
  border-top: 1px solid var(--border);
  padding: 10px 0
}

.plan-record p {
  margin: 6px 0;
  line-height: 1.7
}

.plan-record small {
  color: var(--muted)
}

.plan-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 18px
}

.plan-form-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap
}

.plan-notice {
  color: #668254;
  margin: 12px 0
}

.plan-error {
  color: #b8584c;
  margin: 12px 0
}

td small {
  display: block;
  color: var(--muted);
  font-size: 11px
}

@media (max-width: 1100px) {
  .plan-columns {
    grid-template-columns:1fr
  }
}

@media (max-width: 600px) {
  .plan-search {
    margin-left: 0;
    width: 100%
  }
}
</style>
