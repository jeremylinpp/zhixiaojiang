<script lang="ts" setup>
import PageHeader from './ui/PageHeader.vue';
import {onMounted, ref} from 'vue';
import StudentPicker from './StudentPicker.vue';
import {request} from '../api';

type Capability = {
  configured: boolean;
  model: string | null;
  whitelist: string[];
  disclaimer: string;
  requiresTeacherConfirmation: boolean;
};
type Volume = {
  students: number;
  growthRecords: number;
  scores: number;
  skills: number;
  openWarnings: number;
  lastRuleAnalysisAt: string | null;
};
type Status = { ai: Capability; data: Volume; boundaries: string[] };
type Analysis = {
  source: string;
  summary: string;
  evidence?: string[];
  attentionAreas?: string[];
  suggestions?: string[];
  disclaimer?: string
};

/** 白名单字段的中文说明：助手只读取这些事实类数据，不含身份信息。 */
const FIELD_LABELS: Record<string, string> = {
  scores: '考试成绩',
  attendance: '出勤',
  behavior: '行为表现',
  skills: '技能记录',
  tasks: '任务完成'
};

const status = ref<Status>(), studentId = ref(0), analysis = ref<Analysis>();
const loading = ref(false), analyzing = ref(false), planBusy = ref(false), error = ref(''), notice = ref('');

