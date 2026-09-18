<script lang="ts" setup>
import {onMounted, ref} from 'vue';
import {request} from '../api';
import EmptyState from './ui/EmptyState.vue';

const props = defineProps<{ teacherPlanId?: number; planStatus?: string }>();
type Plan = {
  id: number;
  title: string;
  goal: string;
  actions: string;
  reviewOn: string;
  version: number;
  status?: string
};
type Detail = {
  versions: Plan[];
  latestVersion: number;
  executions: { id: number; content: string; occurredOn: string }[];
  feedback: { id: number; content: string; createdAt: string }[]
};
const items = ref<Plan[]>([]), selected = ref<Plan>(), detail = ref<Detail>(), loading = ref(false), busy = ref(false),
    error = ref(''), notice = ref('');
const today = new Date().toLocaleDateString('en-CA');
const publication = ref({title: '', goal: '', actions: '', reviewOn: today});
const entry = ref(''), occurredOn = ref(today), requestKey = ref(crypto.randomUUID());
const states: Record<string, string> = {
  CONFIRMED: '待执行',
  IN_PROGRESS: '执行中',
  COMPLETED: '已完成',
  CLOSED: '已结束'
};

async function load() {
  loading.value = true;
  error.value = '';
  try {
    items.value = (await request<{
      items: Plan[]
    }>(props.teacherPlanId ? `/interventions/${props.teacherPlanId}/student-publications` : '/student-portal/plans')).items;
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}

async function inspect(plan: Plan) {
  if (busy.value) return;
  busy.value = true;
  error.value = '';
  try {
    const result = await request<Detail>(props.teacherPlanId ? `/student-plan-publications/${plan.id}` : `/student-portal/plans/${plan.id}`);
    selected.value = result.versions.find(version => version.id === plan.id) || plan;
    detail.value = result;
    entry.value = '';
    requestKey.value = crypto.randomUUID();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}

async function publish() {
  if (busy.value) return;
  busy.value = true;
  error.value = '';
  try {
    await request(`/interventions/${props.teacherPlanId}/student-publications`, 'POST', {
      ...publication.value,
      expectedVersion: Math.max(0, ...items.value.map(x => x.version))
    });
    notice.value = '学生可见版本已发布。内部备注没有发布。';
    publication.value = {title: '', goal: '', actions: '', reviewOn: today};
    await load();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}

async function saveEntry() {
  if (busy.value || !selected.value) return;
  busy.value = true;
  error.value = '';
  try {
    await request(props.teacherPlanId ? `/student-plan-publications/${selected.value.id}/feedback` : `/student-portal/plans/${selected.value.id}/executions`, 'POST', {
      content: entry.value,
      occurredOn: occurredOn.value,
      requestKey: requestKey.value
    });
    entry.value = '';
    requestKey.value = crypto.randomUUID();
    detail.value = await request<Detail>(props.teacherPlanId ? `/student-plan-publications/${selected.value.id}` : `/student-portal/plans/${selected.value.id}`);
    notice.value = '记录已保存。';
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}

onMounted(load);
</script>
<template>
  <section class="student-plans">
    <h2>{{ teacherPlanId ? '发布给学生的成长计划' : '我的成长计划' }}</h2>
    <p>仅展示教师明确发布的目标、行动与反馈；执行记录不会自动改变成绩或积分。</p>
    <p v-if="error" role="alert">{{ error }}
      <button :disabled="busy" class="button" @click="load">重新读取</button>
    </p>
    <p v-if="notice" role="status">{{ notice }}</p>
    <p v-if="loading" role="status">正在读取计划…</p>
    <form v-if="teacherPlanId && ['CONFIRMED','IN_PROGRESS'].includes(planStatus||'')"
          class="action-form plan-box panel" @submit.prevent="publish"><h3>确认学生可见内容并发布新版本</h3>
      <p>请勿填写预警等级、内部谈话或家庭隐私。下方字段会原样向学生展示。</p>
      <fieldset :disabled="busy"><label>学生可见标题<input v-model="publication.title" maxlength="160"
                                                           required/></label><label>成长目标<textarea
          v-model="publication.goal" maxlength="1000" required rows="2"/></label><label>具体行动<textarea
          v-model="publication.actions" maxlength="2000" required rows="3"/></label><label>阶段复评日期<input
          v-model="publication.reviewOn" required type="date"/></label></fieldset>
      <button :disabled="busy || loading || !!error" class="button primary">确认并发布给学生</button>
    </form>
    <article v-for="plan in items" :key="plan.id" class="panel plan-box"><h3>{{ plan.title }} · 第 {{ plan.version }}
      版</h3>
      <p v-if="plan.status">{{ states[plan.status] || plan.status }}</p>
      <p class="prose">目标：{{ plan.goal }}</p>
      <p class="prose">行动：{{ plan.actions }}</p>
      <p>复评日期：{{ plan.reviewOn }}</p>
      <button :disabled="busy" class="button" @click="inspect(plan)">执行记录与阶段反馈</button>
    </article>
    <EmptyState v-if="!items.length && !loading && !error" title="暂无已发布的成长计划"/>
    <section v-if="selected && detail" class="panel plan-box"><h3>{{ selected.title }} · 第 {{ selected.version }}
      版记录</h3>
      <nav aria-label="计划版本" class="plan-versions">
        <button v-for="version in detail.versions" :key="version.id" :aria-pressed="version.id===selected.id"
                :disabled="busy" class="button" @click="inspect(version)">第
          {{ version.version }} 版{{ version.version === detail.latestVersion ? '（当前）' : '' }}
        </button>
      </nav>
      <p class="prose">目标：{{ selected.goal }}</p>
      <p class="prose">行动：{{ selected.actions }}</p>
      <p v-if="selected.version!==detail.latestVersion">历史版本仅供回顾，请切换当前版本记录新行动。</p><h4>执行情况</h4>
      <p v-if="!detail.executions.length">尚无执行记录。</p>
      <article v-for="record in detail.executions" :key="record.id"><small>{{ record.occurredOn }}</small>
        <p class="prose">{{ record.content }}</p></article>
      <h4>教师阶段反馈</h4>
      <p v-if="!detail.feedback.length">尚无阶段反馈。</p>
      <article v-for="record in detail.feedback" :key="record.id"><small>{{ record.createdAt }}</small>
        <p class="prose">{{ record.content }}</p></article>
      <form
          v-if="teacherPlanId || (selected.version===detail.latestVersion && ['CONFIRMED','IN_PROGRESS'].includes(selected.status||''))"
          class="action-form" @submit.prevent="saveEntry"><label v-if="!teacherPlanId">发生日期<input
          v-model="occurredOn" :max="today" required
          type="date"/></label><label>{{ teacherPlanId ? '发布阶段反馈（学生可见）' : '记录本次行动' }}<textarea
          v-model="entry" :disabled="busy" maxlength="2000" required rows="4"/></label>
        <button :disabled="busy || !entry.trim()" class="button primary">{{ busy ? '保存中…' : '保存记录' }}</button>
      </form>
    </section>
  </section>
</template>
<style scoped>.plan-versions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px
}

.plan-versions button[aria-pressed=true] {
  border-color: var(--accent, #e77956)
}

.student-plans {
  margin: 20px 0
}

.plan-box {
  padding: 20px;
  margin: 16px 0
}

.student-plans p {
  line-height: 1.8
}

.prose {
  white-space: pre-wrap;
  overflow-wrap: anywhere
}

.plan-box article {
  padding: 12px 0;
  border-top: 1px solid var(--border)
}

fieldset {
  border: 0;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 12px;
  min-width: 0
}

.student-plans input {
  font: inherit;
  min-height: 44px;
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 8px;
  background: #fffdf9
}</style>
