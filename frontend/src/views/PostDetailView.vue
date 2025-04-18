<template>
  <div class="post-detail-view">
    <div class="back-nav">
      <button class="back-button" @click="goBack">
        <i class="fas fa-arrow-left"></i> 返回
      </button>
    </div>
    
    <div v-if="loading" class="loading-indicator">
      <i class="fas fa-spinner fa-spin"></i> 加载中...
    </div>
    
    <div v-else-if="error" class="error-message">
      <i class="fas fa-exclamation-circle"></i> {{ error }}
    </div>
    
    <div v-else-if="post" class="post-detail">
      <div class="post-header">
        <UserAvatar 
          :src="post.author.avatar" 
          :username="post.author.username"
        />
        <div class="post-user-info">
          <h4 class="post-username">{{ post.author.username }}</h4>
          <span class="post-time">{{ formatDate(post.created_at) }}</span>
        </div>
        <button class="follow-button">
          <i class="fas fa-plus"></i> 关注
        </button>
      </div>
      
      <h1 class="post-title">{{ post.title }}</h1>
      
      <div class="post-content-full">
        <p v-for="(paragraph, index) in contentParagraphs" :key="index">{{ paragraph }}</p>
      </div>
      
      <div class="post-actions-bar">
        <div class="post-action">
          <i class="far fa-heart"></i> {{ post.likes }}
        </div>
        <div class="post-action">
          <i class="far fa-comment"></i> {{ post.comments }}
        </div>
        <div class="post-action">
          <i class="far fa-bookmark"></i>
        </div>
        <div class="post-action">
          <i class="fas fa-share-alt"></i>
        </div>
      </div>
      
      <div class="comments-section">
        <h3 class="comments-title">评论 ({{ post.commentList ? post.commentList.length : 0 }})</h3>
        
        <div class="comment-input">
          <textarea placeholder="添加评论..." v-model="newComment" rows="3"></textarea>
          <button class="comment-submit" :disabled="!newComment.trim()">发布</button>
        </div>
        
        <div class="comments-list" v-if="post.commentList && post.commentList.length > 0">
          <div v-for="comment in post.commentList" :key="comment.id" class="comment-item">
            <div class="comment-header">
              <UserAvatar 
                :src="comment.author.avatar" 
                :username="comment.author.username"
              />
              <div class="comment-user-info">
                <h4 class="comment-username">{{ comment.author.username }}</h4>
                <span class="comment-time">{{ formatDate(comment.created_at) }}</span>
              </div>
            </div>
            <div class="comment-content">{{ comment.content }}</div>
            <div class="comment-actions">
              <div class="comment-action"><i class="far fa-heart"></i> 点赞</div>
              <div class="comment-action"><i class="far fa-comment"></i> 回复</div>
            </div>
          </div>
        </div>
        
        <div v-else class="no-comments">
          还没有评论，来发表第一条吧
        </div>
      </div>
    </div>
    
    <div v-else class="not-found">
      <i class="fas fa-exclamation-triangle"></i>
      <p>帖子不存在或已被删除</p>
      <button class="btn btn-primary" @click="goHome">返回首页</button>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex';
import UserAvatar from '@/components/UserAvatar.vue';

export default {
  name: 'PostDetailView',
  components: {
    UserAvatar
  },
  data() {
    return {
      newComment: ''
    };
  },
  computed: {
    ...mapGetters({
      post: 'currentPost',
      loading: 'isLoading',
      error: 'error'
    }),
    contentParagraphs() {
      return this.post ? this.post.content.split('\n\n') : [];
    }
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
    goBack() {
      this.$router.go(-1);
    },
    goHome() {
      this.$router.push({ name: 'home' });
    }
  },
  created() {
    const postId = parseInt(this.$route.params.id);
    this.$store.dispatch('fetchPostById', postId);
  }
}
</script>

<style scoped>
.post-detail-view {
  flex: 1;
  padding: 16px;
  background-color: var(--background-color);
}

.back-nav {
  margin-bottom: 16px;
}

.back-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 80px;
  height: 36px;
  padding: 0 16px;
  gap: 4px;
  border: 1px solid var(--primary-color);
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  background-color: white;
  margin-bottom: 20px;
  color: var(--primary-color);
  transition: all 0.2s ease;
}

.back-button:hover {
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-end));
  border-color: transparent;
  color: white;
}

.back-button i {
  margin-right: 8px;
}

.loading-indicator,
.error-message,
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 50px 0;
  color: var(--light-text-color);
  text-align: center;
}

.loading-indicator i,
.error-message i,
.not-found i {
  font-size: 32px;
  margin-bottom: 15px;
}

.error-message {
  color: var(--error-color);
}

.not-found button {
  margin-top: 20px;
}

.post-detail {
  background-color: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.post-header {
  padding: 16px;
  display: flex;
  align-items: center;
}

.post-user-info {
  flex: 1;
  margin-left: 10px;
}

.post-username {
  font-weight: 500;
  font-size: 14px;
  margin: 0;
}

.post-time {
  font-size: 12px;
  color: var(--light-text-color);
}

.follow-button {
  padding: 6px 12px;
  background-color: var(--secondary-color);
  border: none;
  border-radius: 20px;
  color: var(--text-color);
  font-size: 12px;
  cursor: pointer;
}

.follow-button i {
  margin-right: 4px;
}

.post-title {
  padding: 0 16px 16px;
  font-size: 22px;
  font-weight: 600;
  margin: 0;
}

.post-content-full {
  padding: 0 16px;
  font-size: 16px;
  color: var(--text-color);
  line-height: 1.6;
}

.post-content-full p {
  margin-bottom: 16px;
}

.post-actions-bar {
  display: flex;
  justify-content: space-around;
  padding: 16px;
  border-top: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
  margin-top: 16px;
}

.post-action {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--light-text-color);
  cursor: pointer;
}

.post-action:hover {
  color: var(--primary-color);
}

.comments-section {
  padding: 16px;
}

.comments-title {
  font-size: 18px;
  font-weight: 500;
  margin-bottom: 16px;
}

.comment-input {
  display: flex;
  flex-direction: column;
  margin-bottom: 24px;
}

.comment-input textarea {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 12px;
  resize: none;
  font-size: 14px;
  margin-bottom: 10px;
}

.comment-input textarea:focus {
  outline: none;
  border-color: var(--primary-color);
}

.comment-submit {
  align-self: flex-end;
  padding: 8px 16px;
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-end));
  color: white;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
}

.comment-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 16px;
}

.comment-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.comment-user-info {
  margin-left: 10px;
}

.comment-username {
  font-weight: 500;
  font-size: 14px;
  margin: 0;
}

.comment-time {
  font-size: 12px;
  color: var(--light-text-color);
}

.comment-content {
  font-size: 14px;
  margin-bottom: 8px;
  line-height: 1.5;
}

.comment-actions {
  display: flex;
  gap: 16px;
}

.comment-action {
  font-size: 12px;
  color: var(--light-text-color);
  cursor: pointer;
}

.comment-action:hover {
  color: var(--primary-color);
}

.no-comments {
  text-align: center;
  color: var(--light-text-color);
  padding: 30px 0;
  font-style: italic;
}

@media (max-width: 768px) {
  .post-title {
    font-size: 20px;
  }
  
  .post-content-full {
    font-size: 15px;
  }
}
</style> 