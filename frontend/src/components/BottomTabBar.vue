<template>
  <div class="bottom-tab-bar">
    <router-link to="/" class="tab-item" active-class="active">
      <i class="fas fa-home"></i>
      <span>首页</span>
    </router-link>
    
    <div class="tab-item create-button">
      <div class="create-button-circle" @click="openCreatePost">
        <i class="fas fa-plus"></i>
      </div>
    </div>
    
    <router-link :to="profileLink" class="tab-item" active-class="active">
      <i class="fas fa-user"></i>
      <span>我</span>
    </router-link>
  </div>
</template>

<script>
export default {
  name: 'BottomTabBar',
  computed: {
    profileLink() {
      // 用户已登录，前往用户资料页，否则前往登录页
      return this.$store.getters.isAuthenticated 
        ? `/profile/${this.$store.getters.currentUser?.id || '123'}`
        : '/login';
    }
  },
  methods: {
    openCreatePost() {
      // 如果用户未登录，先引导至登录页
      if (!this.$store.getters.isAuthenticated) {
        this.$router.push('/login');
        return;
      }
      
      this.$router.push('/create-post');
    }
  }
}
</script>

<style scoped>
/* 样式已移至全局CSS文件(main.css) */
</style> 