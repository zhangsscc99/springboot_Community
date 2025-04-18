<template>
  <div class="home-view">
    <div class="nav-tabs">
      <div 
        v-for="tab in tabs" 
        :key="tab" 
        class="nav-tab" 
        :class="{ active: activeTab === tab }"
        @click="switchTab(tab)"
      >
        {{ tab }}
      </div>
    </div>
    
    <div v-if="loading" class="loading-indicator">
      <i class="fas fa-spinner fa-spin"></i> 加载中...
    </div>
    
    <div v-else-if="error" class="error-message">
      <i class="fas fa-exclamation-circle"></i> {{ error }}
    </div>
    
    <div v-else class="post-list">
      <div v-for="post in currentTabPosts" :key="post.id" class="post-card" @click="goToPostDetail(post.id)">
        <h3 class="post-title">{{ post.title }}</h3>
        <div class="post-header">
          <UserAvatar 
            :src="post.author.avatar" 
            :username="post.author.username"
          />
          <div class="post-user-info">
            <h4 class="post-username">{{ post.author.username }}</h4>
            <span class="post-time">{{ formatDate(post.created_at) }}</span>
          </div>
        </div>
        <p class="post-content">{{ post.content }}</p>
        <div class="post-tags" v-if="post.tags">
          <span v-for="(tag, index) in post.tags" :key="index" class="post-tag">
            #{{ tag }}
          </span>
        </div>
        <div class="post-footer">
          <div class="post-actions">
            <div class="post-action">
              <i class="fas fa-heart"></i> {{ post.likes }}
            </div>
            <div class="post-action">
              <i class="fas fa-comment-dots"></i> {{ post.comments }}
            </div>
            <div class="post-action">
              <i class="fas fa-star"></i> {{ Math.floor(Math.random() * 100) }}
            </div>
          </div>
          <div class="post-action">
            <i class="fas fa-share-alt"></i> {{ Math.floor(Math.random() * 50) }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex';
import UserAvatar from '@/components/UserAvatar.vue';

export default {
  name: 'HomeView',
  components: {
    UserAvatar
  },
  data() {
    return {
      tabs: ['关注', '推荐', '热榜', '故事', '情感知识']
    };
  },
  computed: {
    ...mapGetters({
      posts: 'allPosts',
      loading: 'isLoading',
      error: 'error',
      activeTab: 'activeTab',
      currentTabPosts: 'currentTabPosts'
    })
  },
  methods: {
    switchTab(tab) {
      this.$store.dispatch('setActiveTab', tab);
    },
    formatDate(dateString) {
      const date = new Date(dateString);
      const now = new Date();
      const diffInSeconds = Math.floor((now - date) / 1000);
      
      if (diffInSeconds < 60) {
        return '刚刚';
      } else if (diffInSeconds < 3600) {
        return Math.floor(diffInSeconds / 60) + '分钟前';
      } else if (diffInSeconds < 86400) {
        return Math.floor(diffInSeconds / 3600) + '小时前';
      } else if (diffInSeconds < 604800) {
        return Math.floor(diffInSeconds / 86400) + '天前';
      } else {
        return `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`;
      }
    },
    goToPostDetail(postId) {
      this.$router.push({ name: 'post-detail', params: { id: postId } });
    }
  },
  created() {
    this.$store.dispatch('fetchPosts');
  }
}
</script>

<style scoped>
.home-view {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.nav-tabs {
  display: flex;
  overflow-x: auto;
  white-space: nowrap;
  background-color: white;
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 60px;
  z-index: 90;
  padding: 0 10px;
}

.nav-tab {
  padding: 12px 16px;
  font-size: 16px;
  color: var(--text-color);
  cursor: pointer;
  position: relative;
  transition: color 0.3s;
}

.nav-tab.active {
  color: var(--primary-color);
  font-weight: 500;
}

.nav-tab.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  width: 30px;
  height: 2px;
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-end));
  opacity: 0.6;
  transform: translateX(-50%);
  border-radius: 1px;
}

.post-title {
  padding: 16px 16px 12px;
  font-size: 20px;
  font-weight: 600;
  margin: 0;
  letter-spacing: -0.3px;
}

.post-header {
  padding: 0 16px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.post-user-info {
  flex: 1;
}

.post-username {
  font-weight: 500;
  font-size: 13px;
  margin: 0;
}

.post-time {
  font-size: 11px;
  color: var(--light-text-color);
}

.post-content {
  padding: 0 16px 12px;
  font-size: 14px;
  color: var(--light-text-color);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
  letter-spacing: -0.2px;
}

.post-tags {
  padding: 0 16px 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.post-tag {
  color: var(--primary-color);
  font-size: 14px;
  font-weight: 500;
}

.post-footer {
  display: flex;
  justify-content: space-between;
  padding: 12px 16px;
  border-top: 1px solid var(--border-color);
  color: var(--light-text-color);
  font-size: 14px;
}

.post-actions {
  display: flex;
  gap: 16px;
}

.post-action {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  font-size: 14px;
}

.post-action i {
  font-size: 16px;
}

.post-action:hover {
  color: var(--primary-color);
}

.loading-indicator,
.error-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 50px 0;
  color: var(--light-text-color);
  font-size: 16px;
}

.loading-indicator i,
.error-message i {
  font-size: 32px;
  margin-bottom: 15px;
}

.error-message {
  color: var(--error-color);
}
</style> 