<script setup lang="ts">
import PageHeader from './ui/PageHeader.vue';
import EmptyState from './ui/EmptyState.vue';
import {computed,onMounted,ref} from 'vue';
import {request} from '../api';

type Task={id:number;module:string;title:string;description:string|null;dueOn:string;pointReward:number;status:string};
type Student={id:number;name:string;studentNo:string};
type Assignment={id:number;studentId:number;studentName:string;status:string;completedOn:string|null;teacherNote:string|null};

/** 六机模块固定在枚举内，避免自由文本导致统计口径混乱。 */
const MODULES=['铸机魂','立机规','淬机质','铺机路','聚机力','调机态'];
const STATUS:Record<string,string>={ASSIGNED:'待完成',COMPLETED:'已完成'};

const tasks=ref<Task[]>([]),loading=ref(false),error=ref(''),notice=ref(''),busy=ref(false),formError=ref('');
const creating=ref(false),students=ref<Student[]>([]),studentQuery=ref('');
const selected=ref<Task>(),assignments=ref<Assignment[]>([]),detailBusy=ref(false);
const picked=ref<number[]>([]),evaluatingId=ref(0),evaluationNote=ref('');
const draft=ref({module:'聚机力',title:'',description:'',dueOn:'',pointReward:2});
const today=new Date().toLocaleDateString('en-CA');
const defaultDue=new Date(Date.now()+7*86400000).toLocaleDateString('en-CA');
let generation=0;

const visibleStudents=computed(()=>{
  const keyword=studentQuery.value.trim();
  return keyword?students.value.filter(s=>s.name.includes(keyword)||s.studentNo.includes(keyword)):students.value;
});

async function loadTasks(){
  const current=++generation;
  loading.value=true;error.value='';
  try{
    const data=await request<{items:Task[]}>('/growth-tasks');
    if(current!==generation)return;
    tasks.value=data.items;
  }catch(e){if(current===generation)error.value=(e as Error).message;}finally{if(current===generation)loading.value=false;}
}
async function loadStudents(){
  try{students.value=(await request<{items:Student[]}>('/students?pageSize=100')).items;}
  catch(e){formError.value=(e as Error).message;}
}
async function inspect(task:Task){
  detailBusy.value=true;formError.value='';notice.value='';evaluatingId.value=0;evaluationNote.value='';
  try{
    selected.value=task;
    assignments.value=(await request<{items:Assignment[]}>(`/growth-tasks/${task.id}/students`)).items;
    picked.value=[];
  }catch(e){formError.value=(e as Error).message;}finally{detailBusy.value=false;}
}
async function refreshDetail(){ if(selected.value) await inspect(selected.value); }
async function createTask(){
  if(busy.value)return;
  if(!draft.value.title.trim()){formError.value='请填写任务标题';return;}
  busy.value=true;formError.value='';
  try{
    const created=await request<{id:number}>('/growth-tasks','POST',{module:draft.value.module,title:draft.value.title.trim(),description:draft.value.description.trim()||undefined,dueOn:draft.value.dueOn||undefined,pointReward:Number(draft.value.pointReward)});
    notice.value=`任务 #${created.id} 已发布，可在下方指派学生。`;
    creating.value=false;draft.value={module:draft.value.module,title:'',description:'',dueOn:defaultDue,pointReward:2};
    await loadTasks();
    await inspect(tasks.value.find(t=>t.id===created.id) || tasks.value[0]);
  }catch(e){formError.value=(e as Error).message;}finally{busy.value=false;}
}
async function assign(){
  if(!selected.value||busy.value||!picked.value.length)return;
  busy.value=true;formError.value='';
  try{
    await request(`/growth-tasks/${selected.value.id}/assign`,'POST',{studentIds:picked.value});
    notice.value=`已指派 ${picked.value.length} 名学生；重复指派不会产生重复记录。`;
    await refreshDetail();
  }catch(e){formError.value=(e as Error).message;}finally{busy.value=false;}
}
async function complete(row:Assignment){
  if(busy.value)return;
  busy.value=true;formError.value='';
  try{
    const result=await request<{awarded:boolean}>(`/student-tasks/${row.id}/complete`,'POST',{});
    notice.value=result.awarded?`已确认 ${row.studentName} 完成任务，并按任务奖励发放机智币。`:`该记录此前已确认，未重复发放机智币。`;
    await refreshDetail();
  }catch(e){formError.value=(e as Error).message;}finally{busy.value=false;}
}
async function evaluate(row:Assignment){
  if(busy.value)return;
  busy.value=true;formError.value='';
  try{
    await request(`/student-tasks/${row.id}/evaluate`,'POST',{note:evaluationNote.value.trim()||undefined});
    notice.value='任务评价已记录。';
    evaluatingId.value=0;evaluationNote.value='';
    await refreshDetail();
  }catch(e){formError.value=(e as Error).message;}finally{busy.value=false;}
}
function toggle(id:number){ picked.value=picked.value.includes(id)?picked.value.filter(x=>x!==id):[...picked.value,id]; }
onMounted(async()=>{ await loadTasks(); await loadStudents(); });
</script>

