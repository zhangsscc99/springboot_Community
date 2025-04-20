<template>
  <div class="post-detail-view" :class="{ 'no-wait-cursor': isActionLoading }">
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
        <h3 class="comments-title">评论 ({{ comments.length || 0 }})</h3>
        
        <div class="comment-input">
          <textarea 
            placeholder="添加评论..." 
            v-model="newComment" 
            rows="3"
            :disabled="commentLoading"
          ></textarea>
          <button 
            class="comment-submit" 
            :disabled="!newComment.trim() || !isAuthenticated || commentLoading" 
            @click="submitComment"
          >
            <i v-if="commentLoading" class="fas fa-spinner fa-spin"></i>
            <span v-else>{{ isAuthenticated ? '发布' : '请先登录' }}</span>
          </button>
        </div>
        
        <div v-if="commentsLoading" class="comments-loading">
          <i class="fas fa-spinner fa-spin"></i> 加载评论中...
        </div>
        
        <div class="comments-list" v-else-if="comments && comments.length > 0">
          <div v-for="comment in comments" :key="comment.id" class="comment-item">
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
              <div class="comment-action" @click="likeComment(comment.id)" :class="{ 'active': comment.likedByCurrentUser }">
                <i class="fas fa-heart"></i> {{ comment.likes || 0 }}
              </div>
              <div class="comment-action" @click="replyToComment(comment)">
                <i class="fas fa-reply"></i> 回复
              </div>
              <div class="comment-action delete" v-if="canDeleteComment(comment)" @click="deleteComment(comment.id)">
                <i class="fas fa-trash-alt"></i> 删除
              </div>
            </div>
            
            <!-- 嵌套回复 -->
            <div v-if="comment.replies && comment.replies.length > 0" class="comment-replies">
              <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                <div class="comment-header">
                  <UserAvatar 
                    :src="reply.author.avatar" 
                    :username="reply.author.username"
                    :size="28"
                  />
                  <div class="comment-user-info">
                    <h4 class="comment-username">{{ reply.author.username }}</h4>
                    <span class="comment-time">{{ formatDate(reply.created_at) }}</span>
                  </div>
                </div>
                <div class="comment-content">
                  <span class="reply-to" v-if="reply.replyToUser">@{{ reply.replyToUser.username }} </span>
                  {{ reply.content }}
                </div>
              </div>
            </div>
            
            <!-- 回复输入框 -->
            <div v-if="replyingTo === comment.id" class="reply-input">
              <textarea 
                v-model="replyContent" 
                placeholder="回复评论..." 
                rows="2"
                :disabled="replyLoading"
              ></textarea>
              <div class="reply-actions">
                <button @click="cancelReply" class="reply-cancel">取消</button>
                <button 
                  @click="submitReply(comment.id)" 
                  class="reply-submit"
                  :disabled="!replyContent.trim() || replyLoading"
                >
                  <i v-if="replyLoading" class="fas fa-spinner fa-spin"></i>
                  <span v-else>回复</span>
                </button>
              </div>
            </div>
          </div>
        </div>
        
        <div v-else class="no-comments">
          还没有评论，来发表第一条吧
        </div>
        
        <div v-if="comments.length > 0 && hasMoreComments" class="load-more-comments">
          <button @click="loadMoreComments" class="load-more-btn" :disabled="loadingMoreComments">
            <i v-if="loadingMoreComments" class="fas fa-spinner fa-spin"></i>
            <span v-else>加载更多评论</span>
          </button>
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
import { formatDistanceToNow, format } from 'date-fns';
import { zhCN } from 'date-fns/locale';
import UserAvatar from '@/components/UserAvatar.vue';
import apiService from '@/services/apiService';
import axios from 'axios';

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
      debounceTimer: null,  // 防抖定时器
      
      // 评论相关
      commentLoading: false,
      commentsLoading: false,
      replyingTo: null,
      replyContent: '',
      replyLoading: false,
      commentPage: 0,
      commentSize: 10,
      hasMoreComments: false,
      loadingMoreComments: false
    };
  },
  computed: {
    ...mapState(['user']),
    ...mapGetters({
      post: 'currentPost',
      loading: 'isLoading',
      actionLoading: 'isActionLoading',
      error: 'error',
      isAuthenticated: 'isAuthenticated'
    }),
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
      'fetchComments',
      'deleteComment',
      'likeComment'
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
    
    // 获取评论
    async fetchPostComments() {
      this.commentsLoading = true;
      try {
        const response = await apiService.comments.getByPostId(this.postId);
        console.log('获取到的评论:', response.data);
        // 确保我们正确处理评论数据，无论是数组还是分页对象
        this.comments = response.data.content || response.data || [];
      } catch (error) {
        console.error('加载评论失败:', error);
      } finally {
        this.commentsLoading = false;
      }
    },
    
    // 提交评论
    async submitComment() {
      if (!this.newComment.trim()) {
        alert('请输入评论内容');
        return;
      }
      
      if (!this.isAuthenticated) {
        alert('请先登录');
        return;
      }
      
      this.commentLoading = true;
      try {
        console.log('准备提交评论');
        
        // 创建评论数据对象
        const commentData = {
          content: this.newComment.trim(),
          postId: this.postId  // 确保包含帖子ID
        };
        
        console.log('提交评论数据:', commentData);
        
        // 直接使用axios发送请求以排除apiService可能的问题
        const response = await axios.post(
          `/api/posts/${this.postId}/comments`, 
          commentData,
          { headers: { 'Content-Type': 'application/json' } }
        );
        
        console.log('评论提交响应:', response);
        
        if (response.data) {
          // 添加到评论列表
          this.comments.unshift(response.data);
          this.newComment = '';
          
          // 更新评论计数
          if (this.post) {
            this.post.comments = (this.post.comments || 0) + 1;
          }
        }
      } catch (error) {
        console.error('提交评论失败:', error.response || error);
        alert('评论提交失败: ' + (error.response?.data?.message || error.message || '未知错误'));
      } finally {
        this.commentLoading = false;
      }
    },
    
    // 回复评论相关方法
    async replyToComment(comment) {
      if (!this.isAuthenticated) {
        alert('请先登录再回复评论');
        return;
      }
      
      this.replyingTo = comment.id;
      this.replyContent = '';
    },
    
    cancelReply() {
      this.replyingTo = null;
      this.replyContent = '';
    },
    
    async submitReply(commentId) {
      if (!this.replyContent.trim() || !this.isAuthenticated) return;
      
      this.replyLoading = true;
      try {
        // 确保 commentId 存在且有效
        if (!commentId) {
          throw new Error('无效的评论ID');
        }
        
        // 创建回复数据
        const replyData = {
          content: this.replyContent,
          parentId: commentId,
          // 如果需要回复特定用户
          replyToUserId: this.currentUser?.id
        };
        
        // 调用API创建回复
        const response = await apiService.comments.createReply(commentId, replyData);
        const newReply = response.data;
        
        // 确保回复对象有效
        if (newReply) {
          // 找到要回复的评论
          const parentComment = this.comments.find(c => c.id === commentId);
          if (parentComment) {
            // 初始化回复数组（如果不存在）
            if (!parentComment.replies) {
              parentComment.replies = [];
            }
            // 添加新回复
            parentComment.replies.push(newReply);
          }
          
          // 清除回复状态
          this.replyingTo = null;
          this.replyContent = '';
          
          // 更新帖子评论计数
          if (this.post) {
            this.post.comments = (this.post.comments || 0) + 1;
          }
        }
      } catch (error) {
        console.error('提交回复失败:', error);
      } finally {
        this.replyLoading = false;
      }
    },
    
    // 加载更多评论
    async loadMoreComments() {
      this.commentPage++;
      this.loadingMoreComments = true;
      try {
        await this.fetchPostComments();
      } finally {
        this.loadingMoreComments = false;
      }
    },
    
    // 检查用户是否可以删除评论
    canDeleteComment(comment) {
      if (!this.isAuthenticated || !this.user) return false;
      
      // 当前用户是评论作者或帖子作者
      return (
        comment.author.id === this.user.id || 
        (this.post && this.post.author && this.post.author.id === this.user.id)
      );
    },
    
    // 删除评论
    async deleteComment(commentId) {
      if (!confirm('确定要删除这条评论吗？')) return;
      
      try {
        await this.$store.dispatch('deleteComment', commentId);
        
        // 从评论列表中移除
        this.comments = this.comments.filter(c => c.id !== commentId);
        
        // 更新帖子评论数
        if (this.post) {
          const currentComments = this.post.comments || 0;
          this.$store.commit('UPDATE_POST_FIELD', {
            postId: this.postId,
            field: 'comments',
            value: Math.max(0, currentComments - 1)
          });
        }
      } catch (error) {
        console.error('删除评论失败:', error);
      }
    },
    
    // 评论点赞
    async likeComment(commentId) {
      if (!this.isAuthenticated) {
        alert('请先登录后再点赞');
        return;
      }
      
      try {
        const comment = this.comments.find(c => c.id === commentId);
        if (!comment) return;
        
        if (comment.likedByCurrentUser) {
          await this.$store.dispatch('unlikeComment', commentId);
          comment.likedByCurrentUser = false;
          comment.likes = Math.max(0, (comment.likes || 0) - 1);
        } else {
          await this.$store.dispatch('likeComment', commentId);
          comment.likedByCurrentUser = true;
          comment.likes = (comment.likes || 0) + 1;
        }
      } catch (error) {
        console.error('评论点赞操作失败:', error);
      }
    },
    goBack() {
      this.$router.go(-1);
    },
    goHome() {
      this.$router.push({ name: 'home' });
    },
    // 加载评论
    async loadComments() {
      this.commentsLoading = true;
      try {
        const response = await apiService.comments.getByPostId(this.postId);
        this.comments = response.data.content || [];
      } catch (error) {
        console.error('加载评论失败:', error);
      } finally {
        this.commentsLoading = false;
      }
    }
  },
  async mounted() {
    // 加载帖子详情
    await this.fetchPost();
    
    // 加载评论
    await this.fetchPostComments();
    
    // 检查认证状态
    console.log('当前认证状态:', this.isAuthenticated);
    console.log('当前用户:', this.user);
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

/* 新增样式 - 当操作加载时禁用等待光标 */
.no-wait-cursor {
  cursor: default !important;
}
.no-wait-cursor * {
  cursor: default !important;
}
.no-wait-cursor .post-action {
  cursor: pointer !important;
}

/* 添加新的评论样式 */
.comments-loading {
  text-align: center;
  padding: 20px;
  color: var(--light-text-color);
}

.comment-replies {
  margin-left: 20px;
  margin-top: 10px;
  border-left: 2px solid var(--border-color);
  padding-left: 15px;
}

.reply-item {
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--border-color);
}

.reply-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.reply-input {
  margin-top: 12px;
  margin-left: 20px;
  border-left: 2px solid var(--primary-color);
  padding-left: 15px;
}

.reply-input textarea {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 10px;
  resize: none;
  font-size: 14px;
  margin-bottom: 8px;
}

.reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.reply-cancel {
  padding: 6px 12px;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 16px;
  cursor: pointer;
  font-size: 12px;
}

.reply-submit {
  padding: 6px 12px;
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-end));
  color: white;
  border: none;
  border-radius: 16px;
  cursor: pointer;
  font-size: 12px;
}

.reply-submit:disabled, .reply-cancel:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.reply-to {
  color: var(--primary-color);
  font-weight: 500;
}

.comment-action.active {
  color: var(--primary-color);
}

.comment-action.delete {
  color: #e74c3c;
}

.load-more-comments {
  text-align: center;
  margin-top: 20px;
}

.load-more-btn {
  padding: 8px 16px;
  background-color: transparent;
  border: 1px solid var(--primary-color);
  color: var(--primary-color);
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.load-more-btn:hover {
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-end));
  color: white;
}

.load-more-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style> 