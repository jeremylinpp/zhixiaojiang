<script lang="ts" setup>
import {nextTick, onMounted, ref, watch} from 'vue';
import StudentPlans from './StudentPlans.vue';
import StudentMessages from './StudentMessages.vue';
import PageHeader from './ui/PageHeader.vue';
import {request} from '../api';
import EmptyState from './ui/EmptyState.vue';
import PaginationBar from './ui/PaginationBar.vue';
import StudentPasswordForm from './StudentPasswordForm.vue';
import StudentGrowth from './StudentGrowth.vue';
import TaskAttachments, {type Attachment} from './TaskAttachments.vue';

/**
 * 学生端内容区：外壳（顶栏、侧边栏、内容滚动）由 App.vue 提供，
 * 与教师端共用同一套布局，这里只按 view 渲染对应页面。
 */
const props = defineProps<{ view: string }>();
const emit = defineEmits<{ navigate: [string] }>();

type Task = {
  id: number;
  title: string;
  description?: string;
  module: string;
  dueOn: string;
  status: string;
  pointReward?: number;
  teacherNote?: string
};
type Submission = {
  id: number;
  content: string;
  status: string;
  feedback?: string;
  createdAt: string;
  attachments: Attachment[]
};
type Ledger = { id: number; amount: number; reason: string; createdAt: string; reversalOf?: number; reversed: boolean };
const attachmentIds = ref<number[]>([]);
const labels: Record<string, string> = {
  ASSIGNED: '待提交',
  SUBMITTED: '待评价',
  RETURNED: '需补充',
  COMPLETED: '已完成'
};
const pageDescriptions: Record<string, string> = {
  概览: '你的待办、机智币余额与最近动态。',
  六机任务: '教师指派的任务与你的提交记录。',
  成长画像: '四维评价、成绩、技能与活动记录。',
  机智币: '积分流水与余额；撤销通过反向流水实现。',
  成长计划: '教师发布的目标与行动安排，可记录执行情况。',
  消息: '任务、成长计划与成长记录审核的通知。',
  个人资料: '账号信息与修改密码。'
};
const busy = ref(false), error = ref(''), notice = ref('');

const home = ref<{ student: { name: string; studentNo: string; className: string }; todos: Task[]; balance: number }>();
const tasks = ref<Task[]>([]), selected = ref<Task>(), history = ref<Submission[]>([]), content = ref(''),
    requestKey = ref('');
const ledger = ref<{ items: Ledger[]; balance: number; total: number }>(), page = ref(1), pageSize = ref(20);
let generation = 0;

async function load() {
  const current = ++generation;
  error.value = '';
  try {
    if (props.view === '概览') {
      const data = await request<typeof home.value>('/student-portal/home');
      if (current === generation) home.value = data;
    } else if (props.view === '六机任务') {
      const data = await request<{ items: Task[] }>('/student-portal/tasks');
      if (current === generation) tasks.value = data.items;
    } else if (props.view === '机智币') {
      const data = await request<typeof ledger.value>(`/student-portal/points?page=${page.value}&pageSize=${pageSize.value}`);
      if (current === generation) ledger.value = data;
    }
  } catch (e) {
    if (current === generation) error.value = (e as Error).message;
  }
}

function navigate(next: string) {
  if (busy.value) return;
  selected.value = undefined;
  notice.value = '';
  emit('navigate', next);
}

