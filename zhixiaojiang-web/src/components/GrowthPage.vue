<script setup lang="ts">
import {computed,ref,watch} from 'vue';
import {request} from '../api';
import StudentPicker from './StudentPicker.vue';
type Score={id:number;subject:string;examName:string;score:number;fullScore:number;occurredOn:string};
type Evaluation={id:number;periodStart:string;periodEnd:string;moralScore:number|null;skillScore:number|null;thinkingScore:number|null;smartScore:number|null;evidence:string};
type Workspace={student:{name:string};growthIndex:number|null;dimensions:Record<string,number|null>;scores:Score[];evaluations:Evaluation[];growth:{id:number;title:string;detail:string;occurredOn:string;source:string;createdBy:number}[];attendance:{id:number;attendanceDate:string;status:string;note:string}[];skills:{id:number;skillName:string;score:number;level:string;occurredOn:string;evidence:string}[]};
const today=new Date().toLocaleDateString('en-CA');
const studentId=ref(0),from=ref(`${today.slice(0,4)}-01-01`),to=ref(today),subject=ref('');
const data=ref<Workspace>(),loading=ref(false),busy=ref(false),error=ref(''),formError=ref(''),notice=ref('');
const editor=ref<HTMLDialogElement>(),kind=ref('scores');
const label=ref(''),note=ref(''),score=ref<number|string>(''),fullScore=ref(100),date=ref(today),start=ref(today),end=ref(today),source=ref('教师录入'),dimension=ref('SMART'),attendance=ref('PRESENT'),examSubject=ref('数学');
const evaluation=ref<Record<string,number|string>>({moral:'',skill:'',thinking:'',smart:''});
const axes=[{key:'moral',name:'品德'},{key:'skill',name:'技能'},{key:'thinking',name:'思维'},{key:'smart',name:'智行'}];
const kindNames:Record<string,string>={scores:'录入考试成绩',evaluations:'录入四维评价',growth:'录入成长记录',skills:'录入技能记录',attendance:'登记每日出勤'};
const attendanceLabels:Record<string,string>={PRESENT:'出勤',LATE:'迟到',ABSENT:'缺勤',LEAVE:'请假'};
const subjects=computed(()=>[...new Set(data.value?.scores.map(x=>x.subject)||[])]);
const exams=computed(()=>[...(data.value?.scores || [])].filter(x=>x.subject===subject.value).sort((a,b)=>a.occurredOn.localeCompare(b.occurredOn)||a.id-b.id));
const trend=computed(()=>exams.value.map((x,i)=>`${40+i*400/Math.max(1,exams.value.length-1)},${160-x.score/x.fullScore*120}`).join(' '));
const complete=computed(()=>data.value && axes.every(axis=>data.value!.dimensions[axis.key]!=null));
const radar=computed(()=>axes.map((axis,i)=>{const value=Number(data.value?.dimensions[axis.key]||0)/100*75;return `${110+Math.sin(i*Math.PI/2)*value},${110-Math.cos(i*Math.PI/2)*value}`;}).join(' '));
let generation=0;
async function load(){
  const current=++generation;error.value='';data.value=undefined;
  if(!studentId.value){loading.value=false;return;}
  if(!from.value || !to.value || from.value>to.value){error.value='请选择有效的统计周期';return;}
  loading.value=true;
  try{const result=await request<Workspace>(`/students/${studentId.value}/growth-workspace?from=${from.value}&to=${to.value}`);if(current!==generation)return;data.value=result;if(!subjects.value.includes(subject.value))subject.value=subjects.value[0]||'';}
  catch(e){if(current===generation)error.value=(e as Error).message;}finally{if(current===generation)loading.value=false;}
}
watch(studentId,()=>{notice.value='';load();});
function open(type:string){kind.value=type;formError.value='';label.value='';note.value='';score.value='';fullScore.value=100;date.value=today;start.value=today;end.value=today;evaluation.value={moral:'',skill:'',thinking:'',smart:''};editor.value?.showModal();}
async function save(){
  if(busy.value)return;busy.value=true;formError.value='';
  try{
    let body:Record<string,unknown>;
    if(kind.value==='scores')body={subject:examSubject.value.trim(),examName:label.value.trim(),score:Number(score.value),fullScore:fullScore.value,occurredOn:date.value};
    else if(kind.value==='evaluations')body={periodStart:start.value,periodEnd:end.value,evidence:note.value.trim(),...Object.fromEntries(axes.map(axis=>[`${axis.key}Score`,evaluation.value[axis.key]===''?null:Number(evaluation.value[axis.key])]))};
    else if(kind.value==='attendance')body={attendanceDate:date.value,status:attendance.value,note:note.value.trim()};
    else if(kind.value==='skills')body={skillName:label.value.trim(),score:Number(score.value),occurredOn:date.value,evidence:note.value.trim()};
    else body={title:label.value.trim(),score:Number(score.value),dimension:dimension.value,detail:note.value.trim(),source:source.value.trim(),occurredOn:date.value};
    await request(`/students/${studentId.value}/${kind.value}`,'POST',body);
    editor.value?.close();notice.value='记录已保存。仅显示当前统计周期内的数据；如日期不在周期内，请调整筛选。';await load();
  }catch(e){formError.value=(e as Error).message;}finally{busy.value=false;}
}
</script>
<template>
  <section class="page-scroll workspace-page">
    <header class="workspace-heading"><div><p class="eyebrow">成长记录与阶段评价</p><h1>成长画像</h1><p>四维评价、成绩趋势和成长经历，以实际记录为依据。</p></div></header>
    <StudentPicker v-model="studentId" :disabled="busy"/>
    <form class="growth-filter" @submit.prevent="load"><label>开始日期<input v-model="from" type="date" required/></label><label>结束日期<input v-model="to" type="date" required/></label><button class="button" :disabled="loading || !studentId">查询周期</button></form>
    <p v-if="error" class="growth-error" role="alert">{{error}}</p><p v-if="notice" class="growth-notice" role="status">{{notice}}</p>
    <p v-if="!studentId" class="growth-empty">请选择学生查看成长画像。</p><p v-else-if="loading" role="status">正在汇总成长记录…</p>
    <template v-if="data && !loading">
      <div class="growth-actions"><button v-for="(text,type) in kindNames" :key="type" class="button" @click="open(String(type))">{{text}}</button></div>
      <div class="growth-charts">
        <section class="panel growth-card"><h2>{{data.student.name}} · 四维评价</h2><div class="radar-layout"><svg v-if="complete" viewBox="0 0 220 220" role="img" aria-label="品德、技能、思维、智行四维雷达图"><polygon v-for="radius in [25,50,75]" :key="radius" :points="`110,${110-radius} ${110+radius},110 110,${110+radius} ${110-radius},110`" fill="none" stroke="#d5d4ca"/><path d="M110 35V185M35 110H185" stroke="#d5d4ca"/><polygon :points="radar" fill="#e5785540" stroke="#e57855" stroke-width="2"/><text x="110" y="20" text-anchor="middle">品德</text><text x="190" y="115">技能</text><text x="110" y="207" text-anchor="middle">思维</text><text x="2" y="115">智行</text></svg><p v-else class="growth-empty">维度不齐全，暂不绘制完整雷达。</p><dl><template v-for="axis in axes" :key="axis.key"><dt>{{axis.name}}</dt><dd>{{data.dimensions[axis.key]??'数据不足'}}</dd></template><dt>成长指数</dt><dd>{{data.growthIndex??'数据不足'}}</dd></dl></div><p class="growth-help">取周期内完成的教师评价，各维度先求平均，四维齐全后等权计算指数。成长记录评分与机智币不自动换算为四维评价。</p></section>
        <section class="panel growth-card"><header class="growth-chart-heading"><h2>考试成绩趋势</h2><select v-model="subject" aria-label="选择趋势科目"><option v-for="item in subjects" :key="item">{{item}}</option></select></header><svg v-if="exams.length>=2" class="trend-chart" viewBox="0 0 480 190" role="img" :aria-label="`${subject}成绩百分比趋势`"><path d="M40 40V160H440" fill="none" stroke="#cccac0"/><path d="M40 88H440" stroke="#d9ba8b" stroke-dasharray="4 4"/><text x="4" y="44">100%</text><text x="4" y="91">60%</text><text x="14" y="164">0%</text><polyline :points="trend" fill="none" stroke="#e57855" stroke-width="3"/><circle v-for="(exam,i) in exams" :key="exam.id" :cx="40+i*400/Math.max(1,exams.length-1)" :cy="160-exam.score/exam.fullScore*120" r="4" fill="#e57855"><title>{{exam.examName}}：{{exam.score}} / {{exam.fullScore}}</title></circle></svg><p v-else class="growth-empty">至少两次同科目成绩才能显示趋势。</p><p class="growth-help">按各批次满分折算为百分比；虚线为 60% 及格线。不同科目不混用。</p></section>
      </div>
      <section class="panel data-card growth-section"><h2>考试明细 · {{subject||'暂无科目'}}</h2><table><thead><tr><th>日期</th><th>考试批次</th><th>分数 / 满分</th><th>结果</th></tr></thead><tbody><tr v-for="exam in [...exams].reverse()" :key="exam.id"><td>{{exam.occurredOn}}</td><td>{{exam.examName}}</td><td>{{exam.score}} / {{exam.fullScore}}</td><td>{{exam.score>=exam.fullScore*.6?'及格':'未及格'}}</td></tr><tr v-if="!exams.length"><td colspan="4" class="empty-cell">本周期暂无考试成绩</td></tr></tbody></table></section>
      <section class="panel data-card growth-section"><h2>四维评价依据</h2><table><thead><tr><th>评价周期</th><th v-for="axis in axes" :key="axis.key">{{axis.name}}</th><th>依据</th></tr></thead><tbody><tr v-for="item in data.evaluations" :key="item.id"><td>{{item.periodStart}} — {{item.periodEnd}}</td><td>{{item.moralScore??'—'}}</td><td>{{item.skillScore??'—'}}</td><td>{{item.thinkingScore??'—'}}</td><td>{{item.smartScore??'—'}}</td><td>{{item.evidence}}</td></tr><tr v-if="!data.evaluations.length"><td colspan="6" class="empty-cell">暂无教师四维评价</td></tr></tbody></table></section>
      <div class="growth-charts growth-section"><section class="panel growth-card"><h2>成长时间轴</h2><article v-for="item in data.growth" :key="item.id" class="growth-event"><strong>{{item.title}}</strong><p>{{item.detail}}</p><small>{{item.occurredOn}} · {{item.source}} · 操作者 #{{item.createdBy}}</small></article><p v-if="!data.growth.length" class="growth-empty">本周期暂无成长记录</p></section><section class="panel growth-card"><h2>技能记录</h2><article v-for="item in data.skills" :key="item.id" class="growth-event"><strong>{{item.skillName}} · {{item.score}} 分</strong><p>{{item.evidence}}</p><small>{{item.occurredOn}} {{item.level}}</small></article><p v-if="!data.skills.length" class="growth-empty">本周期暂无技能记录</p></section></div>
      <section class="panel data-card growth-section"><h2>每日出勤</h2><table><thead><tr><th>日期</th><th>状态</th><th>说明</th></tr></thead><tbody><tr v-for="item in data.attendance" :key="item.id"><td>{{item.attendanceDate}}</td><td>{{attendanceLabels[item.status]||item.status}}</td><td>{{item.note||'—'}}</td></tr><tr v-if="!data.attendance.length"><td colspan="3" class="empty-cell">本周期尚未登记出勤，不默认为出勤</td></tr></tbody></table></section>
      <p class="growth-help growth-section">明细分别展示所选周期最近 100 条；四维平均值统计该周期全部评价。需要更早明细时请缩小日期范围。</p>
    </template>
    <dialog ref="editor" @cancel="busy && $event.preventDefault()"><header class="dialog-heading"><h2>{{kindNames[kind]}}</h2><button class="button" :disabled="busy" @click="editor?.close()">关闭</button></header><form class="action-form" @submit.prevent="save"><fieldset :disabled="busy"><template v-if="kind==='evaluations'"><label>周期开始<input v-model="start" type="date" :max="end" required/></label><label>周期结束<input v-model="end" type="date" :min="start" :max="today" required/></label><label v-for="axis in axes" :key="axis.key">{{axis.name}}（留空表示缺失）<input v-model="evaluation[axis.key]" type="number" min="0" max="100" step="0.1"/></label></template><template v-else><label>发生日期<input v-model="date" type="date" :max="today" required/></label><label v-if="kind==='scores'">科目<input v-model="examSubject" maxlength="40" required/></label><label v-if="kind!=='attendance'">{{kind==='scores'?'考试批次':kind==='skills'?'技能名称':'记录标题'}}<input v-model="label" :maxlength="kind==='growth'?160:120" required/></label><label v-if="kind==='attendance'">出勤状态<select v-model="attendance"><option v-for="(text,key) in attendanceLabels" :key="key" :value="key">{{text}}</option></select></label><label v-else>评分<input v-model="score" type="number" min="0" :max="kind==='scores'?fullScore:100" step="0.1" required/></label><label v-if="kind==='scores'">本次考试满分<input v-model.number="fullScore" type="number" min="0.01" max="9999.99" step="0.01" required/></label><template v-if="kind==='growth'"><label>维度<select v-model="dimension"><option value="MORAL">品德</option><option value="SKILL">技能</option><option value="THINKING">思维</option><option value="SMART">智行</option></select></label><label>记录来源<input v-model="source" maxlength="64" required/></label></template></template><label v-if="kind!=='scores'">{{kind==='evaluations'||kind==='skills'?'评价依据':'说明'}}<textarea v-model="note" :maxlength="kind==='evaluations'?1000:kind==='attendance'?200:500" rows="3" :required="kind==='evaluations'||kind==='skills'"/></label></fieldset><p v-if="formError" role="alert" class="growth-error">{{formError}}</p><button class="button primary dialog-done" :disabled="busy">{{busy?'保存中…':'保存记录'}}</button></form></dialog>
  </section>
