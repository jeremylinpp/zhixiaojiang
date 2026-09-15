<script setup lang="ts">
import {onMounted,ref} from 'vue';
import {request} from '../api';
type Warning={id:number;studentId:number;studentName:string;studentNo:string;level:string;status:string;summary:string;ruleCode:string;teacherNote:string|null;createdAt:string;evidence:unknown[]};
type Event={id:number;action:string;note:string;createdAt:string;actor:string};
type Analysis={source:string;summary:string;evidence?:string[];attentionAreas?:string[];suggestions?:string[];disclaimer?:string};

const LEVEL:Record<string,string>={NORMAL:'正常',ATTENTION:'关注',FOCUS:'重点关注',REVIEW:'人工研判',MANUAL:'人工研判'};
const STATUS:Record<string,string>={OPEN:'待研判',REVIEWED:'已研判',CLOSED:'已关闭'};
const TONE:Record<string,string>={重点关注:'danger',人工研判:'danger',关注:'warning',待研判:'warning',正常:'success',已研判:'success',已关闭:'success'};
const FILTERS=[{value:'OPEN',label:'待研判'},{value:'REVIEWED',label:'已研判'},{value:'CLOSED',label:'已关闭'},{value:'ALL',label:'全部'}];

const items=ref<Warning[]>([]),total=ref(0),page=ref(1),pageSize=20;
const status=ref('OPEN'),query=ref('');
const loading=ref(false),error=ref(''),notice=ref('');
const selected=ref<Warning>(),events=ref<Event[]>([]),detailBusy=ref(false);
const note=ref(''),escalate=ref(false),busy=ref(false),formError=ref('');
const analysis=ref<Analysis>(),analyzing=ref(false),planTitle=ref(''),planBusy=ref(false);
let generation=0;

async function load(){
  const current=++generation;
  loading.value=true;error.value='';
  try{
    const data=await request<{items:Warning[];total:number}>(`/warnings?status=${status.value}&q=${encodeURIComponent(query.value.trim())}&page=${page.value}&pageSize=${pageSize}`);
    if(current!==generation)return;
    items.value=data.items;total.value=data.total;
  }catch(e){if(current===generation)error.value=(e as Error).message;}finally{if(current===generation)loading.value=false;}
}
async function inspect(id:number){
  detailBusy.value=true;error.value='';analysis.value=undefined;note.value='';escalate.value=false;formError.value='';
  try{
    const data=await request<{warning:Warning;events:Event[]}>(`/warnings/${id}`);
    selected.value=data.warning;events.value=data.events;planTitle.value=`${data.warning.studentName}阶段帮扶方案`;
  }catch(e){error.value=(e as Error).message;}finally{detailBusy.value=false;}
}
async function refresh(){items.value=[];await load();if(selected.value){const kept=selected.value.id;await inspect(kept);}}
async function triage(){
  if(!selected.value||busy.value)return;
  busy.value=true;formError.value='';
  try{
    await request(`/warnings/${selected.value.id}/triage`,'POST',{note:note.value.trim(),expectedStatus:selected.value.status,escalate:escalate.value});
    notice.value=escalate.value?'已记录研判并标记为需要人工重点研判。':'已记录教师研判。';
    await refresh();
  }catch(e){formError.value=(e as Error).message;}finally{busy.value=false;}
}
async function closeWarning(){
  if(!selected.value||busy.value)return;
  if(!note.value.trim()){formError.value='请填写关闭原因';return;}
  busy.value=true;formError.value='';
  try{
    await request(`/warnings/${selected.value.id}/close`,'POST',{note:note.value.trim(),expectedStatus:selected.value.status});
    notice.value='预警已关闭，研判与关闭原因保留在过程记录中。';
    await refresh();
  }catch(e){formError.value=(e as Error).message;}finally{busy.value=false;}
}
async function analyze(){
  if(!selected.value||analyzing.value)return;
  analyzing.value=true;formError.value='';
  try{
    analysis.value=await request<Analysis>('/ai/student-analysis','POST',{studentId:selected.value.studentId});
  }catch(e){formError.value=(e as Error).message;}finally{analyzing.value=false;}
}
async function createPlan(){
  if(!selected.value||planBusy.value)return;
  planBusy.value=true;formError.value='';
  try{
    const suggestions=(analysis.value?.suggestions||[]).length?analysis.value!.suggestions:['班主任个别谈话','两周后复评'];
    const created=await request<{id:number}>('/interventions','POST',{studentId:selected.value.studentId,warningId:selected.value.id,title:planTitle.value.trim()||'阶段成长支持方案',suggestions,teacherNote:note.value.trim()||undefined});
    notice.value=`已创建帮扶草案 #${created.id}（状态：草稿），请在「一人一策」中审核后执行。`;
  }catch(e){formError.value=(e as Error).message;}finally{planBusy.value=false;}
}
async function runAnalyzeRules(){
  if(busy.value)return;
  busy.value=true;error.value='';
  try{
    const result=await request<{created:number;message:string}>('/warnings/analyze','POST');
    notice.value=`规则筛查完成，新增 ${result.created} 条预警。`;
    page.value=1;await load();
  }catch(e){error.value=(e as Error).message;}finally{busy.value=false;}
}
function search(){page.value=1;selected.value=undefined;load();}
function switchStatus(value:string){status.value=value;page.value=1;selected.value=undefined;load();}
function turn(delta:number){page.value+=delta;selected.value=undefined;load();}
onMounted(load);
</script>

