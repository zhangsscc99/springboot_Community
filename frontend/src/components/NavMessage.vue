<template>
  <div class="nav-message relative">
    <router-link to="/messages" class="inline-block p-2 relative">
      <svg class="w-6 h-6" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"></path>
      </svg>
      <div 
        v-if="unreadCount > 0" 
        class="unread-badge absolute top-0 right-0 transform translate-x-1/4 -translate-y-1/4 bg-red-500 text-white text-xs font-bold rounded-full min-w-[20px] h-[20px] flex items-center justify-center px-1"
      >
        {{ displayCount }}
      </div>
    </router-link>
  </div>
</template>

<script>
import { mapGetters } from 'vuex';

export default {
  name: 'NavMessage',
  computed: {
    ...mapGetters(['unreadMessageCount']),
    unreadCount() {
      return this.unreadMessageCount;
    },
    // 显示未读数，超过99显示为99+
    displayCount() {
      return this.unreadCount > 99 ? '99+' : this.unreadCount;
    }
  }
};
</script>

<style scoped>
.unread-badge {
  transition: all 0.3s ease;
}
</style> 