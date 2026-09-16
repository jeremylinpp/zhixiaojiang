<script setup lang="ts">
import PixelPet from './components/PixelPet.vue';
import StudentRecords from './components/StudentRecords.vue';
import PointsPage from './components/PointsPage.vue';
import TeacherProfile from './components/TeacherProfile.vue';
import GrowthPage from './components/GrowthPage.vue';
import WarningsPage from './components/WarningsPage.vue';
import InterventionPage from './components/InterventionPage.vue';
import TasksPage from './components/TasksPage.vue';
import DiagnosisPage from './components/DiagnosisPage.vue';
import AssistantPage from './components/AssistantPage.vue';
import { computed, ref, nextTick, onMounted, onUnmounted, watch } from 'vue';
import { PanelLeft, Search, Bell, Languages, FlaskConical, Activity, LayoutDashboard, Radar, KeyRound, FileText, ListTodo, Wallet, MessagesSquare, UserRound, ArrowRight, ChevronUp, ChevronDown, Check, Circle, CreditCard, SquareTerminal, RadioTower, ShieldCheck, Timer, BookOpen, Flame, TrendingUp, X, UsersRound, GraduationCap, Coins, Target, ClipboardCheck, Sparkles } from 'lucide-vue-next';

/** 本地开发和正式构建均固定使用智小匠业务视图，忽略旧参考页参数。 */
const school = computed(() => true);
const loginPage = ref(location.pathname === '/login');
const username = ref('teacher');
const password = ref('password');
const loginError = ref('');
const signingIn = ref(false);
const csrfToken = ref('');
const collapsed = ref(false);
const guide = ref(true);
const panel = ref('');
const live = ref(false);
const liveError = ref('');
const dashboard = ref<{metrics?:{studentCount?:number;activeRate?:number|null;attendanceCompleteness?:number;attendanceDate?:string|null};targets?:Array<{name:string;currentValue:number;targetValue:number}>;warnings?:Array<{id:number;level:string;summary:string;studentName:string}>}>({});
const query = ref('');
/** 全部业务页面都有独立组件；参考复原视图里的少量入口仍走占位页面。 */
const DEDICATED_PAGES = ['概览','学生档案','成长画像','机智币','智能预警','一人一策','六机任务','班级诊改','教师资料','智小匠助手'];
const businessPages = [...DEDICATED_PAGES];
const initialPage = new URLSearchParams(location.search).get('page') || '概览';
const active = ref(businessPages.includes(initialPage) ? initialPage : '概览');
const dialog = ref<HTMLDialogElement>();
const t = (reference: string, adapted: string) => school.value ? adapted : reference;
const groups = computed(() => [
  { label: t('聊天', '育人助手'), items: [{ label: t('调试台', '智小匠助手'), icon: FlaskConical }] },
  { label: t('常规', '班级治理'), items: [
    { label: '概览', icon: Activity }, { label: t('数据看板', '学生档案'), icon: LayoutDashboard },
    { label: t('模型状态', '成长画像'), icon: Radar }, { label: t('访问密钥', '机智币'), icon: school.value ? Coins : KeyRound },
    { label: t('使用日志', '智能预警'), icon: FileText }, { label: t('任务日志', '六机任务'), icon: ListTodo },
  ] },
  { label: t('个人', '持续诊改'), items: [
    { label: t('计费中心', '一人一策'), icon: school.value ? GraduationCap : Wallet },
    { label: t('我的工单', '班级诊改'), icon: MessagesSquare }, { label: t('个人资料', '教师资料'), icon: UserRound },
  ] },
]);
const steps = computed(() => [
  { title: t('创建访问密钥', '查看成长数据'), text: t('为你的应用或服务创建密钥', '汇集学业、行为与技能成长记录'), icon: school.value ? UsersRound : KeyRound, complete: false },
  { title: t('添加额度', '开展辅助研判'), text: t('生产流量前保持充足余额', '结合数据趋势，了解学生近期状态'), icon: school.value ? Radar : CreditCard, complete: true },
  { title: t('发送请求', '制定帮扶计划'), text: t('使用 Playground 或你的客户端验证路由', '教师确认后实施，持续跟踪成长变化'), icon: school.value ? ClipboardCheck : SquareTerminal, complete: false },
]);
/** 推荐操作直接进入对应业务页面。 */
const actions = computed(() => [
  { title: t('访问密钥', '智能预警'), page: '智能预警', description: t('为你的应用或服务创建密钥', '查看成长趋势与待关注事项'), icon: school.value ? Radar : KeyRound },
  { title: t('使用日志', '一人一策'), page: '一人一策', description: t('查看请求、错误和计费详情', '审核帮扶建议，记录实施过程'), icon: FileText },
  { title: t('方案与计费', '六机任务'), page: '六机任务', description: t('扩展流量前查看模型费率', '查看活动安排与学生完成情况'), icon: BookOpen },
]);
const stats = computed(() => [
  { label: t('近 24 小时消耗', '班级人数'), value: t('$0', dashboard.value.metrics?.studentCount == null ? '未连接' : String(dashboard.value.metrics.studentCount)), description: t('近 24 小时消耗量 (USD)', '在籍学生，来自班级驾驶舱'), icon: school.value ? UsersRound : Flame, tone: 'orange' },
  { label: t('历史使用情况', '班级成长指数'), value: t('$0', classTargetValue.value), description: t('总消耗 (USD)', '班级诊改指标「数学及格率」当前值'), icon: TrendingUp, tone: 'green' },
  { label: t('请求计数', '今日出勤率'), value: t('0', dashboard.value.metrics?.activeRate == null ? '数据不足' : `${dashboard.value.metrics.activeRate}%`), description: t('总请求数', attendanceDescription.value), icon: Activity, tone: 'blue' },
]);
const classTargetValue = computed(() => {
  const target = dashboard.value.targets?.find(x => x.name === '数学及格率');
  return target?.currentValue == null ? '数据不足' : `${target.currentValue}%`;
});
const attendanceDescription = computed(() => {
  const metrics = dashboard.value.metrics;
  if (!metrics?.attendanceDate) return '尚未登记出勤';
  return `${metrics.attendanceDate} 已登记 ${metrics.attendanceCompleteness ?? 0} 人`;
});
/** 概览的待关注事项取真实预警，没有预警时显示空状态，不展示示例数据。 */
const firstWarning = computed(() => dashboard.value.warnings?.[0]);
const searchResults = computed(() => groups.value.flatMap(g => g.items).filter(i => i.label.includes(query.value.trim())));
async function open(title: string) { panel.value = title; query.value = ''; await nextTick(); dialog.value?.showModal(); if (title === '搜索') dialog.value?.querySelector('input')?.focus(); }
function close() { dialog.value?.close(); panel.value = ''; }
function choose(title: string) { close(); active.value = title; }
const apiBase = (import.meta.env.VITE_API_BASE || 'http://127.0.0.1:8081/api/v1').replace(/\/$/, '');
async function apiGet(path: string) { const res = await fetch(`${apiBase}${path}`, { credentials: 'include' }); if (!res.ok) throw new Error(`HTTP ${res.status}`); const body = await res.json(); return body.data ?? {}; }
async function loadCsrf() { try { const d = await apiGet('/auth/csrf'); csrfToken.value = d.token || ''; } catch { /* backend may be offline while the visual preview is used */ } }
function keys(e: KeyboardEvent) { if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); open('搜索'); } }
async function loadDashboard() { try { const res = await fetch(`${apiBase}/dashboard/class`, { credentials: 'include' }); if (!res.ok) throw new Error('backend unavailable'); const body = await res.json(); dashboard.value = body.data ?? {}; live.value = true; } catch { liveError.value = '后端未连接，当前显示演示数据'; } }
async function signIn() { signingIn.value = true; loginError.value = ''; try { const res = await fetch(`${apiBase}/auth/login`, { method: 'POST', credentials: 'include', headers: {'Content-Type':'application/json'}, body: JSON.stringify({username: username.value, password: password.value}) }); if (!res.ok) throw new Error('登录失败'); location.href = '/?view=school'; } catch { loginError.value = '暂时无法登录，请检查后端服务或账号密码'; } finally { signingIn.value = false; } }
async function logout() { try { if (!csrfToken.value) await loadCsrf(); const headers: Record<string,string> = {}; if (csrfToken.value) headers['X-CSRF-TOKEN'] = csrfToken.value; await fetch(`${apiBase}/auth/logout`, { method: 'POST', credentials: 'include', headers }); } catch { /* allow local preview logout when backend is offline */ } finally { location.href = '/login'; } }
onMounted(() => { document.addEventListener('keydown', keys); loadCsrf(); loadDashboard(); });
watch(active, () => { const url = new URL(location.href); url.searchParams.set('page', active.value); history.replaceState(null, '', url); });
onUnmounted(() => document.removeEventListener('keydown', keys));
</script>