<template>
  <section class="page-scroll workspace-page">
    <header class="workspace-heading">
      <div><p class="eyebrow">规则筛查与教师研判</p><h1>智能预警</h1><p>规则引擎只提供趋势事实，最终判断由班主任完成。</p></div>
      <button class="button primary" :disabled="busy" @click="runAnalyzeRules">{{ busy ? '分析中…' : '执行规则分析' }}</button>
    </header>

    <div class="warning-filters">
      <button v-for="filter in FILTERS" :key="filter.value" class="button" :class="{ selected: status === filter.value }" @click="switchStatus(filter.value)">{{ filter.label }}</button>
      <form class="warning-search" @submit.prevent="search">
        <input v-model="query" aria-label="按学生或预警内容搜索" placeholder="学生姓名、学号或预警内容"/>
        <button class="button" :disabled="loading">搜索</button>
      </form>
      <span class="result-count">共 {{ total }} 条</span>
    </div>

    <p v-if="notice" class="warning-notice" role="status">{{ notice }}</p>
    <p v-if="error" class="warning-error" role="alert">{{ error }} <button class="button" @click="load">重试</button></p>
    <p v-if="loading" role="status">正在读取预警…</p>

    <section v-else-if="!error" class="data-card panel">
      <table>
        <thead><tr><th>等级</th><th>学生</th><th>提示</th><th>规则</th><th>状态</th><th>生成时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="item in items" :key="item.id">
            <td><span class="status-chip" :class="TONE[LEVEL[item.level]] || 'success'">{{ LEVEL[item.level] || item.level }}</span></td>
            <td>{{ item.studentName }}<small>{{ item.studentNo }}</small></td>
            <td class="warning-summary">{{ item.summary }}</td>
            <td>{{ item.ruleCode }}</td>
            <td><span class="status-chip" :class="TONE[STATUS[item.status]] || 'warning'">{{ STATUS[item.status] || item.status }}</span></td>
            <td>{{ item.createdAt.replace('T', ' ').slice(0, 16) }}</td>
            <td><button class="table-action" @click="inspect(item.id)">查看详情</button></td>
          </tr>
          <tr v-if="!items.length"><td colspan="7" class="empty-cell">当前筛选条件下没有预警</td></tr>
        </tbody>
      </table>
    </section>

    <section v-if="selected" class="panel warning-detail">
      <header class="warning-detail-heading">
        <div>
          <h2>{{ selected.studentName }} · {{ LEVEL[selected.level] || selected.level }}</h2>
          <p class="muted">{{ selected.ruleCode }} · {{ STATUS[selected.status] || selected.status }} · 生成于 {{ selected.createdAt.replace('T', ' ').slice(0, 16) }}</p>
        </div>
        <button class="button" :disabled="detailBusy" @click="selected = undefined">收起</button>
      </header>
      <p class="warning-detail-summary">{{ selected.summary }}</p>

      <div class="warning-columns">
        <div>
          <h3>规则证据</h3>
          <ul class="warning-evidence">
            <li v-for="(row, index) in selected.evidence" :key="index">{{ typeof row === 'string' ? row : JSON.stringify(row) }}</li>
            <li v-if="!selected.evidence.length" class="muted">未记录结构化证据</li>
          </ul>
          <h3>过程记录</h3>
          <article v-for="event in events" :key="event.id" class="warning-event">
            <strong>{{ event.action === 'TRIAGE' ? '教师研判' : event.action === 'CLOSE' ? '关闭预警' : event.action }}</strong>
            <p>{{ event.note }}</p>
            <small>{{ event.createdAt.replace('T', ' ').slice(0, 16) }} · {{ event.actor }}</small>
          </article>
          <p v-if="!events.length" class="muted">尚无研判或关闭记录。</p>
        </div>

        <div>
          <h3>教师研判</h3>
          <form class="action-form" @submit.prevent="triage">
            <label>研判说明<textarea v-model="note" rows="3" maxlength="400" required placeholder="记录核实到的实际情况与判断依据"/></label>
            <label class="warning-checkbox"><input v-model="escalate" type="checkbox"/> 标记为需要人工重点研判</label>
            <button class="button primary dialog-done" :disabled="busy || selected.status !== 'OPEN'" >{{ busy ? '提交中…' : '提交研判' }}</button>
            <small v-if="selected.status !== 'OPEN'" class="muted">该预警已研判，不能重复提交。</small>
          </form>
          <button class="button warning-close" :disabled="busy" @click="closeWarning">关闭预警</button>

          <h3>AI 辅助分析</h3>
          <button class="button" :disabled="analyzing" @click="analyze">{{ analyzing ? '分析中…' : '生成辅助分析' }}</button>
          <div v-if="analysis" class="warning-analysis">
            <p class="warning-source">来源：{{ analysis.source === 'MODEL' ? '模型服务' : '规则模板（未配置模型服务）' }}</p>
            <p>{{ analysis.summary }}</p>
            <ul v-if="analysis.evidence?.length"><li v-for="row in analysis.evidence" :key="row">{{ row }}</li></ul>
            <ul v-if="analysis.suggestions?.length"><li v-for="row in analysis.suggestions" :key="row">{{ row }}</li></ul>
            <p class="muted">{{ analysis.disclaimer || 'AI辅助建议，仅供教师参考' }}</p>
            <form class="action-form" @submit.prevent="createPlan">
              <label>帮扶方案标题<input v-model="planTitle" maxlength="160" required/></label>
              <button class="button primary dialog-done" :disabled="planBusy">{{ planBusy ? '创建中…' : '转为帮扶草案' }}</button>
            </form>
          </div>
        </div>
      </div>
      <p v-if="formError" class="warning-error" role="alert">{{ formError }}</p>
    </section>

    <nav class="warning-pagination" aria-label="预警分页">
      <button class="button" :disabled="loading || page <= 1" @click="turn(-1)">上一页</button>
      <span>第 {{ page }} 页</span>
      <button class="button" :disabled="loading || page * pageSize >= total" @click="turn(1)">下一页</button>
    </nav>
  </section>
