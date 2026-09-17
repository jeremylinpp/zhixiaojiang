<script setup lang="ts">
import PageHeader from './ui/PageHeader.vue';
import {onMounted,ref} from 'vue';
import {request} from '../api';
type Profile={teacher:{username:string;displayName:string;role:string};classes:{id:number;name:string;grade:string;studentCount:number;isDemo:boolean}[]};
const profile=ref<Profile>(),name=ref(''),error=ref(''),notice=ref(''),loading=ref(true),saving=ref(false);
async function load(){loading.value=true;error.value='';try{profile.value=await request<Profile>('/profile');name.value=profile.value.teacher.displayName;}catch(e){error.value=(e as Error).message;}finally{loading.value=false;}}
async function save(){if(!profile.value || saving.value)return;error.value='';notice.value='';saving.value=true;try{await request('/profile','PUT',{displayName:name.value.trim(),previousDisplayName:profile.value.teacher.displayName});profile.value.teacher.displayName=name.value.trim();notice.value='资料已保存';}catch(e){error.value=(e as Error).message;}finally{saving.value=false;}}
onMounted(load);
</script>
<template>
  <section class="page-scroll workspace-page"><PageHeader title="教师资料" eyebrow="账号与任教班级" description="查看当前登录账号和由你负责的班级。"></PageHeader><p v-if="loading" role="status">正在读取资料…</p><p v-if="error" role="alert" class="profile-error">{{error}} <button class="button" :disabled="saving" @click="load">重新读取</button></p><p v-if="notice" role="status" class="profile-notice">{{notice}}</p><template v-if="profile && !loading"><section class="panel profile-card"><h2>基本资料</h2><dl><dt>登录账号</dt><dd>{{profile.teacher.username}}</dd><dt>角色</dt><dd>{{profile.teacher.role==='TEACHER'?'教师':profile.teacher.role}}</dd></dl><form class="action-form" @submit.prevent="save"><label>显示名称<input v-model="name" required maxlength="80" :disabled="saving"/></label><button class="button primary dialog-done" :disabled="saving || !name.trim() || name.trim()===profile.teacher.displayName">{{saving?'保存中…':'保存资料'}}</button></form><p class="muted profile-help">账号和权限由管理员维护，此处仅修改显示名称。</p></section><section class="profile-classes"><article v-for="room in profile.classes" :key="room.id" class="panel profile-card"><span class="status-chip success">{{room.isDemo?'演示班级':'任教班级'}}</span><h2>{{room.name}}</h2><p>{{room.grade}} · {{room.studentCount}} 名在籍学生</p></article><p v-if="!profile.classes.length" class="muted">当前账号尚未关联班级，请联系管理员。</p></section></template></section>
</template>
<style scoped>
.profile-card{padding:22px;max-width:600px}.profile-card h2{margin:4px 0 14px}.profile-card dl{display:grid;grid-template-columns:100px 1fr;gap:12px;margin:0 0 20px}.profile-card dt{color:var(--muted)}.profile-card dd{margin:0}.profile-help{font-size:13px;line-height:1.8;margin-top:16px}.profile-classes{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:16px;margin-top:20px}.profile-error{color:#b8584c;margin:14px 0}.profile-notice{color:#668254;margin:14px 0}
</style>
