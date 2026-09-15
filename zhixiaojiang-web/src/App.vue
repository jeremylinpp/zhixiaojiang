<script setup lang="ts">
import PixelPet from './components/PixelPet.vue';
import StudentRecords from './components/StudentRecords.vue';
import PointsPage from './components/PointsPage.vue';
import TeacherProfile from './components/TeacherProfile.vue';
import GrowthPage from './components/GrowthPage.vue';
import WarningsPage from './components/WarningsPage.vue';
import InterventionPage from './components/InterventionPage.vue';
import { computed, ref, nextTick, onMounted, onUnmounted, watch } from 'vue';
import { PanelLeft, Search, Bell, Languages, Palette, FlaskConical, Activity, LayoutDashboard, Radar, KeyRound, FileText, ListTodo, Wallet, MessagesSquare, UserRound, ArrowRight, ChevronUp, ChevronDown, Check, Circle, CreditCard, SquareTerminal, RadioTower, ShieldCheck, Timer, BookOpen, Flame, TrendingUp, X, UsersRound, GraduationCap, Coins, Target, ClipboardCheck, Sparkles } from 'lucide-vue-next';

/** 参考控制台对照视图只在本地开发可用；生产构建固定进入智小匠业务视图。 */
const referenceAvailable = import.meta.env.DEV;
const school = ref(!referenceAvailable || new URLSearchParams(location.search).get('view') !== 'reference');
const loginPage = ref(location.pathname === '/login');
const username = ref('teacher');
const password = ref('password');
const loginError = ref('');
const signingIn = ref(false);
const csrfToken = ref('');
const actionBusy = ref(false);
const actionError = ref('');
const formTitle = ref('');
const formNote = ref('');
const formAmount = ref(1);
const formDate = ref('');
const collapsed = ref(false);
const guide = ref(true);
const panel = ref('');
const live = ref(false);
const liveError = ref('');
const dashboard = ref<{metrics?:{studentCount?:number;activeRate?:number|null;attendanceCompleteness?:number;attendanceDate?:string|null};targets?:Array<{name:string;currentValue:number;targetValue:number}>}>({});
const pageRows = ref<Record<string, string[][]>>({});
const query = ref('');
const businessPages = ['概览','学生档案','成长画像','机智币','智能预警','一人一策','六机任务','班级诊改','智小匠助手','教师资料'];
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
const actions = computed(() => [
  { title: t('访问密钥', '智能预警'), description: t('为你的应用或服务创建密钥', '查看成长趋势与待关注事项'), icon: school.value ? Radar : KeyRound },
  { title: t('使用日志', '一人一策'), description: t('查看请求、错误和计费详情', '审核帮扶建议，记录实施过程'), icon: FileText },
  { title: t('方案与计费', '六机任务'), description: t('扩展流量前查看模型费率', '查看活动安排与学生完成情况'), icon: BookOpen },
]);
const stats = computed(() => [
  { label: t('近 24 小时消耗', '班级人数'), value: t('$0', String(dashboard.value.metrics?.studentCount ?? 42)), description: t('近 24 小时消耗量 (USD)', '2025 级工业机器人应用与维护班'), icon: school.value ? UsersRound : Flame, tone: 'orange' },
  { label: t('历史使用情况', '班级成长指数'), value: t('$0', String(dashboard.value.targets?.find(x => x.name === '数学及格率')?.currentValue ?? 82.6)), description: t('总消耗 (USD)', live.value ? '已从 Oracle 数据服务读取' : '较上月提升 4.3 · 演示数据'), icon: TrendingUp, tone: 'green' },
  { label: t('请求计数', '今日出勤率'), value: t('0', dashboard.value.metrics?.activeRate == null ? '数据不足' : `${dashboard.value.metrics.activeRate}%`), description: t('总请求数', dashboard.value.metrics?.attendanceCompleteness ? `已登记 ${dashboard.value.metrics.attendanceCompleteness} 人` : '尚未完成登记'), icon: Activity, tone: 'blue' },
]);
const searchResults = computed(() => groups.value.flatMap(g => g.items).filter(i => i.label.includes(query.value.trim())));
async function open(title: string) { panel.value = title; query.value = ''; actionError.value = ''; formTitle.value = ''; formNote.value = ''; formAmount.value = 1; formDate.value = ''; await nextTick(); dialog.value?.showModal(); if (title === '搜索') dialog.value?.querySelector('input')?.focus(); }
function close() { dialog.value?.close(); panel.value = ''; }
function choose(title: string) { close(); active.value = title; }
const pageInfo = computed(() => ({
  '学生档案': ['学生档案', '42 名学生的成长资料与阶段变化', ['姓名','成长指数','最新记录','状态'], [['小智','82','数学成绩连续下降','重点关注'],['陈同学','88','机器人实训完成','正常'],['周同学','79','职业规划待完成','关注'],['林同学','84','暂无最新记录','正常'],...Array.from({length:38},(_,i)=>[`演示学生${String(i+5).padStart(2,'0')}`,'—','暂无最新记录','正常'])]],
  '成长画像': ['成长画像', '品德、技能、思维、智行四维成长状态', ['学生','品德','技能','思维','智行'], [['小智','88','91','72','78'],['陈同学','92','84','86','90'],['周同学','83','82','76','80']]],
  '机智币': ['机智币', '过程性评价与正向激励流水', ['时间','学生','行为','变化'], [['09-08','小智','完成实训安全测验','+2'],['09-06','陈同学','班级贡献','+2'],['09-05','小智','迟到','-1']]],
  '智能预警': ['智能预警', '规则筛查成长趋势，教师完成最终研判', ['等级','学生','提示','状态'], [['重点关注','小智','学业、活动与行为同步下降','待研判'],['关注','周同学','职业规划任务未完成','开放'],['正常','陈同学','近四周状态稳定','已关闭']]],
  '一人一策': ['一人一策', '教师审核帮扶建议并记录执行过程', ['学生','方案','状态','下次复评'], [['小智','阶段学习支持计划','执行中','09-15'],['周同学','职业路径启蒙计划','草稿','09-20']]],
  '六机任务': ['六机任务', '铸机魂、立机规、淬机质、铺机路、聚机力、调机态', ['模块','任务','截止日期','奖励'], [['铸机魂','名匠故事研读','09-20','+2'],['立机规','实训安全规范','09-18','+2'],['淬机质','AI 创意小赛','09-30','+5']]],
  '班级诊改': ['班级诊改', '目标—标准—计划—实施—监测—诊断—改进—优化', ['目标','当前值','目标值','偏差'], [['数学及格率','76%','85%','-9%'],['职业规划清晰度','78%','80%','-2%'],['任务完成率','89%','90%','-1%']]],
  '智小匠助手': ['智小匠助手', '以数据为依据的班级育人辅助入口', ['能力','当前状态','说明'], [['成长分析','可用','基于已录入的成长记录'],['趋势预警','可用','规则引擎每日筛查'],['AI 建议','模板模式','配置模型服务后启用']]],
} as Record<string,[string,string,string[],string[][]]>));
const apiBase = (import.meta.env.VITE_API_BASE || 'http://127.0.0.1:8081/api/v1').replace(/\/$/, '');
const pageSearch = ref('');
const rowsForActive = computed(() => (pageRows.value[active.value] || pageInfo.value[active.value]?.[3] || []).filter(row => row.some(cell => String(cell ?? '').includes(pageSearch.value.trim()))));
async function apiGet(path: string) { const res = await fetch(`${apiBase}${path}`, { credentials: 'include' }); if (!res.ok) throw new Error(`HTTP ${res.status}`); const body = await res.json(); return body.data ?? {}; }
async function loadCsrf() { try { const d = await apiGet('/auth/csrf'); csrfToken.value = d.token || ''; } catch { /* backend may be offline while the visual preview is used */ } }
async function loadPage() {
  const requestedPage = active.value;
  if (!school.value || ['概览','学生档案','成长画像','机智币','教师资料','智能预警','一人一策'].includes(requestedPage)) return;
  try {
    if (requestedPage === '学生档案') { const d = await apiGet('/students?page=1&pageSize=50'); pageRows.value[requestedPage] = (d.items || []).map((x:any) => [x.name, String(x.growthIndex ?? '—'), '成长档案已建立', x.status === 'ACTIVE' ? '正常' : '已归档']); }
    if (requestedPage === '成长画像') { const d = await apiGet('/students/1'); const values = Object.fromEntries((d.growth || []).map((x:any) => [x.dimension, x.score])); pageRows.value[requestedPage] = [['小智',String(values.MORAL ?? '—'),String(values.SKILL ?? '—'),String(values.THINKING ?? '—'),String(values.SMART ?? '—')]]; }
    if (requestedPage === '智能预警') { const d = await apiGet('/warnings'); pageRows.value[requestedPage] = (d.items || []).map((x:any) => [x.level === 'FOCUS' ? '重点关注' : '关注', x.studentName, x.summary, x.status === 'OPEN' ? '待研判' : '已关闭']); }
    if (requestedPage === '一人一策') { const d = await apiGet('/interventions'); pageRows.value[requestedPage] = (d.items || []).map((x:any) => [x.studentName, x.title, x.status === 'CONFIRMED' ? '已确认' : x.status === 'IN_PROGRESS' ? '执行中' : x.status, x.reviewAt || '待安排']); }
    if (requestedPage === '六机任务') { const d = await apiGet('/growth-tasks'); pageRows.value[requestedPage] = (d.items || []).map((x:any) => [x.module, x.title, String(x.dueOn || '').slice(5), `+${x.pointReward}`]); }
    if (requestedPage === '班级诊改') { const d = await apiGet('/class-diagnoses'); pageRows.value[requestedPage] = (d.items || []).map((x:any) => [x.name, `${x.currentValue}%`, `${x.targetValue}%`, `${x.deviation > 0 ? '+' : ''}${x.deviation}%`]); }
  } catch { liveError.value = '后端暂时不可用，当前页面保留演示数据'; }
}
async function runWarningAnalysis() { try { if (!csrfToken.value) await loadCsrf(); const res = await fetch(`${apiBase}/warnings/analyze`, { method: 'POST', credentials: 'include', headers: csrfToken.value ? {'X-CSRF-TOKEN': csrfToken.value} : {} }); if (!res.ok) throw new Error(); const body = await res.json(); await loadPage(); panel.value = `规则分析完成 · ${body.data?.created ?? 0} 条新增`; await nextTick(); dialog.value?.showModal(); } catch { panel.value = '规则分析暂不可用'; await nextTick(); dialog.value?.showModal(); } }
async function saveAction() { actionBusy.value = true; actionError.value = ''; try { if (!csrfToken.value) await loadCsrf(); const headers: Record<string,string> = {'Content-Type':'application/json'}; if (csrfToken.value) headers['X-CSRF-TOKEN'] = csrfToken.value; let path = ''; let body: Record<string,unknown> = {}; if (panel.value === '新增学生') { path='/students'; body={name:formTitle.value||'未命名学生',gender:formNote.value||undefined}; } else if (panel.value === '新增积分记录') { path='/points'; body={studentId:1,amount:Number(formAmount.value),category:'MANUAL',reason:formNote.value||'教师手工调整',idempotencyKey:`web-${Date.now()}`}; } else if (panel.value === '发布成长任务') { path='/growth-tasks'; body={module:'聚机力',title:formTitle.value||'班级成长任务',description:formNote.value,dueOn:formDate.value||undefined,pointReward:Number(formAmount.value)}; } else if (panel.value === '新增诊改目标') { path='/class-diagnoses'; body={name:formTitle.value||'班级成长目标',targetValue:Number(formAmount.value),currentValue:0,unit:'%'}; } else if (panel.value === '生成帮扶方案') { path='/interventions'; body={studentId:1,title:formTitle.value||'阶段成长支持方案',teacherNote:formNote.value}; } else { path='/students/1/growth'; body={dimension:'SMART',score:Number(formAmount.value),title:formTitle.value||'教师成长记录',detail:formNote.value,occurredOn:formDate.value||undefined,source:'教师录入'}; } const res = await fetch(`${apiBase}${path}`,{method:'POST',credentials:'include',headers,body:JSON.stringify(body)}); if (!res.ok) throw new Error(); close(); await Promise.all([loadPage(),loadDashboard()]); } catch { actionError.value = '保存失败，请检查后端连接与表单内容'; } finally { actionBusy.value = false; } }
function pageAction(title: string) { if (title === '智能预警') { runWarningAnalysis(); return; } open(title === '学生档案' ? '新增学生' : title === '一人一策' ? '生成帮扶方案' : title === '机智币' ? '新增积分记录' : title === '六机任务' ? '发布成长任务' : title === '班级诊改' ? '新增诊改目标' : '新增记录'); }
function switchView() { if (!referenceAvailable) return; school.value = !school.value; active.value = '概览'; history.replaceState(null, '', school.value ? '?view=school' : '?view=reference'); }
function keys(e: KeyboardEvent) { if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); open('搜索'); } }
async function loadDashboard() { try { const res = await fetch(`${apiBase}/dashboard/class`, { credentials: 'include' }); if (!res.ok) throw new Error('backend unavailable'); const body = await res.json(); dashboard.value = body.data ?? {}; live.value = true; } catch { liveError.value = '后端未连接，当前显示演示数据'; } }
async function signIn() { signingIn.value = true; loginError.value = ''; try { const res = await fetch(`${apiBase}/auth/login`, { method: 'POST', credentials: 'include', headers: {'Content-Type':'application/json'}, body: JSON.stringify({username: username.value, password: password.value}) }); if (!res.ok) throw new Error('登录失败'); location.href = '/?view=school'; } catch { loginError.value = '暂时无法登录，请检查后端服务或账号密码'; } finally { signingIn.value = false; } }
async function logout() { try { if (!csrfToken.value) await loadCsrf(); const headers: Record<string,string> = {}; if (csrfToken.value) headers['X-CSRF-TOKEN'] = csrfToken.value; await fetch(`${apiBase}/auth/logout`, { method: 'POST', credentials: 'include', headers }); } catch { /* allow local preview logout when backend is offline */ } finally { location.href = '/login'; } }
onMounted(() => { document.addEventListener('keydown', keys); loadCsrf(); loadDashboard(); loadPage(); });
watch(active, () => { pageSearch.value = ''; const url = new URL(location.href); url.searchParams.set('page',active.value); history.replaceState(null,'',url); loadPage(); });
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
      <nav class="top-nav" aria-label="主导航">
        <button v-for="label in (school ? ['首页', '工作台', '学生成长', '六机任务', '帮助', '关于'] : ['主页', '控制台', '功能中心', '公告板', '文档', '关于'])" :key="label" @click="label === '学生成长' ? choose('学生档案') : label === '六机任务' ? choose('六机任务') : ['首页', '工作台', '主页', '控制台'].includes(label) ? active = '概览' : open(label)">{{ label }}</button>
      </nav>
      <button class="search-button" @click="open('搜索')"><Search /><span>搜索</span><kbd>⌘ K</kbd></button>
      <div class="top-tools">
        <button class="icon-button notification" aria-label="通知，2条" @click="open('通知')"><Bell /><span>2</span></button>
        <button class="icon-button optional-tool" aria-label="语言" @click="open('语言')"><Languages /></button>
        <button v-if="referenceAvailable" class="icon-button" aria-label="视觉版本" @click="open('视觉版本')"><Palette /></button>
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
      <button v-if="referenceAvailable" class="view-switch" @click="switchView"><Palette /><span>{{ school ? '查看参考复原' : '切换智小匠' }}</span></button>
    </aside>

    <main class="canvas">
      <div v-if="active === '概览'" class="page-scroll">
        <div class="page-heading"><h1>概览</h1><PixelPet /><span v-if="school" class="demo-tag">{{ live ? 'Oracle 数据' : '演示数据' }}</span><span v-if="liveError" class="connection-note">{{ liveError }}</span></div>

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
              <div v-else class="student-trend"><div class="trend-heading"><span>数学成绩 · 最近四次</span><span class="muted">趋势证据</span></div><div class="trend-scores"><span>78</span><span>70</span><span>63</span><span>58</span></div><svg viewBox="0 0 300 60" aria-label="数学成绩连续下降"><path d="M10 8L104 24L198 40L290 52" fill="none" stroke="#df795d" stroke-width="2.5"/><g fill="#df795d"><circle cx="10" cy="8" r="3"/><circle cx="104" cy="24" r="3"/><circle cx="198" cy="40" r="3"/><circle cx="290" cy="52" r="3"/></g></svg></div>
              <div class="status-list">
                <div><span class="mini-icon blue"><RadioTower /></span><strong>{{ t('路由已启用', '成长数据已更新') }}</strong><span>{{ t('在线', '今日') }}</span></div>
                <div><span class="mini-icon green"><ShieldCheck /></span><strong>{{ t('认证已配置', '教师人工研判') }}</strong><span>{{ t('需要 API 密钥', '待确认') }}</span></div>
                <div><span class="mini-icon pink"><Timer /></span><strong>{{ t('已选择模型', '下一次成长复评') }}</strong><span>{{ t('example-model', '两周后') }}</span></div>
              </div>
              <p v-if="school" class="ai-note">AI 辅助建议，仅供教师参考。</p>
            </div>
          </div>

          <aside class="recommend panel"><p class="eyebrow">推荐操作</p><h2>{{ t('保持平台就绪', '关注每一步成长') }}</h2><div class="recommend-list"><button v-for="action in actions" :key="action.title" @click="open(action.title)"><span class="icon-tile"><component :is="action.icon"/></span><span><strong>{{ action.title }}</strong><small>{{ action.description }}</small></span></button></div></aside>
        </section>

        <section class="usage panel">
          <div class="usage-main"><h2>{{ t('用量概览', '班级概览') }}</h2><p class="section-description">{{ t('监控余额、用量和请求量', '关注出勤、成长与任务完成情况') }}</p><div class="stats-grid"><article v-for="stat in stats" :key="stat.label" class="stat-card" :class="stat.tone"><div class="stat-label"><span class="mini-icon" :class="stat.tone"><component :is="stat.icon"/></span>{{ stat.label }}</div><strong class="stat-value">{{ stat.value }}</strong><p>{{ stat.description }}</p><div class="stat-line"/></article></div></div>
          <aside class="balance"><div class="balance-label"><span>{{ t('剩余额度', '本月任务完成率') }}</span><span class="health"><i/>{{ t('正常', '稳步推进') }}</span></div><strong class="balance-value">{{ t('$10', '89%') }}</strong><div class="balance-mini"><div><span><Flame/>{{ t('近 24 小时消耗', '待关注学生') }}</span><strong>{{ t('$0', '4 人') }}</strong></div><div><span><ShieldCheck/>{{ t('可用时长', '本月成长变化') }}</span><strong>{{ t('暂无使用记录', '+4.3') }}</strong></div></div><button class="button primary balance-action" @click="open(t('计费中心', '六机成长任务'))">{{ t('计费中心', '查看成长任务') }}<ArrowRight/></button></aside>
        </section>
        <footer class="preview-footer"><span>{{ referenceAvailable ? '视觉复原预览 · ' : '' }}{{ school ? '智小匠业务适配' : '参考控制台' }} · {{ live ? '实时数据' : '演示数据' }}</span><button v-if="referenceAvailable" @click="switchView">{{ school ? '查看参考复原' : '切换智小匠视图' }}<ArrowRight/></button></footer>
      </div>
      <StudentRecords v-else-if="school && active === '学生档案'" />
      <PointsPage v-else-if="school && active === '机智币'" />
      <TeacherProfile v-else-if="school && active === '教师资料'" />
      <GrowthPage v-else-if="school && active === '成长画像'" />
      <WarningsPage v-else-if="school && active === '智能预警'" />
      <InterventionPage v-else-if="school && active === '一人一策'" />
      <div v-else class="page-scroll workspace-page">
        <div class="workspace-heading"><div><p class="eyebrow"><Sparkles/>班主任工作台</p><h1>{{ pageInfo[active]?.[0] || active }}</h1><p>{{ pageInfo[active]?.[1] || '持续记录学生成长，保持班级运行可见。' }}</p></div><button class="button primary" @click="pageAction(active)"><Sparkles/>{{ active === '智能预警' ? '执行规则分析' : active === '学生档案' ? '新增学生' : active === '一人一策' ? '生成帮扶方案' : active === '机智币' ? '新增积分记录' : active === '六机任务' ? '发布成长任务' : active === '班级诊改' ? '新增诊改目标' : '新增记录' }}</button></div>
        <div class="workspace-toolbar"><label class="inline-search"><Search/><input v-model="pageSearch" placeholder="搜索当前页面"/><kbd>⌘ K</kbd></label><span class="result-count">{{ rowsForActive.length }} 条记录 · {{ live ? '已连接 Oracle' : '演示数据' }}</span></div>
        <section class="data-card panel"><table><thead><tr><th v-for="head in (pageInfo[active]?.[2] || [])" :key="head">{{ head }}</th><th>操作</th></tr></thead><tbody><tr v-for="row in rowsForActive" :key="row.join('-')"><td v-for="cell in row" :key="cell"><span v-if="cell === '重点关注'" class="status-chip danger">{{ cell }}</span><span v-else-if="cell === '关注' || cell === '待研判'" class="status-chip warning">{{ cell }}</span><span v-else-if="cell === '正常' || cell === '已关闭' || cell === '可用'" class="status-chip success">{{ cell }}</span><span v-else>{{ cell }}</span></td><td><button class="table-action" @click="open(row[0] || active)">查看详情 <ArrowRight/></button></td></tr><tr v-if="!rowsForActive.length"><td :colspan="(pageInfo[active]?.[2]?.length || 1) + 1" class="empty-cell">暂无记录</td></tr></tbody></table></section>
        <section class="workspace-note panel"><span class="icon-tile orange"><ShieldCheck/></span><div><strong>数据口径提示</strong><p>{{ active === '智能预警' ? '预警来自规则筛查，最终判断由班主任完成。' : active === '机智币' ? '积分记录独立于四维评价，撤销会生成反向流水。' : '页面数据会在保存后写入 MySQL，并保留操作者与时间记录。' }}</p></div></section>
      </div>
    </main>

    <dialog ref="dialog" @click="(e) => e.target === dialog && close()" @close="panel = ''">
      <div class="dialog-heading"><h2>{{ panel }}</h2><button class="icon-button" aria-label="关闭" @click="close"><X/></button></div>
      <template v-if="panel === '搜索'"><label class="dialog-search"><Search/><input v-model="query" autofocus placeholder="搜索页面与操作" aria-label="搜索页面与操作"/></label><div class="search-results"><button v-for="result in searchResults" :key="result.label" @click="choose(result.label)"><component :is="result.icon"/>{{ result.label }}<ArrowRight/></button><p v-if="!searchResults.length" class="muted">没有找到匹配页面</p></div></template>
      <template v-else-if="panel === '视觉版本'"><p class="dialog-description">{{ referenceAvailable ? '相同的布局、字体和组件，查看两种内容版本。' : '生产构建固定使用智小匠业务视图，参考对照仅在本地开发可用。' }}</p><button v-if="referenceAvailable" class="version-choice" @click="school = false; close()"><Palette/>参考复原<Check v-if="!school"/></button><button class="version-choice" @click="school = true; close()"><GraduationCap/>智小匠<Check v-if="school"/></button></template>
      <template v-else-if="panel === '通知'"><div class="notice-row"><span class="mini-icon orange"><Palette/></span><div><strong>视觉复原预览已就绪</strong><p>顶部调色板可切换参考版与智小匠版。</p></div></div><div class="notice-row"><span class="mini-icon blue"><ShieldCheck/></span><div><strong>数据连接状态</strong><p>{{ live ? '已连接 Oracle 数据服务。' : '当前使用演示数据，保存操作需要后端在线。' }}</p></div></div></template>
      <template v-else-if="['新增学生','新增积分记录','发布成长任务','新增诊改目标','生成帮扶方案','新增记录'].includes(panel)"><form class="action-form" @submit.prevent="saveAction"><label v-if="panel === '新增学生'">姓名<input v-model="formTitle" required placeholder="请输入学生姓名"/></label><label v-else-if="panel !== '新增积分记录'">标题<input v-model="formTitle" required placeholder="请输入标题"/></label><label v-if="panel === '新增学生'">性别（可选）<input v-model="formNote" placeholder="男 / 女"/></label><label v-else-if="panel === '新增积分记录'">积分变化<input v-model.number="formAmount" type="number" required /></label><label v-if="panel === '新增诊改目标'">目标值（%）<input v-model.number="formAmount" type="number" min="0" max="100" required /></label><label v-if="panel === '新增记录'">评分（0–100）<input v-model.number="formAmount" type="number" min="0" max="100" required /></label><label v-if="panel === '发布成长任务' || panel === '新增记录'">日期<input v-model="formDate" type="date" /></label><label v-if="!['新增诊改目标','新增学生'].includes(panel)">说明<textarea v-model="formNote" rows="3" placeholder="补充说明（可选）"></textarea></label><p v-if="actionError" class="login-error">{{ actionError }}</p><button class="button primary dialog-done" :disabled="actionBusy">{{ actionBusy ? '保存中…' : '保存记录' }}</button></form></template>
      <template v-else><div class="placeholder-icon"><component :is="school ? GraduationCap : LayoutDashboard"/></div><p class="dialog-description">这是「{{ panel }}」的操作入口。保存后将由后端写入 MySQL，并保留操作者与时间记录。</p><button class="button primary dialog-done" @click="close">返回概览</button></template>
    </dialog>
  </div>
</template>
