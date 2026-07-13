import { createApp } from 'vue'
import { createPinia } from 'pinia'
import {
  ElButton,
  ElCard,
  ElCheckbox,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
} from 'element-plus'

import App from './App.vue'
import router from './router'
import './assets/main.css'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/card/style/css'
import 'element-plus/es/components/checkbox/style/css'
import 'element-plus/es/components/form/style/css'
import 'element-plus/es/components/form-item/style/css'
import 'element-plus/es/components/input/style/css'
import 'element-plus/es/components/message/style/css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElButton)
app.use(ElCard)
app.use(ElCheckbox)
app.use(ElForm)
app.use(ElFormItem)
app.use(ElInput)

app.config.globalProperties.$message = ElMessage

app.mount('#app')