<template>
  <section class="page-scroll workspace-page">
    <PageHeader title="六机任务" eyebrow="铸机魂 · 立机规 · 淬机质 · 铺机路 · 聚机力 · 调机态" description="发布任务、指派学生，教师确认完成后发放机智币；重复确认不会重复发币。"><button class="button primary" @click="creating = !creating">{{ creating ? '收起新建' : '发布任务' }}</button></PageHeader>

    <section v-if="creating" class="panel task-create">
      <h2>发布成长任务</h2>
      <form class="action-form" @submit.prevent="createTask">
        <div class="task-create-grid">
          <label>所属模块<select v-model="draft.module"><option v-for="m in MODULES" :key="m" :value="m">{{ m }}</option></select></label>
          <label>任务标题<input v-model="draft.title" maxlength="160" required/></label>
          <label>截止日期<input v-model="draft.dueOn" type="date" :min="today"/></label>
          <label>完成奖励（机智币）<input v-model.number="draft.pointReward" type="number" min="0" max="100"/></label>
        </div>
        <label>任务说明<textarea v-model="draft.description" rows="2" maxlength="500"/></label>
        <button class="button primary dialog-done" :disabled="busy">{{ busy ? '发布中…' : '发布任务' }}</button>
      </form>
    </section>

    <p v-if="notice" class="task-notice" role="status">{{ notice }}</p>
    <p v-if="error" class="task-error" role="alert">{{ error }} <button class="button" @click="loadTasks">重试</button></p>
    <p v-if="loading" role="status">正在读取任务…</p>

    <section v-else-if="!error" class="data-card panel">
      <table>
        <thead><tr><th>模块</th><th>任务</th><th>截止日期</th><th>奖励</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="task in tasks" :key="task.id">
            <td>{{ task.module }}</td>
            <td>{{ task.title }}<small v-if="task.description">{{ task.description }}</small></td>
            <td>{{ task.dueOn }}</td>
            <td>{{ task.pointReward > 0 ? '+' + task.pointReward : '无' }}</td>
            <td><button class="table-action" @click="inspect(task)">指派与完成情况</button></td>
          </tr>
          <tr v-if="!tasks.length"><td colspan="5" class="empty-cell"><EmptyState title="尚未发布成长任务" description="点击发布任务，安排学生成长活动。"/></td></tr>
        </tbody>
      </table>
    </section>

    <section v-if="selected" class="panel task-detail">
      <header class="task-heading">
        <div><h2>{{ selected.module }} · {{ selected.title }}</h2><p class="muted">截止 {{ selected.dueOn }} · 完成奖励 {{ selected.pointReward }} 机智币 · 已指派 {{ assignments.length }} 人</p></div>
        <button class="button" :disabled="detailBusy" @click="selected = undefined">收起</button>
      </header>

      <div class="task-columns">
        <div>
          <h3>指派学生</h3>
          <input v-model="studentQuery" class="task-search" aria-label="按姓名或学号筛选学生" placeholder="筛选姓名或学号"/>
          <div class="task-students">
            <label v-for="student in visibleStudents" :key="student.id" class="task-student">
              <input type="checkbox" :checked="picked.includes(student.id)" @change="toggle(student.id)"/>
              <span>{{ student.name }}</span><small>{{ student.studentNo }}</small>
            </label>
            <p v-if="!visibleStudents.length" class="muted">没有符合条件的学生。</p>
          </div>
          <button class="button primary" :disabled="busy || !picked.length" @click="assign">指派已选 {{ picked.length }} 人</button>
        </div>

        <div>
          <h3>完成情况</h3>
          <article v-for="row in assignments" :key="row.id" class="task-record">
            <div class="task-record-head">
              <strong>{{ row.studentName }}</strong>
              <span class="status-chip" :class="row.status === 'COMPLETED' ? 'success' : 'warning'">{{ STATUS[row.status] || row.status }}</span>
            </div>
            <p class="muted">{{ row.completedOn ? `完成于 ${row.completedOn}` : '尚未完成' }}<span v-if="row.teacherNote"> · {{ row.teacherNote }}</span></p>
            <div class="task-record-actions">
              <button class="button" :disabled="busy || row.status === 'COMPLETED'" @click="complete(row)">确认完成并发币</button>
              <button class="button" :disabled="busy" @click="evaluatingId = evaluatingId === row.id ? 0 : row.id">{{ evaluatingId === row.id ? '取消评价' : '记录评价' }}</button>
            </div>
            <form v-if="evaluatingId === row.id" class="action-form" @submit.prevent="evaluate(row)">
              <label>评价说明<textarea v-model="evaluationNote" rows="2" maxlength="500" placeholder="记录完成质量与下一步建议"/></label>
              <button class="button primary dialog-done" :disabled="busy">{{ busy ? '保存中…' : '保存评价' }}</button>
            </form>
          </article>
          <p v-if="!assignments.length" class="muted">该任务尚未指派学生。</p>
        </div>
      </div>
      <p v-if="formError" class="task-error" role="alert">{{ formError }}</p>
    </section>
  </section>
