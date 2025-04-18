<template>
  <div class="home-view">
    <div class="nav-tabs">
      <div class="nav-tab active">关注</div>
      <div class="nav-tab">推荐</div>
      <div class="nav-tab">热榜</div>
      <div class="nav-tab">故事</div>
      <div class="nav-tab">情感知识</div>
    </div>
    
    <div v-if="loading" class="loading-indicator">
      <i class="fas fa-spinner fa-spin"></i> 加载中...
    </div>
    
    <div v-else-if="error" class="error-message">
      <i class="fas fa-exclamation-circle"></i> {{ error }}
    </div>
    
    <div v-else class="post-list">
      <div v-for="post in posts" :key="post.id" class="post-card" @click="goToPostDetail(post.id)">
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
        <h3 class="post-title">{{ post.title }}</h3>
        <p class="post-content">{{ post.content }}</p>
        <div class="post-footer">
          <div class="post-actions">
            <div class="post-action">
              <i class="far fa-heart"></i> {{ post.likes }}
            </div>
            <div class="post-action">
              <i class="far fa-comment"></i> {{ post.comments }}
            </div>
            <div class="post-action">
              <i class="far fa-bookmark"></i>
            </div>
          </div>
          <div class="post-action">
            <i class="fas fa-share-alt"></i>
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
  computed: {
    ...mapGetters({
      posts: 'allPosts',
      loading: 'isLoading',
      error: 'error'
    })
  },
  methods: {
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