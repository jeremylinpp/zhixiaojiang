<script setup lang="ts">
import {ref} from 'vue';
import {request} from '../api';
defineProps<{required?:boolean}>();
const currentPassword=ref(''),newPassword=ref(''),confirmation=ref(''),busy=ref(false),error=ref('');
async function save(){
 if(busy.value)return;error.value='';
 if(newPassword.value!==confirmation.value){error.value='两次输入的新密码不一致';return;}
 busy.value=true;
 try{await request('/student-portal/password','POST',{currentPassword:currentPassword.value,newPassword:newPassword.value});currentPassword.value='';newPassword.value='';confirmation.value='';location.href='/login?passwordChanged=1';}
 catch(e){error.value=(e as Error).message;}finally{busy.value=false;}
}
</script>
<template><section class="panel password-panel"><h2>{{required?'首次登录，请设置新密码':'修改密码'}}</h2><p>新密码需为 12–64 个字符。保存后所有旧会话失效，请使用新密码重新登录。</p><form class="action-form" @submit.prevent="save"><label>当前密码<input v-model="currentPassword" type="password" autocomplete="current-password" required maxlength="64" :disabled="busy"/></label><label>新密码<input v-model="newPassword" type="password" autocomplete="new-password" required minlength="12" maxlength="64" :disabled="busy"/></label><label>确认新密码<input v-model="confirmation" type="password" autocomplete="new-password" required minlength="12" maxlength="64" :disabled="busy"/></label><p v-if="error" role="alert">{{error}}</p><button class="button primary" :disabled="busy">{{busy?'正在保存…':'保存并重新登录'}}</button></form></section></template>
<style scoped>.password-panel{padding:24px;max-width:560px;margin:24px auto}.password-panel p{line-height:1.8}.password-panel input,.password-panel button{min-height:44px}</style>
