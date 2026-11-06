<script setup lang="ts">
import { ref } from 'vue'

const health = ref<Record<string, string>>({})

async function check() {
  try {
    const res = await fetch('/api/Health')
    console.log('响应对象', res)
    if (!res.ok) {
      throw new Error(`http状态码：${res.status}`)
    }
    const data = await res.json()
    health.value = data
  } catch (err) {
    console.error('请求失败原因：', err) // F12控制台看这个！
    health.value = { api: 'fail' }
  }
}


</script>

<template>
  <h3>骨架自检</h3>
  <button @click="check">检查后端</button>
  <ul>
    <li v-for="(value, key) in health" :key="key">
      {{ key }}:
      <span :style="{ color: value === 'ok' ? '#2e7d32' : '#c62828' }">●</span>
      {{ value }}
    </li>
  </ul>
</template>