</template>
<style scoped>
.growth-filter,.growth-actions{display:flex;gap:12px;flex-wrap:wrap;align-items:center;margin:16px 0}.growth-filter label{display:flex;align-items:center;gap:8px;font-size:13px}input,select{font:inherit;border:1px solid var(--border);border-radius:5px;background:#fffdf9;padding:7px;min-width:0}.growth-charts{display:grid;grid-template-columns:1fr 1fr;gap:16px}.growth-card{padding:20px;min-width:0}.radar-layout{display:flex;align-items:center;gap:12px}.radar-layout svg{width:220px;height:220px;flex:0 1 220px}.radar-layout dl{display:grid;grid-template-columns:1fr 1fr;gap:12px;font-size:14px;min-width:140px}.radar-layout dd{margin:0}.growth-help{font-size:12px;line-height:1.8;color:var(--muted)}.growth-chart-heading{display:flex;justify-content:space-between;gap:8px;align-items:center}.trend-chart{width:100%;height:220px}.trend-chart text,.radar-layout text{font:12px serif;fill:var(--muted)}.growth-empty{padding:22px 0;color:var(--muted)}.growth-section{margin-top:18px}.growth-section>h2{padding:14px 16px;font-size:18px}.growth-event{border-top:1px solid var(--border);margin-top:14px;padding-top:14px}.growth-event p{margin:8px 0;line-height:1.8}.growth-event small{color:var(--muted)}.growth-error{color:#b8584c;margin:12px 0}.growth-notice{color:#668254;margin:12px 0}fieldset{border:0;margin:0;padding:0;display:grid;gap:14px;min-width:0}@media(max-width:1100px){.growth-charts{grid-template-columns:1fr}}@media(max-width:600px){.radar-layout{flex-wrap:wrap}.growth-filter label{width:100%}.growth-filter input{flex:1}.growth-chart-heading{flex-wrap:wrap}}
</style>
