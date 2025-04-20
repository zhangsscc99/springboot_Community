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
      <h1 class="post-title">{{ post.title }}</h1>
      
      <div class="post-header">
        <UserAvatar 
          :src="post.author.avatar" 
          :username="post.author.username"
        />
        <div class="post-user-info">
          <h4 class="post-username">{{ post.author.username }}</h4>
          <span class="post-time">{{ formattedDate }}</span>
        </div>
        <button class="follow-button">
          <i class="fas fa-plus"></i> 关注
        </button>
      </div>
      
      <div class="post-content-full">
        <p v-for="(paragraph, index) in contentParagraphs" :key="index">{{ paragraph }}</p>
      </div>
      
      <div class="post-tags" v-if="post.tags">
        <span v-for="(tag, index) in post.tags" :key="index" class="post-tag">
          #{{ tag }}
        </span>
      </div>
      
      <div class="post-actions-bar">
        <div class="post-action" :class="{ active: isLiked }" @click="handleLike">
          <i class="fas fa-heart"></i>
          <span>{{ post.likes || 0 }}</span>
        </div>
        <div class="post-action" @click="scrollToComments">
          <i class="fas fa-comment"></i>
          <span>{{ post.comments || 0 }}</span>
        </div>
        <div class="post-action" :class="{ active: isFavorited }" @click="handleFavorite">
          <i class="fas fa-star"></i>
          <span>{{ post.favorites || 0 }}</span>
        </div>
        <div class="post-action">
          <i class="fas fa-eye"></i>
          <span>{{ post.views || 0 }}</span>
        </div>
      </div>
      
      <div class="comments-section" ref="commentsSection">
        <h3 class="comments-title">评论 ({{ post.comments || 0 }})</h3>
        
        <div class="comment-input">
          <textarea placeholder="添加评论..." v-model="newComment" rows="3"></textarea>
          <button class="comment-submit" :disabled="!newComment.trim() || !isAuthenticated" @click="submitComment">
            {{ isAuthenticated ? '发布' : '请先登录' }}
          </button>
        </div>
        
        <div class="comments-list" v-if="comments && comments.length > 0">
          <div v-for="comment in comments" :key="comment.id" class="comment-item">
            <div class="comment-header">
              <UserAvatar 
                :src="comment.author.avatar" 
                :username="comment.author.username"
              />
              <div class="comment-user-info">
                <h4 class="comment-username">{{ comment.author.username }}</h4>
                <span class="comment-time">{{ formattedDate(comment.created_at) }}</span>
              </div>
            </div>
            <div class="comment-content">{{ comment.content }}</div>
            <div class="comment-actions">
              <div class="comment-action"><i class="fas fa-heart"></i> 点赞</div>
              <div class="comment-action"><i class="fas fa-reply"></i> 回复</div>
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
import { mapState, mapGetters, mapActions } from 'vuex';
import { formatDistanceToNow } from 'date-fns';
import { zhCN } from 'date-fns/locale';
import UserAvatar from '@/components/UserAvatar.vue';

