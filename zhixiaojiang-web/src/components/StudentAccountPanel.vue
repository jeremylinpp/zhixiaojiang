<script lang="ts" setup>
import {onMounted, ref} from 'vue';
import {request} from '../api';

const props = defineProps<{ studentId: number }>();
const account = ref<{ exists: boolean; account?: { username: string; mustChangePassword: boolean } }>();
const username = ref(''), initialPassword = ref(''), busy = ref(false), error = ref(''), notice = ref('');

async function load() {
  try {
    account.value = await request(`/students/${props.studentId}/account`);
  } catch (e) {
    error.value = (e as Error).message;
  }
}

async function create() {
  if (busy.value) return;
  busy.value = true;
  error.value = '';
  try {
    await request(`/students/${props.studentId}/account`, 'POST', {
      username: username.value,
      initialPassword: initialPassword.value
    });
    initialPassword.value = '';
    notice.value = '账号已开通，请通过安全方式将初始登录信息交给学生。首次登录需改密。';
    await load();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}

onMounted(load);
</script>
<template>
  <section class="account-panel"><h3>学生登录账号</h3>
    <p v-if="error" role="alert">{{ error }}
      <button :disabled="busy" class="button" @click="load">重新读取</button>
    </p>
    <p v-if="notice" role="status">{{ notice }}</p>
    <p v-if="account?.exists">账号：{{ account.account?.username }} ·
      {{ account.account?.mustChangePassword ? '待首次改密' : '已启用' }}</p>
    <form v-else-if="account && !account.exists" class="action-form" @submit.prevent="create"><label>登录账号<input
        v-model="username" :disabled="busy" autocomplete="off" maxlength="64" minlength="4"
        pattern="[A-Za-z0-9][A-Za-z0-9_.\-]{3,63}" required/></label><label>初始密码<input v-model="initialPassword" :disabled="busy"
                                                                           autocomplete="new-password" maxlength="64" minlength="12"
                                                                           required
                                                                           type="password"/></label>
      <p>账号仅绑定此学生；初始密码不会在开通后再次显示，请安全交付。</p>
      <button :disabled="busy" class="button primary">{{ busy ? '正在开通…' : '开通学生账号' }}</button>
    </form>
  </section>
</template>
<style scoped>.account-panel {
  border-top: 1px solid var(--border);
  padding: 20px 0
}

.account-panel form {
  max-width: 480px
}</style>
