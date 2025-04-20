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
        <div class="post-action" 
          :class="{ 
            'active': isLiked, 
            'processing': isProcessingLike 
          }" 
          @click="handleLike">
          <i class="fas" :class="isProcessingLike ? 'fa-spinner fa-spin' : 'fa-heart'"></i>
          <span>{{ post.likes || 0 }}</span>
        </div>
        <div class="post-action" @click="scrollToComments">
          <i class="fas fa-comment"></i>
          <span>{{ post.comments || 0 }}</span>
        </div>
        <div class="post-action" 
          :class="{ 
            'active': isFavorited, 
            'processing': isProcessingFavorite 
          }" 
          @click="handleFavorite">
          <i class="fas" :class="isProcessingFavorite ? 'fa-spinner fa-spin' : 'fa-star'"></i>
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
      comments: [],
      isProcessingLike: false,  // 点赞操作进行中标志
      isProcessingFavorite: false,  // 收藏操作进行中标志
      debounceTimer: null  // 防抖定时器
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
        // 使用store中优化过的fetchPostById方法（带缓存）
        const response = await this.fetchPostById(this.postId);
        
        // 如果没有返回数据，可能是获取失败
        if (!response) {
          throw new Error('帖子不存在或已被删除');
        }
        
        this.post = response;
        
        // 检查帖子是否已被当前用户点赞和收藏
        if (this.isAuthenticated) {
          this.isLiked = this.post.likedByCurrentUser || false;
          this.isFavorited = this.post.favoritedByCurrentUser || false;
        }
        
        // 获取评论 - 可以考虑也为评论添加缓存机制
        this.comments = await this.fetchComments(this.postId);
        
        this.error = null;
      } catch (error) {
        console.error('获取帖子详情失败:', error);
        this.error = error.message || '获取帖子失败，请稍后再试';
      } finally {
        this.loading = false;
      }
    },
    // 处理点赞
    async handleLike() {
      // 检查用户登录状态
      if (!this.isAuthenticated) {
        this.$router.push('/login?redirect=' + this.$route.fullPath);
        return;
      }
      
      // 防重复操作 - 如果正在处理点赞请求，则忽略此次点击
      if (this.isProcessingLike) {
        console.log('正在处理点赞操作，请稍后再试');
        return;
      }
      
      // 清除之前的防抖定时器
      if (this.debounceTimer) {
        clearTimeout(this.debounceTimer);
      }
      
      // 保存初始状态，以便操作失败时恢复
      const originalLikeStatus = this.isLiked;
      const originalLikeCount = this.post.likes || 0;
      
      // 设置防抖定时器
      this.debounceTimer = setTimeout(async () => {
        try {
          // 设置处理中状态，防止重复点击
          this.isProcessingLike = true;
          
          // 立即乐观更新界面状态（不等待API响应）
          this.isLiked = !originalLikeStatus;
          this.post.likes = this.isLiked 
            ? (originalLikeCount + 1) 
            : Math.max(0, originalLikeCount - 1);
          
          // 异步发送API请求
          if (this.isLiked) {
            // 点赞 - Vuex已处理缓存更新
            await this.likePost(this.postId);
          } else {
            // 取消点赞 - Vuex已处理缓存更新
            await this.unlikePost(this.postId);
          }
          
          // 不再需要重新获取帖子详情
          // 缓存和状态已在Vuex中更新
          
        } catch (error) {
          console.error('点赞操作失败:', error);
          // 操作失败时，恢复原始状态
          this.isLiked = originalLikeStatus;
          this.post.likes = originalLikeCount;
        } finally {
          // 无论成功失败，都要重置处理中状态
          this.isProcessingLike = false;
        }
      }, 300); // 300毫秒的防抖延迟
    },
    
    // 处理收藏
    async handleFavorite() {
      // 检查用户登录状态
      if (!this.isAuthenticated) {
        this.$router.push('/login?redirect=' + this.$route.fullPath);
        return;
      }
      
      // 防重复操作 - 如果正在处理收藏请求，则忽略此次点击
      if (this.isProcessingFavorite) {
        console.log('正在处理收藏操作，请稍后再试');
        return;
      }
      
      // 清除之前的防抖定时器
      if (this.debounceTimer) {
        clearTimeout(this.debounceTimer);
      }
      
      // 保存初始状态，以便操作失败时恢复
      const originalFavoriteStatus = this.isFavorited;
      const originalFavoriteCount = this.post.favorites || 0;
      
      // 设置防抖定时器
      this.debounceTimer = setTimeout(async () => {
        try {
          // 设置处理中状态，防止重复点击
          this.isProcessingFavorite = true;
          
          // 立即乐观更新界面状态（不等待API响应）
          this.isFavorited = !originalFavoriteStatus;
          this.post.favorites = this.isFavorited 
            ? (originalFavoriteCount + 1) 
            : Math.max(0, originalFavoriteCount - 1);
          
          // 异步发送API请求
          if (this.isFavorited) {
            // 收藏
            await this.favoritePost(this.postId);
          } else {
            // 取消收藏
            await this.unfavoritePost(this.postId);
          }
          
          // 成功操作后，不再重新获取整个帖子详情
          // 注意：我们移除了fetchPost()调用，避免重新加载整个页面
          
        } catch (error) {
          console.error('收藏操作失败:', error);
          // 操作失败时，恢复原始状态
          this.isFavorited = originalFavoriteStatus;
          this.post.favorites = originalFavoriteCount;
        } finally {
          // 无论成功失败，都要重置处理中状态
          this.isProcessingFavorite = false;
        }
      }, 300); // 300毫秒的防抖延迟
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
    // 在组件挂载前就开始获取数据，提高感知速度
    this.fetchPost();
    
    // 预加载可能需要的数据，提高切换到其它页面的速度
    setTimeout(() => {
      try {
        // 这里可以预加载热门帖子、推荐帖子等
        // 在后台进行，不会阻塞主要内容加载
      } catch (error) {
        console.error('预加载数据失败:', error);
      }
    }, 1000); // 延迟1秒预加载，优先加载当前页面
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

.post-action.processing {
  opacity: 0.7;
  cursor: wait;
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