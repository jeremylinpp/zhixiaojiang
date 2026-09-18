<script lang="ts" setup>
import PageHeader from './ui/PageHeader.vue';
import EmptyState from './ui/EmptyState.vue';
import {onMounted, ref} from 'vue';
import {request} from '../api';

type Target = {
  id: number;
  name: string;
  targetValue: number;
  currentValue: number;
  unit: string;
  status: string;
  deviation: number
};
type Record_ = {
  id: number;
  targetId: number;
  measure: string;
  reviewResult: string | null;
  recordedOn: string;
  createdBy: number
};

const CYCLE = ['目标', '标准', '计划', '实施', '监测', '诊断', '改进', '优化'];

const targets = ref<Target[]>([]), cycle = ref<string[]>(CYCLE);
const loading = ref(false), error = ref(''), notice = ref(''), busy = ref(false), formError = ref('');
const selected = ref<Target>(), records = ref<Record_[]>([]), detailBusy = ref(false);
const creating = ref(false), draft = ref({name: '', targetValue: 80, currentValue: 0, unit: '%'});
const measure = ref(''), reviewResult = ref(''), recordedOn = ref('');
const reviewValue = ref<number | null>(null), reviewStatus = ref('');
const today = new Date().toLocaleDateString('en-CA');
let generation = 0;

async function load() {
  const current = ++generation;
  loading.value = true;
  error.value = '';
  try {
    const data = await request<{ items: Target[]; cycle: string[] }>('/class-diagnoses');
    if (current !== generation) return;
    targets.value = data.items;
    if (data.cycle?.length) cycle.value = data.cycle;
  } catch (e) {
    if (current === generation) error.value = (e as Error).message;
  } finally {
    if (current === generation) loading.value = false;
  }
}

async function inspect(target: Target) {
  detailBusy.value = true;
  formError.value = '';
  notice.value = '';
  measure.value = '';
  reviewResult.value = '';
  recordedOn.value = today;
  reviewValue.value = target.currentValue;
  reviewStatus.value = target.status;
  try {
    selected.value = target;
    records.value = (await request<{ items: Record_[] }>(`/class-diagnoses/${target.id}/records`)).items;
  } catch (e) {
    formError.value = (e as Error).message;
  } finally {
    detailBusy.value = false;
  }
}

async function refreshDetail() {
  if (selected.value) await inspect(selected.value);
}