async function load() {
  loading.value = true;
  error.value = '';
  try {
    status.value = await request<Status>('/assistant/status');
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}

async function analyze() {
  if (!studentId.value || analyzing.value) return;
  analyzing.value = true;
  error.value = '';
  notice.value = '';
  analysis.value = undefined;
  try {
    analysis.value = await request<Analysis>('/ai/student-analysis', 'POST', {studentId: studentId.value});
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    analyzing.value = false;
  }
}

async function createPlan() {
  if (!analysis.value || planBusy.value) return;
  planBusy.value = true;
  error.value = '';
  try {
    const suggestions = analysis.value.suggestions?.length ? analysis.value.suggestions : undefined;
    const created = await request<{ id: number }>('/interventions', 'POST', {
      studentId: studentId.value,
      suggestions,
      teacherNote: `来源：${analysis.value.source === 'MODEL' ? '模型服务' : '规则模板'}辅助分析`
    });
    notice.value = `已生成帮扶草案 #${created.id}（草稿）。请在「一人一策」中审核确认后再执行。`;
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    planBusy.value = false;
  }
}

function modeLabel(capability: Capability) {
  return capability.configured ? '已接入模型服务' : '规则模板模式';
}

onMounted(load);
</script>

<template>
  <section class="page-scroll workspace-page">
    <PageHeader description="助手只做数据归纳与建议，结论与处置由班主任作出。" eyebrow="以数据为依据的育人辅助入口"
                title="智小匠助手">
      <button :disabled="loading" class="button" @click="load">{{ loading ? '读取中…' : '刷新状态' }}</button>
    </PageHeader>

    <p v-if="error" class="assistant-error" role="alert">{{ error }}
      <button class="button" @click="load">重试</button>
    </p>
    <p v-if="notice" class="assistant-notice" role="status">{{ notice }}</p>
    <p v-if="loading && !status" role="status">正在读取助手状态…</p>

    <template v-if="status">
      <section class="assistant-cards">
        <article class="panel assistant-card">
          <h2>AI 建议</h2>
          <p :class="status.ai.configured ? 'success' : 'warning'" class="assistant-mode">{{ modeLabel(status.ai) }}</p>
          <p class="muted">模型：{{ status.ai.model || '未配置' }}；未配置或调用异常时自动降级为规则模板，演示不中断。</p>
          <p class="muted">送模型字段：{{ status.ai.whitelist.map(f => FIELD_LABELS[f] || f).join('、') }}</p>
          <p class="assistant-disclaimer">{{ status.ai.disclaimer }}</p>
        </article>

        <article class="panel assistant-card">
          <h2>已录入数据</h2>
          <dl class="assistant-metrics">
            <div>
              <dt>在籍学生</dt>
              <dd>{{ status.data.students }}</dd>
            </div>
            <div>
              <dt>成长记录</dt>
              <dd>{{ status.data.growthRecords }}</dd>
            </div>
            <div>
              <dt>成绩记录</dt>
              <dd>{{ status.data.scores }}</dd>
            </div>
            <div>
              <dt>技能记录</dt>
              <dd>{{ status.data.skills }}</dd>
            </div>
            <div>
              <dt>待研判预警</dt>
              <dd>{{ status.data.openWarnings }}</dd>
            </div>
          </dl>
          <p class="muted">最近规则筛查：{{
              status.data.lastRuleAnalysisAt ? status.data.lastRuleAnalysisAt.slice(0, 16) : '尚未执行'
            }}</p>
        </article>

        <article class="panel assistant-card">
          <h2>使用边界</h2>
          <ul class="assistant-boundaries">
            <li v-for="row in status.boundaries" :key="row">{{ row }}</li>
          </ul>
        </article>
      </section>

      <section class="panel assistant-analyze">
        <h2>生成辅助分析</h2>
        <StudentPicker v-model="studentId" :disabled="analyzing"/>
        <button :disabled="!studentId || analyzing" class="button primary" @click="analyze">
          {{ analyzing ? '分析中…' : '生成辅助分析' }}
        </button>

        <div v-if="analysis" class="assistant-result">
          <p class="assistant-source">来源：{{
              analysis.source === 'MODEL' ? '模型服务' : '规则模板（未配置模型服务）'
            }}</p>
          <h3>摘要</h3>
          <p>{{ analysis.summary }}</p>
          <template v-if="analysis.evidence?.length"><h3>趋势证据</h3>
            <ul>
              <li v-for="row in analysis.evidence" :key="row">{{ row }}</li>
            </ul>
          </template>
          <template v-if="analysis.attentionAreas?.length"><h3>关注方向</h3>
            <ul>
              <li v-for="row in analysis.attentionAreas" :key="row">{{ row }}</li>
            </ul>
          </template>
          <template v-if="analysis.suggestions?.length"><h3>建议措施</h3>
            <ul>
              <li v-for="row in analysis.suggestions" :key="row">{{ row }}</li>
            </ul>
          </template>
          <p class="assistant-disclaimer">{{ analysis.disclaimer || status.ai.disclaimer }}</p>
          <button :disabled="planBusy" class="button primary" @click="createPlan">{{
              planBusy ? '生成中…' : '转为帮扶草案'
            }}
          </button>
          <small class="muted">草案默认是草稿状态，需在「一人一策」中经教师确认后才能进入执行中。</small>
        </div>
      </section>
    </template>
  </section>
</template>

<style scoped>
.assistant-cards {
  display: grid;
  grid-template-columns:repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px
}

.assistant-card {
  padding: 18px
}

.assistant-card h2 {
  font-size: 17px;
  margin-bottom: 10px
}

.assistant-card .muted {
  font-size: 13px;
  line-height: 1.8
}

.assistant-mode {
  display: inline-block;
  padding: 3px 9px;
  border-radius: 4px;
  font-size: 13px;
  margin-bottom: 10px
}

.assistant-mode.success {
  background: #e8efe5;
  color: #668254
}

.assistant-mode.warning {
  background: #f7ebd9;
  color: #ac7839
}

.assistant-metrics {
  display: grid;
  grid-template-columns:repeat(auto-fit, minmax(90px, 1fr));
  gap: 10px;
  margin: 0 0 12px
}

.assistant-metrics dt {
  color: var(--muted);
  font-size: 12px
}

.assistant-metrics dd {
  margin: 4px 0 0;
  font: 700 20px 'JetBrains Mono Variable', monospace
}

.assistant-boundaries {
  margin: 0;
  padding-left: 18px;
  line-height: 1.9;
  font-size: 13px
}

.assistant-analyze {
  padding: 20px;
  margin-top: 18px
}

.assistant-analyze h2 {
  font-size: 18px;
  margin-bottom: 12px
}

.assistant-result {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid var(--border);
  line-height: 1.85
}

.assistant-result h3 {
  font-size: 14px;
  margin: 14px 0 6px
}

.assistant-result ul {
  margin: 0;
  padding-left: 20px;
  font-size: 14px
}

.assistant-source {
  font-size: 12px;
  color: var(--muted)
}

.assistant-disclaimer {
  font-size: 12px;
  color: #8a8276;
  margin: 10px 0
}

.assistant-notice {
  color: #668254;
  margin: 12px 0
}

.assistant-error {
  color: #b8584c;
  margin: 12px 0
}
</style>
