<script setup lang="ts">
import PageHeader from './ui/PageHeader.vue';
import MetricStrip from './ui/MetricStrip.vue';
import PaginationBar from './ui/PaginationBar.vue';
import EmptyState from './ui/EmptyState.vue';
import {ref,watch,onMounted} from 'vue';
import StudentPicker from './StudentPicker.vue';
import {request} from '../api';
type Entry={id:number;amount:number;reason:string;category:string;createdAt:string;reversed:boolean;reversalOf:number|null};
type Rule={id:number;name:string;amount:number;description:string};
const pageSize=ref(20);
const studentId=ref(0),page=ref(1),total=ref(0),balance=ref(0);
const items=ref<Entry[]>([]),rules=ref<Rule[]>([]);
const loading=ref(false),busy=ref(false),error=ref(''),formError=ref(''),notice=ref('');
const ruleId=ref(0),amount=ref(1),reason=ref('');
const editor=ref<HTMLDialogElement>(),reversing=ref<Entry>();
let generation=0,requestKey='';
const submitted=ref<Record<string,unknown>>();
async function load(){
  const current=++generation;
  items.value=[];error.value='';
  if(!studentId.value){total.value=0;balance.value=0;loading.value=false;return;}
  loading.value=true;
  try{
    const data=await request<{items:Entry[];total:number;balance:number}>(`/students/${studentId.value}/points?page=${page.value}&pageSize=${pageSize.value}`);
    if(current!==generation)return;
    items.value=data.items;total.value=data.total;balance.value=data.balance;
    const lastPage=Math.max(1,Math.ceil(data.total/pageSize.value));
    if(page.value>lastPage){page.value=lastPage;await load();}
  }catch(e){if(current===generation)error.value=(e as Error).message;}finally{if(current===generation)loading.value=false;}
}
function openAward(){reversing.value=undefined;ruleId.value=0;amount.value=1;reason.value='';formError.value='';submitted.value=undefined;requestKey=crypto.randomUUID();editor.value?.showModal();}
function openReverse(entry:Entry){reversing.value=entry;formError.value='';editor.value?.showModal();}
function selectRule(){const rule=rules.value.find(r=>r.id===ruleId.value);if(rule){amount.value=rule.amount;reason.value=rule.name;}}
async function save(){
  if(busy.value)return;
  formError.value='';busy.value=true;
  try{
    if(reversing.value){
      const result=await request<{saved:boolean}>(`/points/${reversing.value.id}/reverse`,'POST');
      notice.value=result.saved?'已生成反向流水，原记录保留。':'该记录已撤销，未重复生成流水。';
    }else{
      if(!submitted.value){
        if(!reason.value.trim())throw new Error('请填写积分依据');
        if(!ruleId.value && (!Number.isInteger(amount.value) || amount.value===0))throw new Error('积分必须是非零整数');
        submitted.value={studentId:studentId.value,amount:amount.value,ruleId:ruleId.value || null,reason:reason.value.trim(),idempotencyKey:requestKey};
      }
      const result=await request<{saved:boolean}>('/points','POST',submitted.value);
      notice.value=result.saved?'积分已保存。':'该请求已保存，未重复发币。';
    }
    editor.value?.close();page.value=1;await load();
  }catch(e){formError.value=(e as Error).message;}finally{busy.value=false;}
}
watch(studentId,()=>{page.value=1;notice.value='';load();});
function turn(next:number){page.value=next;load();}
function resize(size:number){pageSize.value=size;page.value=1;load();}
onMounted(async()=>{try{rules.value=(await request<{items:Rule[]}>('/point-rules')).items;}catch(e){error.value=`积分规则加载失败：${(e as Error).message}`;}});
</script>
<template>
  <section class="page-scroll workspace-page">
    <PageHeader title="机智币" eyebrow="过程评价与正向激励" description="积分独立于四维评价，不自动换算为品德得分。"><button class="button primary" :disabled="!studentId || loading" @click="openAward">新增积分记录</button></PageHeader>
    <StudentPicker v-model="studentId" :disabled="busy"/>
    <p v-if="notice" class="points-notice" role="status">{{notice}}</p>
    <p v-if="error" class="points-error" role="alert">{{error}} <button class="button" @click="load">重新读取</button></p>
    <EmptyState v-if="!studentId" title="请选择学生" description="选择学生后查看真实余额和积分流水。"/>
    <template v-else>
      <p v-if="loading" role="status">正在读取积分…</p>
      <template v-else-if="!error">
        <MetricStrip :items="[{label:'当前余额',value:balance,description:'机智币，不折算为四维评价'},{label:'积分流水',value:total,description:'保留原始记录与撤销记录'}]"/>
        <section class="data-card panel"><table><thead><tr><th>时间</th><th>积分依据</th><th>变化</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="entry in items" :key="entry.id"><td>{{entry.createdAt.replace('T',' ')}}</td><td>{{entry.reason}}<small v-if="entry.reversalOf">关联原流水 #{{entry.reversalOf}}</small></td><td :class="entry.amount>0?'points-positive':'points-error'">{{entry.amount>0?'+':''}}{{entry.amount}}</td><td>{{entry.reversalOf?'反向流水':entry.reversed?'已撤销':'已入账'}}</td><td><button v-if="!entry.reversed && !entry.reversalOf" class="table-action" @click="openReverse(entry)">撤销纠错</button><span v-else>—</span></td></tr><tr v-if="!items.length"><td colspan="5" class="empty-cell"><EmptyState title="尚无积分流水" description="请调整筛选条件，或通过本页操作录入记录。"/></td></tr></tbody></table></section>
        <PaginationBar :page="page" :page-size="pageSize" :total="total" :disabled="loading || busy" @change="turn" @resize="resize"/>
      </template>
    </template>
    <dialog ref="editor" @cancel="busy && $event.preventDefault()"><header class="dialog-heading"><h2>{{reversing?'撤销积分':'新增积分记录'}}</h2><button type="button" class="button" :disabled="busy" @click="editor?.close()">关闭</button></header><form class="action-form" @submit.prevent="save"><p v-if="reversing">确认撤销“{{reversing.reason}}”（{{reversing.amount}} 币）？系统会新增 {{-reversing.amount}} 币的反向流水，不删除原记录。</p><template v-else><fieldset :disabled="busy || !!submitted"><label>积分规则<select v-model.number="ruleId" @change="selectRule"><option :value="0">手工调整</option><option v-for="rule in rules" :key="rule.id" :value="rule.id">{{rule.name}}（{{rule.amount>0?'+':''}}{{rule.amount}}）</option></select></label><label>积分变化<input v-model.number="amount" type="number" min="-1000" max="1000" step="1" :readonly="!!ruleId" required/></label><label>积分依据<textarea v-model="reason" maxlength="160" rows="3" required/></label></fieldset><small v-if="submitted">本次提交内容已锁定，重试使用同一请求编号，避免重复发币。</small></template><p v-if="formError" role="alert" class="points-error">{{formError}}</p><button class="button primary dialog-done" :disabled="busy">{{busy?'保存中…':reversing?'确认撤销':submitted?'重试本次提交':'保存积分'}}</button></form></dialog>
  </section>
</template>
<style scoped>
.points-summary{display:flex;align-items:baseline;gap:14px;padding:18px;margin:18px 0}.points-summary strong{font:700 30px 'JetBrains Mono Variable',monospace}.points-summary span{color:var(--muted)}.points-notice,.points-positive{color:#668254}.points-notice,.points-error{margin:12px 0}.points-error{color:#b8584c}.points-empty{padding:30px 0;color:var(--muted)}td small{display:block;color:var(--muted);font-size:11px}fieldset{border:0;margin:0;padding:0;display:grid;gap:14px;min-width:0}select{font:inherit;padding:10px;border:1px solid var(--border);background:#fff;border-radius:6px}
</style>
