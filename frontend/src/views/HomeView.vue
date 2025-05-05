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

    <div v-if="loading && !hasAnyTabPosts" class="loading-indicator">
      <i class="fas fa-spinner fa-spin"></i> 加载中...
    </div>

    <div v-else-if="error" class="error-message">
      <i class="fas fa-exclamation-circle"></i> {{ error }}
      </div>
    
    <div v-else class="post-list">
      <div v-if="loading && currentTabPosts.length === 0" class="tab-loading">
        <i class="fas fa-spinner fa-spin"></i> 加载{{ activeTab }}内容...
    </div>

      <div v-for="post in currentTabPosts" :key="post.id" class="post-card" @click="goToPostDetail(post.id)">
        <h3 class="post-title">{{ post.title }}</h3>
        <div class="post-header">
          <UserAvatar 
            :src="post.author.avatar" 
            :username="post.author.username"
            :userId="post.author.id"
            class="user-avatar-in-post"
          />
          <div class="post-user-info">
            <h4 class="post-username">{{ post.author.username }}</h4>
            <span class="post-bio">
              <i class="fas fa-info-circle bio-icon"></i> 
              {{ getUserBio(post.author) }}
            </span>
          </div>
        </div>
        <p class="post-content">{{ post.content }}</p>
        
        <!-- 添加发布时间显示 -->
        <div class="post-time-container">
          <span class="post-time">{{ formatDate(post.createdAt) }}</span>
        </div>
        
        <div class="post-tags" v-if="post.tags">
          <span v-for="(tag, index) in post.tags" :key="index" class="post-tag">
            #{{ tag }}
          </span>
        </div>
        <div class="post-footer">
          <div class="post-actions">
            <div class="post-action" 
                 :class="{ active: post.likedByCurrentUser, 'processing': isProcessingLike[post.id] }" 
                 @click.stop="handleLike(post)"
            >
              <i class="fas fa-heart"></i> {{ post.likes || 0 }}
            </div>
            <div class="post-action" 
                 :class="{ active: post.favoritedByCurrentUser, 'processing': isProcessingFavorite[post.id] }" 
                 @click.stop="handleFavorite(post)"
            >
              <i class="fas fa-star"></i> {{ post.favorites || 0 }}
            </div>
          </div>
          <div class="post-info">
            <i class="fas fa-eye"></i> {{ post.views || 0 }}
          </div>
        </div>
      </div>
      
      <div v-if="isPreloading && showPreloader" class="preloading-indicator">
        <i class="fas fa-spinner fa-spin"></i> 正在加载更多内容...
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters, mapState, mapActions } from 'vuex';
import UserAvatar from '@/components/UserAvatar.vue';
import axios from 'axios';

