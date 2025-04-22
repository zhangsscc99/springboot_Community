<template>
  <div class="app-container">
    <header class="app-header">
      <div class="logo">
        <div class="main-title">锦书情感社区</div>
        <div class="subtitle">
          <span class="subtitle-text">锦书</span>
          <span class="logo-ai">AI</span>
          <span class="subtitle-text">的旗下论坛</span>
        </div>
      </div>
      
      <div class="search-box">
        <i class="fas fa-search search-icon"></i>
        <input type="text" class="search-input" placeholder="搜索" />
      </div>
      
      <div class="user-controls">
        <div class="auth-buttons" v-if="!isAuthenticated">
          <button class="btn" @click="goToLogin">登录</button>
          <button class="btn btn-primary" @click="goToRegister">注册</button>
        </div>
        <div v-else class="user-info">
          <span class="username">{{ currentUser?.username || 'User' }}</span>
          <button class="btn" @click="logout">退出</button>
        </div>
      </div>
    </header>
    
    <div class="main-content">
      <keep-alive>
        <router-view v-if="$route.meta.keepAlive" :key="$route.fullPath"/>
      </keep-alive>
      <router-view v-if="!$route.meta.keepAlive" :key="$route.fullPath"/>
    </div>
    
    <BottomTabBar />
  </div>
</template>

<script>
import BottomTabBar from '@/components/BottomTabBar.vue';
import { mapGetters } from 'vuex';

export default {
  name: 'App',
  components: {
    BottomTabBar
  },
  computed: {
    ...mapGetters({
      isAuthenticated: 'isAuthenticated',
      currentUser: 'currentUser',
      actionLoading: 'isActionLoading'
    })
  },
  methods: {
    logout() {
      this.$store.dispatch('logout');
      if (this.$route.path.includes('/profile') || this.$route.path.includes('/create-post')) {
        this.$router.push('/');
      }
    },
    goToLogin() {
      this.$router.push('/login');
    },
    goToRegister() {
      this.$router.push('/register');
    }
  },
  watch: {
    actionLoading(newVal) {
      document.body.setAttribute('data-action-loading', newVal);
    }
  }
}
</script>

<style scoped>
.logo {
  display: flex;
  flex-direction: column;
  font-weight: bold;
}

.main-title {
  font-size: 22px;
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-end));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  color: transparent;
}

.subtitle {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: var(--light-text-color);
  margin-top: 2px;
}

.subtitle-text {
  color: var(--light-text-color);
}

.logo-ai {
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-end));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  color: transparent;
  margin: 0 2px;
}
</style> 