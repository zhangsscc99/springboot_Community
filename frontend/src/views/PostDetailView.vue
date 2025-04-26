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
          :userId="post.author.id"
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
          <i class="fas fa-heart"></i>
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
          <i class="fas fa-star"></i>
          <span>{{ post.favorites || 0 }}</span>
        </div>
        <div class="post-action">
          <i class="fas fa-eye"></i>
          <span>{{ post.views || 0 }}</span>
        </div>
        <div v-if="isAuthor" class="post-action delete-action" @click="confirmDeletePost">
          <i class="fas fa-trash-alt"></i>
          <span>删除</span>
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
                :userId="comment.author.id"
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
                    :userId="reply.author.id"
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
                <div class="reply-actions">
                  <div class="comment-action" @click="replyToReply(comment.id, reply)" v-if="isAuthenticated">
                    <i class="fas fa-reply"></i> 回复
                  </div>
                  <div class="comment-action delete" v-if="canDeleteComment(reply)" @click="deleteComment(reply.id)">
                    <i class="fas fa-trash-alt"></i> 删除
                  </div>
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
      loadingMoreComments: false,
      isLoadingComments: false,
      commentError: null,
      isUrlCopied: false,
      replyingToUser: null
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
    },
    isAuthor() {
      return this.post && this.post.author && this.post.author.id === this.user.id;
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
        // 验证帖子ID
        if (!this.postId) {
          console.error('无效的帖子ID: 路由参数中没有找到ID');
          throw new Error('无效的帖子ID: 请检查URL');
        }

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
        this.comments = await this.fetchComments({ postId: this.postId });
        
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
      
      // 保存初始状态，以便操作失败时恢复
      const originalLikeStatus = this.isLiked;
      const originalLikeCount = this.post.likes || 0;
      
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
      } catch (error) {
        console.error('点赞操作失败:', error);
        // 操作失败时，恢复原始状态
        this.isLiked = originalLikeStatus;
        this.post.likes = originalLikeCount;
      } finally {
        // 无论成功失败，都要重置处理中状态
        this.isProcessingLike = false;
      }
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
      
      // 保存初始状态，以便操作失败时恢复
      const originalFavoriteStatus = this.isFavorited;
      const originalFavoriteCount = this.post.favorites || 0;
      
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
      } catch (error) {
        console.error('收藏操作失败:', error);
        // 操作失败时，恢复原始状态
        this.isFavorited = originalFavoriteStatus;
        this.post.favorites = originalFavoriteCount;
      } finally {
        // 无论成功失败，都要重置处理中状态
        this.isProcessingFavorite = false;
      }
    },
    
    // 滚动到评论区
    scrollToComments() {
      if (this.$refs.commentsSection) {
        this.$refs.commentsSection.scrollIntoView({ behavior: 'smooth' });
      }
    },
    
    // 获取评论
    async loadComments() {
      // 检查当前是否在帖子详情页面
      if (this.$route.name !== 'post-detail') {
        console.log('当前不在帖子详情页面，跳过评论加载');
        return;
      }
    
      // 确保有有效的帖子ID
      if (!this.post || !this.post.id) {
        console.log('帖子ID不存在或无效，无法加载评论');
        return;
      }
      
      // 防止重复加载
      if (this.isLoadingComments) {
        console.log('评论已在加载中，跳过重复请求');
        return;
      }
      
      this.isLoadingComments = true;
      
      try {
        console.log(`开始加载帖子 ${this.post.id} 的评论`);
        const response = await apiService.comments.getCommentsByPostId(this.post.id);
        this.comments = response.data;
        console.log(`成功加载到 ${this.comments.length} 条评论`);
      } catch (error) {
        console.error('加载评论失败:', error);
        if (error.response) {
          this.commentError = `加载评论失败: ${error.response.status} ${error.response.statusText}`;
        } else {
          this.commentError = '加载评论失败，请稍后再试';
        }
      } finally {
        this.isLoadingComments = false;
      }
    },
    
    // 提交评论
    async submitComment() {
      if (!this.newComment.trim() || !this.isAuthenticated) {
        return; // 如果内容为空或未登录，不执行
      }
      
      this.commentLoading = true;
      try {
        // 修改这一行 - 使用正确的方法
        await apiService.comments.create(this.postId, { content: this.newComment.trim() });
        this.newComment = ''; // 清空输入框
        // 重新加载评论
        await this.loadComments(); // 使用现有的获取评论方法
        console.log('评论已添加并刷新');
      } catch (error) {
        console.error('提交评论出错:', error);
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
      this.replyingToUser = comment.author;
    },
    
    // 回复评论的回复
    async replyToReply(commentId, reply) {
      if (!this.isAuthenticated) {
        alert('请先登录再回复');
        return;
      }
      
      // 设置正在回复的评论ID为原始评论
      this.replyingTo = commentId;
      // 清空回复内容
      this.replyContent = '';
      // 设置被回复用户为回复的作者
      this.replyingToUser = reply.author;
      
      // 在回复框中预填@用户名
      this.$nextTick(() => {
        this.replyContent = `@${reply.author.username} `;
        // 聚焦到回复输入框
        const replyTextarea = document.querySelector('.reply-input textarea');
        if (replyTextarea) {
          replyTextarea.focus();
        }
      });
    },
    
    cancelReply() {
      this.replyingTo = null;
      this.replyContent = '';
      this.replyingToUser = null;
    },
    
    async submitReply(commentId) {
      if (!this.replyContent.trim() || !this.isAuthenticated) return;
      
      this.replyLoading = true;
      try {
        // 确保 commentId 存在且有效
        if (!commentId) {
          throw new Error('无效的评论ID');
        }

        // 找到要回复的评论
        const parentComment = this.comments.find(c => c.id === commentId);
        if (!parentComment) {
          throw new Error('找不到要回复的评论');
        }
        
        // 创建回复数据
        const replyData = {
          content: this.replyContent,
          postId: this.postId,  // 确保包含帖子ID
          parentId: commentId,  // 父评论ID
          // 设置回复给谁（评论作者或指定的回复对象）
          replyToUserId: this.replyingToUser ? this.replyingToUser.id : parentComment.author.id
        };
        
        console.log('提交回复:', replyData);
        
        // 直接使用 apiService 发送请求
        const response = await apiService.comments.create(this.postId, replyData);
        const newReply = response.data;
        
        console.log('回复创建成功:', newReply);
        
        // 确保回复对象有效
        if (newReply) {
          // 初始化回复数组（如果不存在）
          if (!parentComment.replies) {
            parentComment.replies = [];
          }
          
          // 添加新回复到UI
          parentComment.replies.push({
            id: newReply.id,
            content: newReply.content,
            author: this.user,  // 使用当前用户作为作者
            created_at: new Date().toISOString(),
            replyToUser: this.replyingToUser || parentComment.author
          });
          
          // 清除回复状态
          this.replyingTo = null;
          this.replyContent = '';
          this.replyingToUser = null;
          
          // 更新帖子评论计数
          if (this.post) {
            this.post.comments = (this.post.comments || 0) + 1;
          }
          
          // 可选：滚动到新回复
          this.$nextTick(() => {
            const replyElements = document.querySelectorAll('.reply-item');
            if (replyElements.length > 0) {
              replyElements[replyElements.length - 1].scrollIntoView({ behavior: 'smooth' });
            }
          });
        }
      } catch (error) {
        console.error('提交回复失败:', error);
        alert('回复发送失败: ' + (error.message || '未知错误'));
      } finally {
        this.replyLoading = false;
      }
    },
    
    // 加载更多评论
    async loadMoreComments() {
      this.commentPage++;
      this.loadingMoreComments = true;
      try {
        await this.loadComments();
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
      // 使用Vue Router的历史导航
      this.$router.go(-1);
      
      // 如果上面的方法出现问题，可以改为返回首页
      // this.$router.push('/');
    },
    goHome() {
      this.$router.push({ name: 'Home' });
    },
    formatDate(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    },
    // 单独修改帖子加载完成的回调
    onPostLoaded() {
      // 帖子加载完成后再加载评论
      if (this.post && this.post.id) {
        this.loadComments();
      }
    },
    copyPostUrl() {
      // 构建包含说明文字和帖子信息的分享文本
      const postTitle = this.post ? this.post.title : '帖子';
      const authorName = this.post && this.post.author ? this.post.author.username : '匿名用户';
      const url = window.location.href;
      
      // 获取更多帖子信息
      let likesCount = this.post && this.post.likes ? this.post.likes : 0;
      let commentsCount = this.post && this.post.comments ? this.post.comments : 0;
      let views = this.post && this.post.views ? this.post.views : 0;
      
      // 格式化日期
      let publishDate = '';
      if (this.post && this.post.createdAt) {
        const date = new Date(this.post.createdAt);
        publishDate = date.toLocaleString('zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit'
        });
      }
      
      // 截取帖子内容前100个字符作为预览
      let contentPreview = '';
      if (this.post && this.post.content) {
        contentPreview = this.post.content.substring(0, 100);
        if (this.post.content.length > 100) {
          contentPreview += '...';
        }
      }
      
      // 格式化分享文本，美化排版
      const shareText = 
      `【锦书情感社区】${postTitle}\n` +
      `—————————————————\n` +
      `作者：${authorName}${publishDate ? ` | 发布于：${publishDate}` : ''}\n` +
      `点赞：${likesCount} | 评论：${commentsCount} | 浏览：${views}\n` +
      `—————————————————\n` +
      (contentPreview ? `${contentPreview}\n\n` : '') +
      `来自锦书旗下的情感社区，欢迎访问查看完整内容：\n${url}`;
      
      // 检查Clipboard API是否可用
      if (navigator.clipboard && navigator.clipboard.writeText) {
        // 现代浏览器 - 使用Clipboard API
        navigator.clipboard.writeText(shareText)
          .then(() => {
            this.isUrlCopied = true;
            // 2秒后重置状态
            setTimeout(() => {
              this.isUrlCopied = false;
            }, 2000);
          })
          .catch(err => {
            console.error('无法使用Clipboard API复制链接: ', err);
            // 使用备用方法
            this.fallbackCopyTextToClipboard(shareText);
          });
      } else {
        // 不支持Clipboard API的浏览器 - 直接使用备用方法
        console.log('Clipboard API不可用，使用备用复制方法');
        this.fallbackCopyTextToClipboard(shareText);
      }
    },
    
    fallbackCopyTextToClipboard(text) {
      let textArea = null;
      
      try {
        textArea = document.createElement("textarea");
        textArea.value = text;
        
        // 使元素不可见但仍可以选择
        textArea.style.position = "fixed";
        textArea.style.left = "-999999px";
        textArea.style.top = "-999999px";
        textArea.style.opacity = "0";
        
        document.body.appendChild(textArea);
        
        // 处理iOS设备
        if (navigator.userAgent.match(/ipad|ipod|iphone/i)) {
          // 创建一个选择范围并选择文本区域
          const range = document.createRange();
          range.selectNodeContents(textArea);
          const selection = window.getSelection();
          selection.removeAllRanges();
          selection.addRange(range);
          textArea.setSelectionRange(0, 999999);
        } else {
          // 其他设备
          textArea.select();
        }
        
        // 执行复制命令
        const successful = document.execCommand('copy');
        
        if (successful) {
          console.log('使用execCommand成功复制了分享文本');
          this.isUrlCopied = true;
          setTimeout(() => {
            this.isUrlCopied = false;
          }, 2000);
        } else {
          console.warn('execCommand复制命令执行但可能失败');
          alert('复制链接失败，请手动复制地址栏中的URL');
        }
      } catch (err) {
        console.error('回退复制方法失败', err);
        alert('复制链接失败，请手动复制地址栏中的URL');
      } finally {
        // 移除临时元素
        if (textArea && document.body.contains(textArea)) {
          document.body.removeChild(textArea);
        }
      }
    },
    async confirmDeletePost() {
      if (!confirm('确定要删除这个帖子吗？')) return;
      
      try {
        console.log(`正在尝试删除帖子 ID: ${this.postId}`);
        console.log(`当前用户 ID: ${this.user?.id}, 帖子作者 ID: ${this.post?.author?.id}`);
        
        // 添加前端额外验证
        if (this.user?.id !== this.post?.author?.id) {
          console.error('权限错误: 当前用户不是帖子作者');
          alert('您没有权限删除这个帖子');
          return;
        }
        
        await this.$store.dispatch('deletePost', this.postId);
        alert('帖子已成功删除');
        this.$router.push({ name: 'Home' });
      } catch (error) {
        console.error('删除帖子失败:', error);
        console.error('删除帖子详细信息:', {
          postId: this.postId,
          userId: this.user?.id,
          authorId: this.post?.author?.id,
          errorMessage: error?.message || '未知错误'
        });
        alert(`删除失败: ${error?.message || '服务器错误，请稍后再试'}`);
      }
    }
  },
  async mounted() {
    try {
      // 首先验证路由参数
      if (!this.$route.params.id) {
        console.warn('路由参数中缺少帖子ID，跳过数据加载');
        this.error = '无效的帖子ID：URL参数缺失';
        return;
      }

      // 验证ID格式
      const postId = this.$route.params.id;
      if (isNaN(Number(postId))) {
        console.warn(`无效的帖子ID格式: ${postId}`);
        this.error = '无效的帖子ID：格式错误';
        return;
      }

      // 加载帖子详情
      await this.fetchPost();
      
      // 只有当帖子成功加载后才加载评论
      if (this.post && this.post.id) {
        await this.loadComments();
      } else {
        console.log('帖子数据未加载，跳过评论加载');
      }
      
      // 检查认证状态
      console.log('当前认证状态:', this.isAuthenticated);
      console.log('当前用户:', this.user);
    } catch (error) {
      console.error('组件初始化出错:', error);
      this.error = '加载内容时出错';
    }
  },
  // 添加路由离开钩子，清理状态
  beforeRouteLeave(to, from, next) {
    // 清理本地组件状态
    this.comments = [];
    this.commentError = null;
    this.isLoadingComments = false;
    
    // 调用 store 的 resetComments 动作，重置全局状态
    this.$store.dispatch('resetComments');
    
    // 继续路由导航
    next();
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

.post-action.delete-action {
  color: #e74c3c;
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

/* 转载按钮样式修改 */
.post-action.share-action {
  position: relative;
}

.post-action.share-action:hover {
  color: var(--primary-color);
}

.post-action.share-action.active {
  color: #2ecc71; /* 绿色，表示复制成功 */
  font-weight: 500;
}

.post-action.share-action.active i {
  animation: bounceIn 0.5s;
}

/* 增加转载提示样式 */
.post-action.share-action:hover::before {
  content: "复制帖子详情和链接";
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  background-color: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 5px 10px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
  margin-bottom: 5px;
  pointer-events: none;
  opacity: 0;
  animation: fadeIn 0.3s forwards;
}

@keyframes fadeIn {
  to {
    opacity: 1;
  }
}

@keyframes bounceIn {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.3);
  }
  100% {
    transform: scale(1);
  }
}

/* 可能需要确保其他按钮样式保持一致 */
.post-action {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.post-action i {
  margin-right: 5px;
  font-size: 16px;
}

.post-action:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.post-action.active {
  color: var(--primary-color);
}

/* 添加与主页相同的动效样式 */
.post-action.active {
  color: var(--primary-color);
  font-weight: 500;
}

.post-action.processing {
  opacity: 0.8;
  pointer-events: none;
  transition: opacity 0.2s ease;
}

/* 添加点赞和收藏动效 */
.post-action i.fas.fa-heart,
.post-action i.fas.fa-star {
  transition: transform 0.2s cubic-bezier(0.18, 0.89, 0.32, 1.28);
}

.post-action:active i.fas {
  transform: scale(1.3);
}
</style> 