<template>
  <div v-if="loginPage" class="login-shell"><section class="login-card panel"><div class="login-brand"><img src="/favicon.svg" alt=""/><strong>智小匠</strong></div><p class="eyebrow">班主任工作台</p><h1>欢迎回来</h1><p class="login-copy">登录后继续管理班级成长与帮扶闭环。</p><form @submit.prevent="signIn"><label>账号<input v-model="username" autocomplete="username" required /></label><label>密码<input v-model="password" type="password" autocomplete="current-password" required /></label><p v-if="loginError" class="login-error">{{ loginError }}</p><button class="button primary login-submit" :disabled="signingIn">{{ signingIn ? '登录中…' : '进入工作台' }}<ArrowRight/></button></form><small>演示账号：teacher / password</small></section></div>
  <div v-else class="application" :class="{ collapsed }">
    <header class="topbar">
      <div class="brand-area">
        <button class="icon-button sidebar-toggle" aria-label="切换侧栏" :aria-expanded="!collapsed" @click="collapsed = !collapsed"><PanelLeft /></button>
        <a class="brand" href="#" @click.prevent="active = '概览'">
          <img src="/favicon.svg" alt="" /> <strong>{{ t('控制台', '智小匠') }}</strong>
        </a>
      </div>
      <button class="search-button" @click="open('搜索')"><Search /><span>搜索</span><kbd>⌘ K</kbd></button>
      <div class="top-tools">
        <button class="icon-button notification" aria-label="通知" @click="open('通知')"><Bell /></button>
        <button class="icon-button optional-tool" aria-label="语言" @click="open('语言')"><Languages /></button>
        <button class="avatar" aria-label="个人资料" @click="school ? choose('教师资料') : open('个人资料')">{{ t('J', '师') }}</button>
      </div>
    </header>

    <aside class="sidebar" aria-label="侧边导航">
      <section v-for="group in groups" :key="group.label" class="menu-group">
        <p class="group-label">{{ group.label }}</p>
        <button v-for="item in group.items" :key="item.label" class="menu-item" :class="{ selected: active === item.label }" :title="item.label" @click="choose(item.label)">
          <component :is="item.icon" /><span>{{ item.label }}</span>
        </button>
      </section>
      <button class="view-switch logout-switch" @click="logout"><UserRound /><span>退出登录</span></button>
    </aside>

    <main class="canvas">
      <div v-if="active === '概览'" class="page-scroll">
        <div class="page-heading"><h1>概览</h1><PixelPet /><span v-if="school" class="demo-tag">{{ live ? 'Oracle 数据' : '未连接后端' }}</span><span v-if="liveError" class="connection-note">{{ liveError }}</span></div>

        <section class="intro-grid">
          <div class="onboarding panel">
            <div class="onboarding-copy">
              <div class="eyebrow"><ListTodo />{{ t('开始使用', '成长工作台') }}</div>
              <h2>{{ t('几分钟内开始使用你的控制台', '让每一位学生的成长被看见') }}</h2>
              <p class="intro-description">{{ t('集中展示密钥、余额、路由和服务状态。', '汇集成长数据、智能研判与精准帮扶。') }}</p>
              <div class="guide-actions">
                <button class="button" @click="guide = !guide"><ChevronUp v-if="guide"/><ChevronDown v-else/>{{ guide ? '隐藏' : '显示' }}{{ t('设置引导', '成长引导') }}</button>
                <button class="button primary" @click="school ? choose('成长画像') : open('创建访问密钥')"><component :is="school ? ClipboardCheck : KeyRound"/>{{ t('创建访问密钥', '记录学生成长') }}</button>
              </div>
              <div v-if="guide" class="steps">
                <div v-for="(step, index) in steps" :key="step.title" class="step">
                  <div class="step-rail"><span :class="{ complete: step.complete }"><Check v-if="step.complete"/><Circle v-else/></span></div>
                  <button class="step-card" @click="school ? choose(['成长画像','智能预警','一人一策'][index] || '概览') : open(step.title)">
                    <span class="icon-tile"><component :is="step.icon"/></span>
                    <span class="step-content"><strong><em>{{ index + 1 }}.</em> {{ step.title }}</strong><small>{{ step.text }}</small></span><ArrowRight class="step-arrow"/>
                  </button>
                </div>
              </div>
              <div v-else class="collapsed-guide"><Check />{{ t('设置引导已收起，随时可以再次展开。', '成长引导已收起，继续查看班级状态。') }}</div>
            </div>

            <div class="request-card">
              <div class="request-header"><span class="icon-tile blue"><component :is="school ? GraduationCap : SquareTerminal"/></span><div><h3>{{ t('首个 API 请求', '小智 · 成长关注') }}</h3><p>{{ t('创建访问密钥以解锁真实请求', '近期学业、活动参与同步下降') }}</p></div><button class="button compact" @click="open(t('访问密钥', '学生成长画像'))">{{ t('访问密钥', '查看画像') }}</button></div>
              <div v-if="!school" class="code-panel"><div class="traffic-lights"><i/><i/><i/></div><pre>curl https://api.example.com/v1/chat/completions \