</template>

<style scoped>
.task-create{padding:20px;margin-bottom:18px}
.task-create h2{font-size:18px;margin-bottom:14px}
.task-create-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:14px;align-items:end}
.task-detail{padding:20px;margin-top:18px}
.task-heading{display:flex;justify-content:space-between;align-items:flex-start;gap:12px}
.task-heading h2{font-size:20px}
.task-columns{display:grid;grid-template-columns:1fr 1fr;gap:24px;margin-top:16px}
.task-columns h3{font-size:15px;margin:0 0 10px}
.task-search{font:inherit;width:100%;border:1px solid var(--border);border-radius:5px;background:#fffdf9;padding:7px 9px;margin-bottom:10px}
.task-students{max-height:280px;overflow:auto;border:1px solid var(--border);border-radius:6px;padding:8px;margin-bottom:12px;background:#fcfbf8}
.task-student{display:flex;align-items:center;gap:8px;padding:6px 4px;font-size:14px}
.task-student small{color:var(--muted);margin-left:auto}
.task-record{border-top:1px solid var(--border);padding:12px 0}
.task-record-head{display:flex;align-items:center;gap:10px}
.task-record p{margin:6px 0;line-height:1.7}
.task-record-actions{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:8px}
.task-notice{color:#668254;margin:12px 0}
.task-error{color:#b8584c;margin:12px 0}
td small{display:block;color:var(--muted);font-size:11px}
select{font:inherit;padding:7px;border:1px solid var(--border);border-radius:5px;background:#fffdf9}
@media(max-width:1100px){.task-columns{grid-template-columns:1fr}}
</style>
