<template>
  <div class="app-container">
    <header class="app-header">
      <h1>社区论坛</h1>
      
      <div class="search-box">
        <i class="fas fa-search search-icon"></i>
        <input type="text" class="search-input" placeholder="搜索" />
      </div>
      
      <div class="user-controls">
        <div class="auth-buttons" v-if="!isLoggedIn">
          <button class="btn">登录</button>
          <button class="btn btn-primary">注册</button>
        </div>
        <div v-else class="user-info">
          <span class="username">{{ username }}</span>
          <button class="btn" @click="logout">退出</button>
        </div>
      </div>
    </header>
    
    <router-view />
    
    <button class="create-post-btn" @click="openCreatePost">
      <i class="fas fa-plus"></i>
    </button>
  </div>
</template>

<script>
export default {
  name: 'App',
  data() {
    return {
      isLoggedIn: false,
      username: 'User123'
    }
  },
  methods: {
    logout() {
      this.isLoggedIn = false;
      // Add real logout logic here
    },
    openCreatePost() {
      // Navigate to create post page
      this.$router.push('/create-post');
    }
  }
}
</script>

<style>
/* Global styles are imported in main.js */
</style> 