async function createTarget() {
  if (busy.value) return;
  if (!draft.value.name.trim()) {
    formError.value = '请填写目标名称';
    return;
  }
  busy.value = true;
  formError.value = '';
  try {
    const created = await request<{ id: number }>('/class-diagnoses', 'POST', {
      name: draft.value.name.trim(),
      targetValue: Number(draft.value.targetValue),
      currentValue: Number(draft.value.currentValue),
      unit: draft.value.unit
    });
    notice.value = `已创建诊改目标 #${created.id}。`;
    creating.value = false;
    draft.value = {...draft.value, name: ''};
    await load();
    await inspect(targets.value.find(t => t.id === created.id) || targets.value[0]);
  } catch (e) {
    formError.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}

async function addRecord() {
  if (!selected.value || busy.value) return;
  if (!measure.value.trim()) {
    formError.value = '请填写改进措施';
    return;
  }
  busy.value = true;
  formError.value = '';
  try {
    await request(`/class-diagnoses/${selected.value.id}/records`, 'POST', {
      measure: measure.value.trim(),
      reviewResult: reviewResult.value.trim() || undefined,
      recordedOn: recordedOn.value || undefined
    });
    notice.value = '改进措施已记录。';
    measure.value = '';
    reviewResult.value = '';
    await refreshDetail();
  } catch (e) {
    formError.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}

async function review() {
  if (!selected.value || busy.value) return;
  busy.value = true;
  formError.value = '';
  try {
    await request(`/class-diagnoses/${selected.value.id}/review`, 'POST', {
      currentValue: Number(reviewValue.value ?? selected.value.currentValue),
      status: reviewStatus.value || undefined
    });
    notice.value = '复评已记录，偏差会按新的当前值重新计算。';
    await load();
    await refreshDetail();
  } catch (e) {
    formError.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}

function deviationTone(target: Target) {
  return target.deviation > 0 ? 'warning' : 'success';
}

onMounted(async () => {
  await load();
  if (targets.value.length) await inspect(targets.value[0]);
});
</script>

<template>
  <section class="page-scroll workspace-page">
    <PageHeader description="目标值、当前值与偏差来自实际记录；改进措施与复评结果逐条留痕。" eyebrow="{{ cycle.join(' · ') }}"
                title="班级诊改">
      <button class="button primary" @click="creating = !creating">{{ creating ? '收起新建' : '新增诊改目标' }}</button>
    </PageHeader>

    <section v-if="creating" class="panel target-create">
      <h2>新增诊改目标</h2>
      <form class="action-form" @submit.prevent="createTarget">
        <div class="target-create-grid">
          <label>目标名称<input v-model="draft.name" maxlength="160" placeholder="例如：数学及格率" required/></label>
          <label>目标值<input v-model.number="draft.targetValue" required step="0.1" type="number"/></label>
          <label>当前值<input v-model.number="draft.currentValue" step="0.1" type="number"/></label>
          <label>单位<input v-model="draft.unit" maxlength="20" placeholder="%"/></label>
        </div>
        <button :disabled="busy" class="button primary dialog-done">{{ busy ? '创建中…' : '创建目标' }}</button>
      </form>
    </section>

    <p v-if="notice" class="target-notice" role="status">{{ notice }}</p>
    <p v-if="error" class="target-error" role="alert">{{ error }}
      <button class="button" @click="load">重试</button>
    </p>
    <p v-if="loading" role="status">正在读取诊改指标…</p>

    <section v-else-if="!error" class="data-card panel">
      <table>
        <thead>
        <tr>
          <th>目标</th>
          <th>当前值</th>
          <th>目标值</th>
          <th>偏差</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="target in targets" :key="target.id">
          <td>{{ target.name }}</td>
          <td>{{ target.currentValue }}{{ target.unit || '' }}</td>
          <td>{{ target.targetValue }}{{ target.unit || '' }}</td>
          <td><span :class="deviationTone(target)" class="status-chip">{{
              target.deviation > 0 ? '差 ' + target.deviation : '已达成'
            }}{{ target.unit || '' }}</span></td>
          <td>{{ target.status }}</td>
          <td>
            <button class="table-action" @click="inspect(target)">措施与复评</button>
          </td>
        </tr>
        <tr v-if="!targets.length">
          <td class="empty-cell" colspan="6">
            <EmptyState description="点击创建目标，设置班级改进方向与目标值。" title="尚未设置诊改目标"/>
          </td>
        </tr>
        </tbody>
      </table>
    </section>

    <section v-if="selected" class="panel target-detail">
      <header class="target-heading">
        <div><h2>{{ selected.name }}</h2>
          <p class="muted">当前 {{ selected.currentValue }}{{ selected.unit || '' }} / 目标 {{
              selected.targetValue
            }}{{ selected.unit || '' }} · 偏差 {{ selected.deviation }}</p></div>
        <button :disabled="detailBusy" class="button" @click="selected = undefined">收起</button>
      </header>

      <div class="target-columns">
        <div>
          <h3>改进措施记录</h3>
          <article v-for="row in records" :key="row.id" class="target-record">
            <strong>{{ row.measure }}</strong>
            <p v-if="row.reviewResult">{{ row.reviewResult }}</p>
            <small>{{ row.recordedOn }} · 记录人 #{{ row.createdBy }}</small>
          </article>
          <p v-if="!records.length" class="muted">尚未记录改进措施。</p>
          <form class="action-form" @submit.prevent="addRecord">
            <label>改进措施<textarea v-model="measure" maxlength="500" placeholder="例如：每周两次数学专项辅导" required
                                     rows="2"/></label>
            <label>复评结果（可选）<textarea v-model="reviewResult" maxlength="500" rows="2"/></label>
            <label>记录日期<input v-model="recordedOn" :max="today" type="date"/></label>
            <button :disabled="busy" class="button">记录措施</button>
          </form>
        </div>

        <div>
          <h3>阶段复评</h3>
          <form class="action-form" @submit.prevent="review">
            <label>复评后的当前值<input v-model.number="reviewValue" required step="0.1" type="number"/></label>
            <label>指标状态<input v-model="reviewStatus" maxlength="20"
                                  placeholder="例如：IN_PROGRESS / ACHIEVED"/></label>
            <button :disabled="busy" class="button primary">{{ busy ? '保存中…' : '记录复评' }}</button>
          </form>
          <p class="muted target-help">复评只更新当前值与状态；措施与复评历史保留在记录中，不会覆盖。</p>
        </div>
      </div>
      <p v-if="formError" class="target-error" role="alert">{{ formError }}</p>
    </section>
  </section>
</template>

<style scoped>
.target-create {
  padding: 20px;
  margin-bottom: 18px
}

.target-create h2 {
  font-size: 18px;
  margin-bottom: 14px
}

.target-create-grid {
  display: grid;
  grid-template-columns:repeat(auto-fit, minmax(160px, 1fr));
  gap: 14px;
  align-items: end
}

.target-detail {
  padding: 20px;
  margin-top: 18px
}

.target-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px
}

.target-heading h2 {
  font-size: 20px
}

.target-columns {
  display: grid;
  grid-template-columns:1fr 1fr;
  gap: 24px;
  margin-top: 16px
}

.target-columns h3 {
  font-size: 15px;
  margin: 0 0 10px
}

.target-record {
  border-top: 1px solid var(--border);
  padding: 10px 0
}

.target-record p {
  margin: 6px 0;
  line-height: 1.7
}

.target-record small {
  color: var(--muted)
}

.target-help {
  font-size: 12px;
  line-height: 1.8;
  margin-top: 10px
}

.target-notice {
  color: #668254;
  margin: 12px 0
}

.target-error {
  color: #b8584c;
  margin: 12px 0
}

@media (max-width: 1100px) {
  .target-columns {
    grid-template-columns:1fr
  }
}
</style>