export default {
  name: 'HomeView',
  components: {
    UserAvatar
  },
  data() {
    return {
      tabs: ['关注', '推荐', '热榜', '故事', '情感知识'],
      isPreloading: false,
      preloadTimeout: null,
      currentPreloadTab: null,
      tabsPreloaded: {
        '关注': false,
        '推荐': false,
        '热榜': false,
        '故事': false,
        '情感知识': false
      },
      requestingTab: null,
      isProcessingLike: {},   // 存储每个帖子的点赞处理状态
      isProcessingFavorite: {} // 存储每个帖子的收藏处理状态
    };
  },
  computed: {
    ...mapState({
      tabPosts: state => state.tabPosts
    }),
    ...mapGetters({
      posts: 'allPosts',
      loading: 'isLoading',
      error: 'error',
      activeTab: 'activeTab',
      currentTabPosts: 'currentTabPosts',
      isTabCacheExpired: 'isTabCacheExpired',
      isAuthenticated: 'isAuthenticated'
    }),
    hasAnyTabPosts() {
      return Object.values(this.tabPosts).some(posts => posts && posts.length > 0);
    },
    showPreloader() {
      return this.currentPreloadTab === this.activeTab;
    }
  },
  methods: {
    ...mapActions([
      'fetchPostsByTab', 
      'likePost', 
      'unlikePost', 
      'favoritePost', 
      'unfavoritePost'
    ]),
    async switchTab(tab) {
      // 取消所有进行中的加载和预加载
      this.cancelPreloading();
      
      // 标记当前正在请求的标签
      this.requestingTab = tab;
      
      try {
        // 设置激活标签（这不会触发数据加载）
        this.$store.commit('SET_ACTIVE_TAB', tab);
        
        // 如果缓存过期或没有数据，则加载数据
        if (this.isTabCacheExpired(tab) || !this.tabPosts[tab] || this.tabPosts[tab].length === 0) {
          await this.$store.dispatch('fetchPostsByTab', tab, { cancelToken: tab });
        }
        
        // 仅当当前标签仍是请求开始时的标签时，才开始预加载
        if (this.requestingTab === tab && this.activeTab === tab) {
          this.preloadOtherTabs(tab);
        }
      } catch (error) {
        if (!axios.isCancel(error)) {
          console.error(`加载标签 ${tab} 失败:`, error);
        }
      } finally {
        // 请求结束后清除标记
        if (this.requestingTab === tab) {
          this.requestingTab = null;
        }
      }
    },
    cancelPreloading() {
      if (this.preloadTimeout) {
        clearTimeout(this.preloadTimeout);
        this.preloadTimeout = null;
      }
      this.isPreloading = false;
      this.currentPreloadTab = null;
    },
    async preloadOtherTabs(currentTab) {
      this.cancelPreloading();
      
      this.isPreloading = true;
      this.currentPreloadTab = currentTab;
      
      this.preloadTimeout = setTimeout(async () => {
        try {
          if (this.activeTab !== currentTab) {
            this.cancelPreloading();
            return;
          }
          
          for (const tab of this.tabs) {
            if (this.activeTab !== currentTab) {
              this.cancelPreloading();
              return;
            }
            
            if (tab === currentTab || this.tabsPreloaded[tab]) continue;
          
            if (this.isTabCacheExpired(tab) || !this.tabPosts[tab] || this.tabPosts[tab].length === 0) {
              console.log(`预加载 ${tab} 标签页数据...`);
              
              await this.preloadTabData(tab);
              this.tabsPreloaded[tab] = true;
              
              await new Promise(resolve => setTimeout(resolve, 300));
              
              if (this.activeTab !== currentTab) {
                this.cancelPreloading();
                return;
              }
            }
          }
        } catch (error) {
          console.error('预加载标签页失败:', error);
        } finally {
          if (this.currentPreloadTab === currentTab) {
            this.isPreloading = false;
            this.currentPreloadTab = null;
          }
        }
      }, 800);
    },
    async preloadTabData(tab) {
      try {
        const response = await this.$store.dispatch('fetchTabDataWithoutActivating', tab);
        return response;
      } catch (error) {
        console.error(`预加载标签 ${tab} 失败:`, error);
        return null;
      }
    },
    formatDate(dateString) {
      if (!dateString) return '未知时间';
      
      try {
        const date = new Date(dateString);
        // 检查日期是否有效
        if (isNaN(date.getTime())) {
          return '未知时间';
        }
        
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
          const year = date.getFullYear();
          const month = date.getMonth() + 1;
          const day = date.getDate();
          return `${year}-${month < 10 ? '0' + month : month}-${day < 10 ? '0' + day : day}`;
        }
      } catch (error) {
        console.error('日期格式化错误:', error, '原始日期字符串:', dateString);
        return '未知时间';
      }
    },
    goToPostDetail(postId) {
      this.$router.push({ name: 'post-detail', params: { id: postId } });
    },
    // 处理点赞
    async handleLike(post) {
      // 检查用户登录状态
      if (!this.isAuthenticated) {
        this.$router.push('/login?redirect=' + this.$route.fullPath);
        return;
      }
      
      // 防重复操作 - 如果正在处理请求，则忽略此次点击
      if (this.isProcessingLike[post.id]) {
        console.log('正在处理点赞操作，请稍后再试');
        return;
      }
      
      // 保存初始状态，以便操作失败时恢复
      const originalLikeStatus = post.likedByCurrentUser;
      
      try {
        // 设置处理中状态，防止重复点击 - 使用Vue兼容的赋值方式
        this.isProcessingLike = { ...this.isProcessingLike, [post.id]: true };
        
        // 更新UI状态以提供即时反馈，但不更新数据
        post.likedByCurrentUser = !originalLikeStatus;
        
        // 异步发送API请求 - 让Vuex处理数据更新
        if (post.likedByCurrentUser) {
          // 点赞
          await this.likePost(post.id);
        } else {
          // 取消点赞
          await this.unlikePost(post.id);
        }
      } catch (error) {
        console.error('点赞操作失败:', error);
        // 操作失败时，恢复原始状态
        post.likedByCurrentUser = originalLikeStatus;
      } finally {
        // 无论成功失败，都要重置处理中状态 - 使用Vue兼容的赋值方式
        this.isProcessingLike = { ...this.isProcessingLike, [post.id]: false };
      }
    },
    
    // 处理收藏
    async handleFavorite(post) {
      // 检查用户登录状态
      if (!this.isAuthenticated) {
        this.$router.push('/login?redirect=' + this.$route.fullPath);
        return;
      }
      
      // 防重复操作 - 如果正在处理请求，则忽略此次点击
      if (this.isProcessingFavorite[post.id]) {
        console.log('正在处理收藏操作，请稍后再试');
        return;
      }
      
      // 保存初始状态，以便操作失败时恢复
      const originalFavoriteStatus = post.favoritedByCurrentUser;
      
      try {
        // 设置处理中状态，防止重复点击 - 使用Vue兼容的赋值方式
        this.isProcessingFavorite = { ...this.isProcessingFavorite, [post.id]: true };
        
        // 更新UI状态以提供即时反馈，但不更新数据
        post.favoritedByCurrentUser = !originalFavoriteStatus;
        
        // 异步发送API请求 - 让Vuex处理数据更新
        if (post.favoritedByCurrentUser) {
          // 收藏
          await this.favoritePost(post.id);
        } else {
          // 取消收藏
          await this.unfavoritePost(post.id);
        }
      } catch (error) {
        console.error('收藏操作失败:', error);
        // 操作失败时，恢复原始状态
        post.favoritedByCurrentUser = originalFavoriteStatus;
      } finally {
        // 无论成功失败，都要重置处理中状态 - 使用Vue兼容的赋值方式
        this.isProcessingFavorite = { ...this.isProcessingFavorite, [post.id]: false };
      }
    },
    getUserBio(author) {
      // 如果作者不存在
      if (!author) {
        return '这个人很懒，还没有写简介';
      }

      // 尝试最简单的方法获取bio
      try {
        // 首先直接尝试从JSON格式获取
        const rawAuthor = JSON.parse(JSON.stringify(author));
        if (rawAuthor && rawAuthor.bio) {
          return rawAuthor.bio;
        }
        
        // 尝试从原始对象获取
        if (author.bio) {
          return author.bio;
        }
        
        // 检查其他可能的字段
        const possibleFields = ['introduction', 'description', 'about', 'personalIntro'];
        for (const field of possibleFields) {
          if (author[field]) {
            return author[field];
          }
        }
      } catch (e) {
        console.error('获取用户简介时出错:', e);
      }
      
      // 最终后备方案
      return '这个人很懒，还没有写简介';
    },
    
    // 保留getNestedValue方法，作为工具方法
    getNestedValue(obj, path) {
      try {
        return path.split('.').reduce((prev, curr) => {
          return prev && prev[curr] ? prev[curr] : null;
        }, obj);
      } catch (e) {
        return null;
      }
    },
    debugFirstPostAuthor() {
      if (!this.currentTabPosts || this.currentTabPosts.length === 0) {
        console.log('[Debug] 没有帖子可分析');
        return;
      }
      
      const post = this.currentTabPosts[0];
      const author = post.author;
      
      console.log('===== 帖子作者数据分析开始 =====');
      console.log(`帖子ID: ${post.id}`);
      console.log(`帖子标题: ${post.title}`);
      
      if (!author) {
        console.log('警告: 帖子没有作者信息!');
        return;
      }
      
      console.log(`作者ID: ${author.id}`);
      console.log(`作者名称: ${author.username}`);
      
      // 分析所有顶级字段
      console.log('作者对象顶级字段:');
      for (const key in author) {
        const value = author[key];
        const type = typeof value;
        const preview = type === 'object' 
          ? (value ? `对象/数组，含 ${Object.keys(value).length} 个属性` : 'null') 
          : (type === 'string' ? `"${value.length > 30 ? value.substring(0, 30) + '...' : value}"` : value);
        
        console.log(`  ${key}: ${type} = ${preview}`);
      }
      
      // 搜索所有可能的简介字段
      console.log('查找可能的简介字段:');
      const bioKeywords = ['bio', 'introduction', 'description', 'about', 'intro', 'profile', 'summary', 'info'];
      
      // 递归查找潜在的简介字段
      this.findPotentialBioFields(author, '', bioKeywords);
      
      console.log('===== 帖子作者数据分析结束 =====');
    },
    findPotentialBioFields(obj, prefix, keywords, depth = 0, maxDepth = 3) {
      if (!obj || typeof obj !== 'object' || depth > maxDepth) return;
      
      for (const key in obj) {
        const value = obj[key];
        const path = prefix ? `${prefix}.${key}` : key;
        
        // 检查当前字段是否可能是简介
        if (typeof value === 'string' && value.trim().length > 0) {
          // 检查键名是否包含简介相关关键词
          const isRelevant = keywords.some(keyword => 
            key.toLowerCase().includes(keyword.toLowerCase())
          );
          
          if (isRelevant) {
            console.log(`  找到潜在简介字段 "${path}": "${value.length > 50 ? value.substring(0, 50) + '...' : value}"`);
          }
        }
        
        // 递归检查子对象
        if (value && typeof value === 'object') {
          this.findPotentialBioFields(value, path, keywords, depth + 1, maxDepth);
        }
      }
    },
  },
  beforeUnmount() {
    this.cancelPreloading();
    // 取消所有进行中的请求
    this.tabs.forEach(tab => {
      this.$store.commit('CANCEL_TAB_REQUEST', tab);
    });
  },
  async created() {
    const currentTab = this.activeTab;
    await this.switchTab(currentTab);
    
    // 保留基础的作者信息调试，但移除可能导致错误的代码
    this.debugFirstPostAuthor();
    
    // 移除额外的验证代码
  },
  // 添加activated钩子，处理从其他页面返回时的状态重置
  activated() {
    // 确保返回主页时不会显示来自详情页的错误
    this.$store.commit('SET_ERROR', null);
    
    // 检查当前激活的标签是否有数据，如果没有则加载
    if (!this.tabPosts[this.activeTab] || this.tabPosts[this.activeTab].length === 0) {
      this.switchTab(this.activeTab);
    }

    console.log('HomeView 组件被激活');
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
  align-items: flex-start;
  gap: 10px;
}