export default {
  name: 'PostDetailView',
  components: {
    UserAvatar
  },
  data() {
    return {
      post: null,
      loading: true,
      error: null,
      similarPosts: [],
      newComment: '',
      isLiked: false,
      isFavorited: false,
      comments: []
    };
  },
  computed: {
    ...mapState(['user']),
    ...mapGetters(['isAuthenticated']),
    postId() {
      return this.$route.params.id;
    },
    formattedDate() {
      if (!this.post || !this.post.createdAt) return '';
      return formatDistanceToNow(new Date(this.post.createdAt), { 
        addSuffix: true,
        locale: zhCN
      });
    },
    contentParagraphs() {
      return this.post ? this.post.content.split('\n\n') : [];
    }
  },
  methods: {
    ...mapActions([
      'fetchPostById', 
      'likePost', 
      'unlikePost', 
      'favoritePost', 
      'unfavoritePost',
      'createComment',
      'fetchComments'
    ]),
    async fetchPost() {
      this.loading = true;
      try {
        const response = await this.fetchPostById(this.postId);
        this.post = response;
        
        // 检查帖子是否已被当前用户点赞和收藏
        if (this.isAuthenticated) {
          this.isLiked = this.post.likedByCurrentUser || false;
          this.isFavorited = this.post.favoritedByCurrentUser || false;
        }
        
        // 获取评论
        this.comments = await this.fetchComments(this.postId);
        
        this.error = null;
      } catch (error) {
        console.error('获取帖子详情失败:', error);
        this.error = '获取帖子失败，请稍后再试';
      } finally {
        this.loading = false;
      }
    },
    // 处理点赞
    async handleLike() {
      if (!this.isAuthenticated) {
        this.$router.push('/login?redirect=' + this.$route.fullPath);
        return;
      }
      
      try {
        if (this.isLiked) {
          // 取消点赞
          await this.unlikePost(this.postId);
          this.isLiked = false;
          // 直接更新帖子点赞数
          this.post.likes = Math.max(0, (this.post.likes || 0) - 1);
        } else {
          // 点赞
          await this.likePost(this.postId);
          this.isLiked = true;
          // 直接更新帖子点赞数
          this.post.likes = (this.post.likes || 0) + 1;
        }
        // 重新获取帖子详情以更新状态
        await this.fetchPost();
      } catch (error) {
        console.error('点赞操作失败:', error);
      }
    },
    
    // 处理收藏
    async handleFavorite() {
      if (!this.isAuthenticated) {
        this.$router.push('/login?redirect=' + this.$route.fullPath);
        return;
      }
      
      try {
        if (this.isFavorited) {
          // 取消收藏
          await this.unfavoritePost(this.postId);
          this.isFavorited = false;
          // 直接更新帖子收藏数
          this.post.favorites = Math.max(0, (this.post.favorites || 0) - 1);
        } else {
          // 收藏
          await this.favoritePost(this.postId);
          this.isFavorited = true;
          // 直接更新帖子收藏数
          this.post.favorites = (this.post.favorites || 0) + 1;
        }
        // 重新获取帖子详情以更新状态
        await this.fetchPost();
      } catch (error) {
        console.error('收藏操作失败:', error);
      }
    },
    
    // 滚动到评论区
    scrollToComments() {
      if (this.$refs.commentsSection) {
        this.$refs.commentsSection.scrollIntoView({ behavior: 'smooth' });
      }
    },
    
    // 提交评论
    async submitComment() {
      if (!this.isAuthenticated) {
        this.$router.push('/login?redirect=' + this.$route.fullPath);
        return;
      }
      
      if (!this.newComment.trim()) {
        return;
      }
      
      try {
        await this.createComment({
          postId: this.postId,
          commentData: { content: this.newComment.trim() }
        });
        
        // 清空评论框
        this.newComment = '';
        
        // 重新获取评论和帖子数据
        this.comments = await this.fetchComments(this.postId);
        await this.fetchPost();
      } catch (error) {
        console.error('提交评论失败:', error);
      }
    },
    goBack() {
      this.$router.go(-1);
    },
    goHome() {
      this.$router.push({ name: 'home' });
    }
  },
  async created() {
    await this.fetchPost();
    
    // 获取类似帖子
    try {
      // 这里可以添加获取类似帖子的逻辑
    } catch (error) {
      console.error('获取相似帖子失败:', error);
    }
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
  padding: 0 16px 16px;
  display: flex;
  align-items: center;
}

.post-user-info {
  flex: 1;
  margin-left: 8px;
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

.follow-button {
  padding: 6px 12px;
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-end));
  border: none;
  border-radius: 20px;
  color: white;
  font-size: 12px;
  cursor: pointer;
}

.follow-button i {
  margin-right: 4px;
}

.post-title {
  margin-bottom: 16px;
  padding: 16px 16px 0;
  font-size: 24px;
  font-weight: 600;
  letter-spacing: -0.3px;
}

.post-content-full {
  padding: 0 16px;
  font-size: 16px;
  color: var(--text-color);
  line-height: 1.6;
  letter-spacing: -0.2px;
}

.post-content-full p {
  margin-bottom: 16px;
}

.post-tags {
  padding: 0 16px 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.post-tag {
  color: var(--primary-color);
  font-size: 14px;
  font-weight: 500;
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
  font-size: 15px;
}

.post-action i {
  font-size: 18px;
}

.post-action:hover {
  color: var(--primary-color);
}

.post-action.active {
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
  display: flex;
  align-items: center;
  gap: 4px;
}

.comment-action i {
  font-size: 14px;
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