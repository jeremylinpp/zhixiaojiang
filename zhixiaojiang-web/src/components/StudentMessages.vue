<script setup lang="ts">
import {onMounted,ref} from 'vue';
import {request} from '../api';
import EmptyState from './ui/EmptyState.vue';
import PaginationBar from './ui/PaginationBar.vue';
const emit=defineEmits<{navigate:[destination:string]}>();
type Message={id:number;title:string;content:string;destination:string;createdAt:string;readAt:string|null};
const items=ref<Message[]>([]),total=ref(0),unread=ref(0),page=ref(1),pageSize=ref(20),busy=ref(false),error=ref('');
async function load(){busy.value=true;error.value='';try{const result=await request<{items:Message[];total:number;unread:number}>(`/student-portal/messages?page=${page.value}&pageSize=${pageSize.value}`);items.value=result.items;total.value=result.total;unread.value=result.unread;}catch(e){error.value=(e as Error).message;}finally{busy.value=false;}}
async function read(item:Message,open:boolean){if(busy.value)return;busy.value=true;error.value='';try{await request(`/student-portal/messages/${item.id}/read`,'POST',{});if(open)emit('navigate',item.destination);else await load();}catch(e){error.value=(e as Error).message;}finally{busy.value=false;}}
function change(value:number){page.value=value;load();}
function resize(value:number){pageSize.value=value;change(1);}
onMounted(load);
</script>
<template><section><h2>消息 · {{unread}} 条未读</h2><p v-if="error" role="alert">{{error}} <button class="button" @click="load">重试</button></p><p v-if="busy" role="status">正在处理消息…</p><article v-for="item in items" :key="item.id" class="panel message-card"><h3>{{item.title}} <small>{{item.readAt?'已读':'未读'}}</small></h3><p>{{item.content}}</p><p>{{item.createdAt}}</p><button class="button" :disabled="busy" @click="read(item,true)">查看相关内容</button><button v-if="!item.readAt" class="button" :disabled="busy" @click="read(item,false)">标为已读</button></article><EmptyState v-if="!items.length && !busy && !error" title="暂无消息" description="任务、成长审核与计划反馈会显示在这里。"/><PaginationBar :page="page" :page-size="pageSize" :total="total" :disabled="busy" @change="change" @resize="resize"/></section></template>
<style scoped>.message-card{padding:20px;margin:16px 0}.message-card p{line-height:1.8}.message-card button+button{margin-left:8px}.message-card small{font-size:12px;color:var(--muted)}</style>