</template>

<style scoped>
.warning-filters{display:flex;align-items:center;gap:10px;flex-wrap:wrap;margin-bottom:16px}
.warning-filters .selected{background:#e5e4de;font-weight:600}
.warning-search{display:flex;align-items:center;gap:8px;margin-left:auto}
.warning-search input{font:inherit;border:1px solid var(--border);border-radius:5px;background:#fffdf9;padding:7px 9px;min-width:220px}
.warning-summary{white-space:normal;min-width:240px}
td small{display:block;color:var(--muted);font-size:11px}
.warning-detail{padding:20px;margin-top:18px}
.warning-detail-heading{display:flex;justify-content:space-between;align-items:flex-start;gap:12px}
.warning-detail-heading h2{font-size:20px}
.warning-detail-summary{margin:12px 0 18px;line-height:1.8}
.warning-columns{display:grid;grid-template-columns:1fr 1fr;gap:24px}
.warning-columns h3{font-size:15px;margin:18px 0 10px}
.warning-evidence,.warning-analysis ul{margin:0;padding-left:20px;line-height:1.9;font-size:14px}
.warning-event{border-top:1px solid var(--border);padding:10px 0}
.warning-event p{margin:6px 0;line-height:1.7}
.warning-event small{color:var(--muted)}
.warning-checkbox{display:flex!important;align-items:center;gap:8px}
.warning-close{margin-top:10px}
.warning-analysis{margin-top:12px;padding:14px;border:1px solid var(--border);border-radius:6px;background:#fcfbf8;line-height:1.85}
.warning-source{font-size:12px;color:var(--muted)}
.warning-notice{color:#668254;margin:12px 0}
.warning-error{color:#b8584c;margin:12px 0}
.warning-pagination{display:flex;align-items:center;justify-content:flex-end;gap:14px;margin:16px 0}
@media(max-width:1100px){.warning-columns{grid-template-columns:1fr}}
@media(max-width:600px){.warning-search{margin-left:0;width:100%}.warning-search input{flex:1;min-width:0}}
</style>
