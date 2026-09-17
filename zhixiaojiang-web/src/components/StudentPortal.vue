<script setup lang="ts">
import {nextTick,onMounted,onUnmounted,ref} from 'vue';
import {PanelLeft,Activity,ListTodo,Radar,Coins,UserRound,LogOut,Target,Bell} from 'lucide-vue-next';
import StudentPlans from './StudentPlans.vue';
import StudentMessages from './StudentMessages.vue';
import PageHeader from './ui/PageHeader.vue';
import {request} from '../api';
import EmptyState from './ui/EmptyState.vue';
import PaginationBar from './ui/PaginationBar.vue';
import StudentPasswordForm from './StudentPasswordForm.vue';
import StudentGrowth from './StudentGrowth.vue';
import TaskAttachments, {type Attachment} from './TaskAttachments.vue';
type Task={id:number;title:string;description?:string;module:string;dueOn:string;status:string;pointReward?:number;teacherNote?:string};
type Submission={id:number;content:string;status:string;feedback?:string;createdAt:string;attachments:Attachment[]};
const attachmentIds=ref<number[]>([]);
type Ledger={id:number;amount:number;reason:string;createdAt:string;reversalOf?:number;reversed:boolean};
defineEmits<{logout:[]}>();
const labels:Record<string,string>={ASSIGNED:'待提交',SUBMITTED:'待评价',RETURNED:'需补充',COMPLETED:'已完成'};
const view=ref('首页'),loading=ref(false),busy=ref(false),error=ref(''),notice=ref('');
const collapsed=ref(false),mobile=ref(window.matchMedia('(max-width:600px)').matches),menuToggle=ref<HTMLButtonElement>();
const mobileQuery=window.matchMedia('(max-width:600px)');
const sidebar=ref<HTMLElement>(),mainContent=ref<HTMLElement>();
const menuGroups=[
 {label:'我的成长',items:[{view:'首页',label:'概览',icon:Activity},{view:'任务',label:'六机任务',icon:ListTodo},{view:'成长',label:'成长画像',icon:Radar},{view:'机智币',label:'机智币',icon:Coins}]},
 {label:'成长支持',items:[{view:'计划',label:'成长计划',icon:Target},{view:'消息',label:'消息',icon:Bell}]},
 {label:'个人',items:[{view:'我的',label:'个人资料',icon:UserRound}]},
];
const pageTitles:Record<string,string>={首页:'概览',任务:'六机任务',成长:'成长画像',机智币:'机智币',我的:'个人资料'};
function resizeMenu(event:MediaQueryListEvent){mobile.value=event.matches;collapsed.value=false;}
function closeMenu(){collapsed.value=false;nextTick(()=>menuToggle.value?.focus());}
async function toggleMenu(){collapsed.value=!collapsed.value;await nextTick();if(mobile.value&&collapsed.value)sidebar.value?.querySelector<HTMLButtonElement>('button:not(:disabled)')?.focus();}
function menuKeys(event:KeyboardEvent){
 if(!mobile.value||!collapsed.value)return;
 if(event.key==='Escape'){event.preventDefault();closeMenu();return;}
 if(event.key!=='Tab')return;
 const buttons=Array.from(sidebar.value?.querySelectorAll<HTMLButtonElement>('button:not(:disabled)')||[]);
 const first=buttons[0],last=buttons[buttons.length-1];if(!first)return;
 if(!sidebar.value?.contains(document.activeElement)){event.preventDefault();(event.shiftKey?last:first)?.focus();}
 else if(event.shiftKey&&document.activeElement===first){event.preventDefault();last?.focus();}
 else if(!event.shiftKey&&document.activeElement===last){event.preventDefault();first.focus();}
}
const home=ref<{student:{name:string;studentNo:string;className:string};todos:Task[];balance:number}>();
const tasks=ref<Task[]>([]),selected=ref<Task>(),history=ref<Submission[]>([]),content=ref(''),requestKey=ref('');
const ledger=ref<{items:Ledger[];balance:number;total:number}>(),page=ref(1),pageSize=ref(20);
let generation=0;
async function load(){
 const current=++generation;loading.value=true;error.value='';
 try{
  if(view.value==='首页'){const data=await request<typeof home.value>('/student-portal/home');if(current===generation)home.value=data;}
  else if(view.value==='任务'){const data=await request<{items:Task[]}>('/student-portal/tasks');if(current===generation)tasks.value=data.items;}
  else if(view.value==='机智币'){const data=await request<typeof ledger.value>(`/student-portal/points?page=${page.value}&pageSize=${pageSize.value}`);if(current===generation)ledger.value=data;}
 }catch(e){if(current===generation)error.value=(e as Error).message;}finally{if(current===generation)loading.value=false;}
}
function navigate(next:string){if(busy.value)return;view.value=next;selected.value=undefined;notice.value='';if(mobile.value)collapsed.value=false;load();nextTick(()=>mainContent.value?.focus());}
async function inspect(task:Task){
 if(busy.value)return;busy.value=true;error.value='';
 try{const data=await request<{items:Submission[]}>(`/student-portal/tasks/${task.id}/submissions`);history.value=data.items;selected.value=task;content.value='';attachmentIds.value=[];requestKey.value=crypto.randomUUID();}
 catch(e){error.value=(e as Error).message;}finally{busy.value=false;}
}
async function submit(){
 if(!selected.value||busy.value)return;busy.value=true;error.value='';notice.value='';
 try{await request(`/student-portal/tasks/${selected.value.id}/submissions`,'POST',{content:content.value,requestKey:requestKey.value,attachmentIds:attachmentIds.value});selected.value=undefined;notice.value='成果已提交，等待教师评价。';await load();}
 catch(e){error.value=(e as Error).message;}finally{busy.value=false;}
}
function changePage(next:number){page.value=next;load();}
function resize(size:number){pageSize.value=size;changePage(1);}
onMounted(()=>{load();mobileQuery.addEventListener('change',resizeMenu);document.addEventListener('keydown',menuKeys);});
onUnmounted(()=>{mobileQuery.removeEventListener('change',resizeMenu);document.removeEventListener('keydown',menuKeys);});
</script>
<template>
 <div class="application student-portal" :class="{collapsed}">
  <header class="topbar">
   <div class="brand-area"><button ref="menuToggle" class="icon-button sidebar-toggle" aria-label="切换学生菜单" aria-controls="student-sidebar" :aria-expanded="mobile?collapsed:!collapsed" @click="toggleMenu"><PanelLeft/></button><a class="brand" href="/" @click.prevent="navigate('首页')"><img src="/favicon.svg" alt=""/><strong>智小匠</strong></a></div>
   <div class="top-tools"><span class="student-role">学生端</span><button class="avatar" aria-label="个人资料" :disabled="busy" @click="navigate('我的')">学</button></div>
  </header>
  <button v-if="mobile && collapsed" class="student-menu-backdrop" aria-label="关闭学生菜单" tabindex="-1" @click="closeMenu"/>
  <aside ref="sidebar" id="student-sidebar" class="sidebar" aria-label="学生侧边导航" :role="mobile && collapsed?'dialog':undefined" :aria-modal="mobile && collapsed?true:undefined" :inert="mobile && !collapsed">
   <button v-if="mobile" class="button student-menu-close" @click="closeMenu">关闭菜单</button>
   <nav aria-label="学生功能"><section v-for="group in menuGroups" :key="group.label" class="menu-group"><p class="group-label">{{group.label}}</p><button v-for="item in group.items" :key="item.view" class="menu-item" :class="{selected:view===item.view}" :aria-current="view===item.view?'page':undefined" :aria-label="item.label" :title="item.label" :disabled="busy" @click="navigate(item.view)"><component :is="item.icon"/><span>{{item.label}}</span></button></section></nav>
   <button class="view-switch logout-switch" aria-label="退出登录" title="退出登录" :disabled="busy" @click="$emit('logout')"><LogOut/><span>退出登录</span></button>
  </aside>
  <main ref="mainContent" class="canvas" tabindex="-1" :aria-label="pageTitles[view] || view" :inert="mobile && collapsed"><div class="page-scroll workspace-page student-content">
   <PageHeader :title="pageTitles[view] || view" eyebrow="学生成长工作台"/>
   <p v-if="notice" role="status">{{notice}}</p>
   <p v-if="error" role="alert">{{error}} <button class="button" @click="load">重新读取</button></p>
   <p v-if="loading" role="status">正在读取你的成长数据…</p>
   <StudentPasswordForm v-else-if="view==='我的'"/>
   <StudentGrowth v-else-if="view==='成长'"/>
   <StudentPlans v-else-if="view==='计划'"/>
   <StudentMessages v-else-if="view==='消息'" @navigate="navigate"/>
   <template v-else-if="view==='首页' && home">
    <section class="panel student-card"><h2>{{home.student.name}}，你好</h2><p>{{home.student.className}} · {{home.student.studentNo}}</p><p>机智币余额 <strong>{{home.balance}}</strong> · 待办 <strong>{{home.todos.length}}</strong></p></section>
    <section class="panel student-card"><h2>我的待办</h2><article v-for="task in home.todos" :key="task.id" class="student-task"><h3>{{task.title}}</h3><p>{{labels[task.status]}} · 截止 {{task.dueOn}}</p><button class="button" @click="navigate('任务')">查看任务</button></article><EmptyState v-if="!home.todos.length" title="暂无待办" description="教师发布并指派任务后，会显示在这里。"/></section>
   </template>
   <template v-else-if="view==='任务'">
    <section v-if="selected" class="panel student-card"><button class="button" :disabled="busy" @click="selected=undefined">返回任务列表</button><h2>{{selected.title}}</h2><p>{{selected.description}}</p><p>{{labels[selected.status]}} · 截止 {{selected.dueOn}}</p>
     <form v-if="['ASSIGNED','RETURNED'].includes(selected.status)" class="action-form" @submit.prevent="submit"><label>成果与过程说明<textarea v-model="content" required maxlength="4000" rows="6" :disabled="busy"/></label><TaskAttachments :key="selected.id" :task-id="selected.id" :disabled="busy" @change="attachmentIds=$event" @busy="busy=$event"/><button class="button primary" :disabled="busy || !content.trim()">{{busy?'正在处理…':'提交教师评价'}}</button></form>
     <h3>提交与反馈</h3><article v-for="entry in history" :key="entry.id" class="student-task"><p>{{labels[entry.status]}} · {{entry.createdAt}}</p><p class="student-prose">{{entry.content}}</p><TaskAttachments :files="entry.attachments"/><p v-if="entry.feedback" class="student-prose">教师反馈：{{entry.feedback}}</p></article><EmptyState v-if="!history.length" title="尚未提交成果"/>
    </section>
    <template v-else><article v-for="task in tasks" :key="task.id" class="panel student-card"><span>{{task.module}} · {{labels[task.status]}}</span><h2>{{task.title}}</h2><p>截止 {{task.dueOn}} · 教师确认完成后奖励 {{task.pointReward}} 机智币</p><p v-if="task.teacherNote">教师反馈：{{task.teacherNote}}</p><button class="button" :disabled="busy" @click="inspect(task)">查看要求与成果</button></article><EmptyState v-if="!tasks.length && !error" title="暂无任务" description="教师指派的任务会显示在这里。"/></template>
   </template>
   <section v-else-if="view==='机智币' && ledger" class="panel student-card"><h2>余额 {{ledger.balance}}</h2><p>机智币与四维评价独立，不自动折算为品德分数。</p><article v-for="entry in ledger.items" :key="entry.id" class="student-task"><strong>{{entry.amount>0?'+':''}}{{entry.amount}}</strong><p>{{entry.reason}}</p><small>{{entry.createdAt}}<span v-if="entry.reversed"> · 已撤销</span><span v-if="entry.reversalOf"> · 纠正流水 #{{entry.reversalOf}}</span></small></article><EmptyState v-if="!ledger.items.length" title="暂无积分流水"/><PaginationBar :page="page" :page-size="pageSize" :total="ledger.total" :disabled="loading" @change="changePage" @resize="resize"/></section>
  </div></main>
 </div>
</template>
<style scoped>
.student-role{font-size:12px;color:var(--muted)}
.student-menu-close{min-height:44px;margin:8px 12px}
.student-menu-backdrop{position:fixed;inset:48px 0 0;background:#0003;z-index:8}
.student-card{padding:24px;margin-top:20px}.student-card h2{font-size:22px;margin:12px 0}.student-card p{line-height:1.8}
.student-task{padding:16px 0;border-top:1px solid var(--border);overflow-wrap:anywhere}
.student-prose{white-space:pre-wrap}
@media(max-width:600px){.student-card{padding:16px}.student-portal .menu-item,.student-portal .view-switch{min-height:44px}.student-portal .sidebar-toggle{height:44px;width:44px}}
</style>