-H "Content-Type: application/json" \
-H "Authorization: Bearer $API_KEY" \
-d '{"model":"example-model","messages":...}'</pre></div>
              <div v-else class="student-trend">
                <template v-if="firstWarning">
                  <div class="trend-heading"><span>{{ firstWarning.studentName }} · 规则提示</span><span class="muted">{{ firstWarning.level }}</span></div>
                  <p class="trend-summary">{{ firstWarning.summary }}</p>
                </template>
                <p v-else class="muted">当前没有待关注事项，规则筛查结果会显示在这里。</p>
              </div>
              <div class="status-list">
                <div><span class="mini-icon blue"><RadioTower /></span><strong>{{ t('路由已启用', '出勤登记') }}</strong><span>{{ t('在线', dashboard.metrics?.attendanceDate ? `${dashboard.metrics.attendanceDate} · ${dashboard.metrics.attendanceCompleteness ?? 0} 人` : '尚未登记') }}</span></div>
                <div><span class="mini-icon green"><ShieldCheck /></span><strong>{{ t('认证已配置', '待研判预警') }}</strong><span>{{ t('需要 API 密钥', `${dashboard.warnings?.length ?? 0} 条`) }}</span></div>
                <div><span class="mini-icon pink"><Timer /></span><strong>{{ t('已选择模型', '班级成长指数') }}</strong><span>{{ t('example-model', classTargetValue) }}</span></div>
              </div>
              <p v-if="school" class="ai-note">AI 辅助建议，仅供教师参考。</p>
            </div>
          </div>

          <aside class="recommend panel"><p class="eyebrow">推荐操作</p><h2>{{ t('保持平台就绪', '关注每一步成长') }}</h2><div class="recommend-list"><button v-for="action in actions" :key="action.title" @click="school ? choose(action.page) : open(action.title)"><span class="icon-tile"><component :is="action.icon"/></span><span><strong>{{ action.title }}</strong><small>{{ action.description }}</small></span></button></div></aside>
        </section>

        <section class="usage panel">
          <div class="usage-main"><h2>{{ t('用量概览', '班级概览') }}</h2><p class="section-description">{{ t('监控余额、用量和请求量', '关注出勤、成长与任务完成情况') }}</p><div class="stats-grid"><article v-for="stat in stats" :key="stat.label" class="stat-card" :class="stat.tone"><div class="stat-label"><span class="mini-icon" :class="stat.tone"><component :is="stat.icon"/></span>{{ stat.label }}</div><strong class="stat-value">{{ stat.value }}</strong><p>{{ stat.description }}</p><div class="stat-line"/></article></div></div>
          <aside class="balance"><div class="balance-label"><span>{{ t('剩余额度', '本月任务完成率') }}</span><span class="health"><i/>{{ t('正常', '稳步推进') }}</span></div><strong class="balance-value">{{ t('$10', '89%') }}</strong><div class="balance-mini"><div><span><Flame/>{{ t('近 24 小时消耗', '待关注学生') }}</span><strong>{{ t('$0', '4 人') }}</strong></div><div><span><ShieldCheck/>{{ t('可用时长', '本月成长变化') }}</span><strong>{{ t('暂无使用记录', '+4.3') }}</strong></div></div><button class="button primary balance-action" @click="open(t('计费中心', '六机成长任务'))">{{ t('计费中心', '查看成长任务') }}<ArrowRight/></button></aside>
        </section>
        <footer class="preview-footer"><span>智小匠工作台 · {{ live ? '实时数据' : '未连接后端' }}</span></footer>
      </div>
      <StudentRecords v-else-if="school && active === '学生档案'" />
      <PointsPage v-else-if="school && active === '机智币'" />
      <TeacherProfile v-else-if="school && active === '教师资料'" />
      <GrowthPage v-else-if="school && active === '成长画像'" />
      <WarningsPage v-else-if="school && active === '智能预警'" />
      <InterventionPage v-else-if="school && active === '一人一策'" />
      <TasksPage v-else-if="school && active === '六机任务'" />
      <DiagnosisPage v-else-if="school && active === '班级诊改'" />
      <AssistantPage v-else-if="school && active === '智小匠助手'" />
      <div v-else class="page-scroll workspace-page">
        <div class="workspace-heading"><div><p class="eyebrow"><Sparkles/>参考复原视图</p><h1>{{ active }}</h1><p>该入口只出现在参考对照视图，用于布局与视觉比对，不承载业务数据。</p></div></div>
        <section class="workspace-note panel"><span class="icon-tile orange"><ShieldCheck/></span><div><strong>怎么看到业务数据</strong><p>切换到智小匠视图后，各页面均读写真实数据并保留操作者与时间记录。</p></div></section>
      </div>
    </main>

    <dialog ref="dialog" @click="(e) => e.target === dialog && close()" @close="panel = ''">
      <div class="dialog-heading"><h2>{{ panel }}</h2><button class="icon-button" aria-label="关闭" @click="close"><X/></button></div>
      <template v-if="panel === '搜索'"><label class="dialog-search"><Search/><input v-model="query" autofocus placeholder="搜索页面与操作" aria-label="搜索页面与操作"/></label><div class="search-results"><button v-for="result in searchResults" :key="result.label" @click="choose(result.label)"><component :is="result.icon"/>{{ result.label }}<ArrowRight/></button><p v-if="!searchResults.length" class="muted">没有找到匹配页面</p></div></template>
      <template v-else-if="panel === '通知'"><div class="notice-row"><span class="mini-icon blue"><ShieldCheck/></span><div><strong>数据连接状态</strong><p>{{ live ? '已连接 Oracle 数据服务。' : '当前使用演示数据，保存操作需要后端在线。' }}</p></div></div></template>
      <template v-else><div class="placeholder-icon"><component :is="school ? GraduationCap : LayoutDashboard"/></div><p class="dialog-description">这是「{{ panel }}」的操作入口。保存后将由后端写入 MySQL，并保留操作者与时间记录。</p><button class="button primary dialog-done" @click="close">返回概览</button></template>
    </dialog>
  </div>
</template>
