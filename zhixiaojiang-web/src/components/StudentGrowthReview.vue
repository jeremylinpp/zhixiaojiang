<script setup lang="ts">
import {onMounted,ref} from 'vue';
import {request} from '../api';
const props=defineProps<{studentId:number}>();
type Item={id:number;title:string;content:string;category:string;occurredOn:string;status:string;feedback?:string;previousId?:number;replacedBy?:number};
const items=ref<Item[]>([]),notes=ref<Record<number,string>>({}),busy=ref(false),error=ref('');
const labels:Record<string,string>={PENDING:'待审核',APPROVED:'已通过',RETURNED:'需补充'};
async function load(){try{items.value=(await request<{items:Item[]}>(`/students/${props.studentId}/growth-submissions`)).items;}catch(e){error.value=(e as Error).message;}}
async function review(item:Item,status:string){if(busy.value)return;busy.value=true;error.value='';try{await request(`/growth-submissions/${item.id}/review`,'POST',{status,feedback:notes.value[item.id]?.trim()});await load();}catch(e){error.value=(e as Error).message;}finally{busy.value=false;}}
onMounted(load);
</script>
<template><section class="growth-review panel"><h3>学生提交的成长记录</h3><p v-if="error" role="alert">{{error}}</p><p v-if="!items.length && !error">暂无学生提交。</p><article v-for="item in items" :key="item.id"><strong>{{item.title}} · {{labels[item.status]}}</strong><p>{{item.occurredOn}} · {{item.category}}</p><p v-if="item.previousId">补充自记录 #{{item.previousId}}</p><p v-if="item.replacedBy">已补充为记录 #{{item.replacedBy}}</p><p class="prose">{{item.content}}</p><p v-if="item.feedback">审核反馈：{{item.feedback}}</p><form v-if="item.status==='PENDING'" class="action-form" @submit.prevent="review(item,'APPROVED')"><label>给学生的审核反馈<textarea v-model="notes[item.id]" required maxlength="500" rows="2"/></label><div><button class="button primary" :disabled="busy || !notes[item.id]?.trim()">通过</button><button type="button" class="button" :disabled="busy || !notes[item.id]?.trim()" @click="review(item,'RETURNED')">退回补充</button></div></form></article></section></template>
<style scoped>.growth-review{padding:20px;margin:20px 0}.growth-review article{padding:16px 0;border-top:1px solid var(--border)}.prose{white-space:pre-wrap;overflow-wrap:anywhere}.growth-review button+button{margin-left:8px}</style>
