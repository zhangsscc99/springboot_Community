<template>
  <div class="bottom-tab-bar" v-if="!isChatView">
    <router-link to="/" class="tab-item" active-class="active">
      <i class="fas fa-home"></i>
      <span>首页</span>
    </router-link>
    
    <router-link to="/messages" class="tab-item" active-class="active">
      <div class="relative">
        <i class="fas fa-comment"></i>
        <div v-if="unreadCount > 0" class="unread-badge"></div>
      </div>
      <span>消息</span>
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
import { mapGetters } from 'vuex';

export default {
  name: 'BottomTabBar',
  computed: {
    ...mapGetters(['unreadMessageCount']),
    unreadCount() {
      return this.unreadMessageCount;
    },
    profileLink() {
      // 用户已登录，前往用户资料页，否则前往登录页
      return this.$store.getters.isAuthenticated 
        ? `/profile/${this.$store.getters.currentUser?.id || '123'}`
        : '/login';
    },
    isChatView() {
      return this.$route.name === 'chat';
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
.unread-badge {
  position: absolute;
  top: -3px;
  right: -3px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #ff4757;
}

.relative {
  position: relative;
}
</style> 