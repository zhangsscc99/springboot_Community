import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import './assets/css/main.css'

// 创建应用实例
const app = createApp(App);

// 使用插件
app.use(store);
app.use(router);

// 初始化应用 - 恢复用户会话
store.dispatch('initializeApp').finally(() => {
  // 挂载应用
  app.mount('#app');
}); 