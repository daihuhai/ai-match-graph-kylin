import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { ElMessage } from 'element-plus'
import { createPinia } from 'pinia'
import { permissionDirective } from '@/directives/permission'

const app = createApp(App)

const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.directive('permission', permissionDirective)

app.config.errorHandler = (err) => {
  const msg = err instanceof Error ? err.message : '发生未知错误'
  ElMessage.error(msg)
}

window.addEventListener('unhandledrejection', (e) => {
  const r = (e as PromiseRejectionEvent).reason
  const msg = r instanceof Error ? r.message : '请求失败或发生异常'
  ElMessage.error(msg)
})

app.mount('#app')