.user-avatar-in-post {
  flex-shrink: 0;
}

.post-user-info {
  flex: 1;
  overflow: hidden;
  padding-top: 3px;
}

.post-username {
  font-weight: 500;
  font-size: 13px;
  margin: 0;
}

.post-bio {
  font-size: 12px;
  color: var(--light-text-color);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
  opacity: 0.9;
  padding-top: 2px;
  min-height: 14px; /* 确保即使没有内容也会占据空间 */
  display: block;
  margin-top: 2px;
  font-style: italic;
  letter-spacing: 0.2px;
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

.post-time-container {
  padding: 0 16px 12px;
  text-align: left;
}

.post-time {
  font-size: 12px;
  color: #8a9aa4;
  display: inline-block;
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
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
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
  border-radius: 18px;
  padding: 5px 10px;
  transition: all 0.2s ease;
}

.post-action i {
  font-size: 16px;
}

.post-action:hover {
  color: var(--primary-color);
  background-color: rgba(0, 0, 0, 0.04);
}

.post-info {
  font-size: 14px;
  color: var(--light-text-color);
  display: flex;
  align-items: center;
  gap: 4px;
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

.tab-loading {
  text-align: center;
  padding: 20px;
  color: var(--light-text-color);
}

.preloading-indicator {
  text-align: center;
  padding: 15px;
  color: var(--light-text-color);
  font-size: 14px;
  opacity: 0.7;
}

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

.post-card {
  animation: fade-in 0.3s ease-in-out;
  background-color: white;
  margin-bottom: 12px;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

@keyframes fade-in {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.bio-icon {
  font-size: 10px;
  margin-right: 4px;
  color: var(--light-text-color);
  opacity: 0.8;
}

/* 移除调试按钮样式 */
.debug-button {
  display: none; /* 先隐藏，之后可以完全删除 */
}

/* 在小屏幕上保持良好的布局 */
@media (max-width: 480px) {
  .post-footer {
    flex-direction: row;
    justify-content: space-between;
  }
  
  .post-actions {
    gap: 10px;
  }
  
  .post-action {
    padding: 4px 8px;
  }
}
</style> 