async function inspect(task: Task) {
  if (busy.value) return;
  busy.value = true;
  error.value = '';
  try {
    const data = await request<{ items: Submission[] }>(`/student-portal/tasks/${task.id}/submissions`);
    history.value = data.items;
    selected.value = task;
    content.value = '';
    attachmentIds.value = [];
    requestKey.value = crypto.randomUUID();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}

async function submit() {
  if (!selected.value || busy.value) return;
  busy.value = true;
  error.value = '';
  notice.value = '';
  try {
    await request(`/student-portal/tasks/${selected.value.id}/submissions`, 'POST', {
      content: content.value,
      requestKey: requestKey.value,
      attachmentIds: attachmentIds.value
    });
    selected.value = undefined;
    notice.value = '成果已提交，等待教师评价。';
    await load();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}

function changePage(next: number) {
  page.value = next;
  load();
}

function resize(size: number) {
  pageSize.value = size;
  changePage(1);
}

watch(() => props.view, (next) => {
  selected.value = undefined;
  notice.value = '';
  nextTick(load);
});
onMounted(load);
</script>
<template>
  <section class="page-scroll workspace-page student-content">
    <PageHeader :description="pageDescriptions[view]" eyebrow="学生成长工作台" :title="view"/>
    <p v-if="notice" role="status">{{ notice }}</p>
    <p v-if="error" role="alert">{{ error }}
      <button class="button" @click="load">重新读取</button>
    </p>
    <StudentPasswordForm v-if="view==='个人资料'"/>
    <StudentGrowth v-else-if="view==='成长画像'"/>
    <StudentPlans v-else-if="view==='成长计划'"/>
    <StudentMessages v-else-if="view==='消息'" @navigate="navigate"/>
    <template v-else-if="view==='概览' && home">
      <section class="panel student-card"><h2>{{ home.student.name }}，你好</h2>
        <p>{{ home.student.className }} · {{ home.student.studentNo }}</p>
        <p>机智币余额 <strong>{{ home.balance }}</strong> · 待办 <strong>{{ home.todos.length }}</strong></p>
      </section>
      <section class="panel student-card"><h2>我的待办</h2>
        <article v-for="task in home.todos" :key="task.id" class="student-task"><h3>{{ task.title }}</h3>
          <p>{{ labels[task.status] }} · 截止 {{ task.dueOn }}</p>
          <button class="button" @click="navigate('六机任务')">查看任务</button>
        </article>
        <EmptyState v-if="!home.todos.length" description="教师发布并指派任务后，会显示在这里。" title="暂无待办"/>
      </section>
    </template>
    <template v-else-if="view==='六机任务'">
      <section v-if="selected" class="panel student-card">
        <button :disabled="busy" class="button" @click="selected=undefined">返回任务列表</button>
        <h2>{{ selected.title }}</h2>
        <p>{{ selected.description }}</p>
        <p>{{ labels[selected.status] }} · 截止 {{ selected.dueOn }}</p>
        <p v-if="selected.teacherNote" class="student-prose">当前教师评价：{{ selected.teacherNote }}</p>
        <form v-if="['ASSIGNED','RETURNED'].includes(selected.status)" class="action-form" @submit.prevent="submit">
          <label>成果与过程说明<textarea v-model="content" :disabled="busy" maxlength="4000" required
                                         rows="6"/></label>
          <TaskAttachments :key="selected.id" :disabled="busy" :task-id="selected.id" @busy="busy=$event"
                           @change="attachmentIds=$event"/>
          <button :disabled="busy || !content.trim()" class="button primary">
            {{ busy ? '正在处理…' : '提交教师评价' }}
          </button>
        </form>
        <h3>提交与反馈</h3>
        <article v-for="entry in history" :key="entry.id" class="student-task"><p>{{ labels[entry.status] }} ·
          {{ entry.createdAt }}</p>
          <p class="student-prose">{{ entry.content }}</p>
          <TaskAttachments :files="entry.attachments"/>
          <p v-if="entry.feedback" class="student-prose">教师反馈：{{ entry.feedback }}</p></article>
        <EmptyState v-if="!history.length" title="尚未提交成果"/>
      </section>
      <template v-else>
        <article v-for="task in tasks" :key="task.id" class="panel student-card">
          <span>{{ task.module }} · {{ labels[task.status] }}</span>
          <h2>{{ task.title }}</h2>
          <p>截止 {{ task.dueOn }} · 教师确认完成后奖励 {{ task.pointReward }} 机智币</p>
          <p v-if="task.teacherNote">教师反馈：{{ task.teacherNote }}</p>
          <button :disabled="busy" class="button" @click="inspect(task)">查看要求与成果</button>
        </article>
        <EmptyState v-if="!tasks.length && !error" description="教师指派的任务会显示在这里。" title="暂无任务"/>
      </template>
    </template>
    <section v-else-if="view==='机智币' && ledger" class="panel student-card"><h2>余额 {{ ledger.balance }}</h2>
      <p>机智币与四维评价独立，不自动折算为品德分数。</p>
      <article v-for="entry in ledger.items" :key="entry.id" class="student-task">
        <strong>{{ entry.amount > 0 ? '+' : '' }}{{ entry.amount }}</strong>
        <p>{{ entry.reason }}</p><small>{{ entry.createdAt }}<span v-if="entry.reversed"> · 已撤销</span><span
          v-if="entry.reversalOf"> · 纠正流水 #{{ entry.reversalOf }}</span></small></article>
      <EmptyState v-if="!ledger.items.length" title="暂无积分流水"/>
      <PaginationBar :disabled="busy" :page="page" :page-size="pageSize" :total="ledger.total"
                     @change="changePage" @resize="resize"/>
    </section>
  </section>
</template>
<style scoped>
.student-card {
  padding: 24px;
  margin-top: 20px
}

.student-card h2 {
  font-size: 22px;
  margin: 12px 0
}

.student-card p {
  line-height: 1.8
}

.student-task {
  padding: 16px 0;
  border-top: 1px solid var(--border);
  overflow-wrap: anywhere
}

.student-prose {
  white-space: pre-wrap
}

@media (max-width: 600px) {
  .student-card {
    padding: 16px
  }

  .student-content .menu-item, .student-content .view-switch {
    min-height: 44px
  }
}
</style>
