<template>
  <div class="search-view">
    <div class="search-header">
      <div class="search-input-container">
        <button class="back-button" @click="goBack">
          <i class="fas fa-arrow-left"></i>
        </button>
        <div class="search-box">
          <i class="fas fa-search search-icon"></i>
          <input 
            type="text" 
            class="search-input" 
            placeholder="搜索帖子或用户..." 
            v-model="searchQuery"
            @keyup.enter="performSearch"
            ref="searchInput"
            autocomplete="off"
          />
          <i 
            v-if="searchQuery" 
            class="fas fa-times clear-icon" 
            @click="clearSearch"
          ></i>
        </div>
        <button class="search-button" @click="performSearch">
          搜索
        </button>
      </div>
    </div>

    <div class="search-content">
      <!-- 搜索历史记录 -->
      <div v-if="!searching && !hasSearched" class="search-history">
        <div class="search-section-title">
          <span>搜索历史</span>
          <button @click="clearHistory" class="clear-history-btn">清除</button>
        </div>
        <div class="history-list" v-if="searchHistory.length > 0">
          <div 
            v-for="(item, index) in searchHistory" 
            :key="index" 
            class="history-item"
            @click="useHistoryItem(item)"
          >
            <i class="fas fa-history"></i>
            <span>{{ item }}</span>
            <i class="fas fa-times" @click.stop="removeHistoryItem(index)"></i>
          </div>
        </div>
        <div v-else class="empty-history">
          <p>暂无搜索记录</p>
        </div>
      </div>

      <!-- 搜索结果 -->
      <div v-if="hasSearched" class="search-results">
        <div class="search-section-title">
          <span>搜索结果: "{{ currentSearchTerm }}"</span>
        </div>
        
        <!-- 搜索结果分类标签 -->
        <div class="search-tabs">
          <div 
            class="search-tab" 
            :class="{ active: activeTab === 'posts' }"
            @click="activeTab = 'posts'"
          >
            帖子
          </div>
          <div 
            class="search-tab" 
            :class="{ active: activeTab === 'users' }"
            @click="activeTab = 'users'"
          >
            用户
          </div>
        </div>
        
        <!-- 加载中 -->
        <div v-if="searching" class="loading-indicator">
          <i class="fas fa-spinner fa-spin"></i> 搜索中...
        </div>
        
        <!-- 帖子搜索结果 -->
        <div v-else-if="activeTab === 'posts'" class="tab-content">
          <!-- 无结果 -->
          <div v-if="searchResults.length === 0" class="no-results">
            <i class="fas fa-search"></i>
            <p>未找到相关帖子</p>
            <p class="suggestion">请尝试其他关键词</p>
          </div>
          
          <!-- 搜索结果列表 -->
          <div v-else class="post-list">
            <div 
              v-for="post in searchResults" 
              :key="post.id" 
              class="post-card" 
              @click="goToPostDetail(post.id)"
            >
              <h3 class="post-title">{{ post.title }}</h3>
              <div class="post-header">
                <UserAvatar 
                  :src="post.author.avatar" 
                  :username="post.author.username"
                  :userId="post.author.id"
                />
                <div class="post-user-info">
                  <h4 class="post-username">{{ post.author.nickname || post.author.username }}</h4>
                  <span class="post-time">{{ formatDate(post.created_at) }}</span>
                </div>
              </div>
              <p class="post-content">{{ post.content }}</p>
              <div class="post-footer">
                <div class="post-actions">
                  <div class="post-action" :class="{ active: post.likedByCurrentUser }">
                    <i class="fas fa-heart"></i> {{ post.likes || 0 }}
                  </div>
                  <div class="post-action">
                    <i class="fas fa-comment-dots"></i> {{ post.comments || 0 }}
                  </div>
                  <div class="post-action" :class="{ active: post.favoritedByCurrentUser }">
                    <i class="fas fa-star"></i> {{ post.favorites || 0 }}
                  </div>
                </div>
                <div class="post-action">
                  <i class="fas fa-share-alt"></i> {{ post.views || 0 }}
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 用户搜索结果 -->
        <div v-else-if="activeTab === 'users'" class="tab-content">
          <!-- 无结果 -->
          <div v-if="userResults.length === 0" class="no-results">
            <i class="fas fa-search"></i>
            <p>未找到相关用户</p>
            <p class="suggestion">请尝试其他关键词</p>
          </div>
          
          <!-- 用户列表 -->
          <div v-else class="user-list">
            <div 
              v-for="user in userResults" 
              :key="user.id" 
              class="user-card" 
              @click="goToUserProfile(user.id)"
            >
              <UserAvatar 
                :src="user.avatar" 
                :username="user.username"
                :userId="user.id"
                class="user-avatar"
              />
              <div class="user-info">
                <div class="user-name">{{ user.nickname || user.username }}</div>
                <div class="user-id">锦书号: {{ user.username }}</div>
                <div class="user-bio">{{ user.bio || '这个人很懒，什么都没留下...' }}</div>
              </div>
              <button 
                class="follow-btn" 
                v-if="isAuthenticated && user.id !== currentUserId"
                :class="{ 'following': user.isFollowing }"
                @click.stop="toggleFollow(user)"
              >
                {{ user.isFollowing ? '已关注' : '关注' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import UserAvatar from '@/components/UserAvatar.vue';
import apiService from '@/services/apiService';
import { mapGetters } from 'vuex';

export default {
  name: 'SearchView',
  components: {
    UserAvatar
  },
  data() {
    return {
      searchQuery: '',
      searchResults: [],
      userResults: [],
      searchHistory: [],
      searching: false,
      hasSearched: false,
      currentSearchTerm: '',
      activeTab: 'posts',
      currentUserId: parseInt(localStorage.getItem('userId')) || null,
    };
  },
  computed: {
    ...mapGetters({
      isAuthenticated: 'isAuthenticated'
    })
  },
  methods: {
    goBack() {
      this.$router.go(-1);
    },
    clearSearch() {
      this.searchQuery = '';
      this.$refs.searchInput.focus();
    },
    performSearch() {
      if (!this.searchQuery.trim()) return;
      
      // 保存搜索历史
      this.saveToHistory(this.searchQuery);
      
      // 设置当前搜索词
      this.currentSearchTerm = this.searchQuery;
      
      // 开始搜索
      this.hasSearched = true;
      this.searching = true;
      
      // 同时搜索帖子和用户
      Promise.all([
        this.searchPosts(),
        this.searchUsers()
      ]).finally(() => {
        this.searching = false;
      });
    },
    async searchPosts() {
      try {
        const response = await apiService.posts.search(this.searchQuery);
        this.searchResults = response.data || [];
        
        // 处理搜索结果中可能缺少的字段
        this.searchResults = this.searchResults.map(post => {
          return {
            ...post,
            likedByCurrentUser: post.likedByCurrentUser || false,
            favoritedByCurrentUser: post.favoritedByCurrentUser || false,
            favorites: post.favorites || 0,
            views: post.views || 0
          };
        });
        
        console.log('帖子搜索结果:', this.searchResults);
      } catch (error) {
        console.error('搜索帖子失败:', error);
        this.searchResults = [];
      }
    },
    async searchUsers() {
      try {
        const response = await apiService.users.search(this.searchQuery);
        const users = response.data || [];
        
        // 如果用户已登录，检查是否关注了搜索结果中的用户
        if (this.isAuthenticated && users.length > 0) {
          // 为每个用户添加isFollowing属性，默认为false
          this.userResults = await Promise.all(users.map(async user => {
            try {
              // 当前用户不能关注自己
              if (user.id === this.currentUserId) {
                return { ...user, isFollowing: false };
              }
              
              // 检查是否已关注
              const followResponse = await apiService.users.checkFollowing(user.id);
              return { ...user, isFollowing: followResponse.data };
            } catch (error) {
              console.error(`检查关注状态失败，用户ID: ${user.id}`, error);
              return { ...user, isFollowing: false };
            }
          }));
        } else {
          // 未登录用户或没有搜索结果
          this.userResults = users.map(user => ({ ...user, isFollowing: false }));
        }
        
        console.log('用户搜索结果:', this.userResults);
      } catch (error) {
        console.error('搜索用户失败:', error);
        this.userResults = [];
      }
    },
    saveToHistory(query) {
      query = query.trim();
      if (!query) return;
      
      // 从 localStorage 获取历史记录
      const history = JSON.parse(localStorage.getItem('searchHistory') || '[]');
      
      // 如果已存在相同搜索词，先移除它
      const index = history.indexOf(query);
      if (index !== -1) {
        history.splice(index, 1);
      }
      
      // 添加到历史记录开头
      history.unshift(query);
      
      // 限制历史记录数量（最多保存10条）
      const limitedHistory = history.slice(0, 10);
      
      // 保存到 localStorage 和 data
      localStorage.setItem('searchHistory', JSON.stringify(limitedHistory));
      this.searchHistory = limitedHistory;
    },
    loadSearchHistory() {
      // 从 localStorage 加载历史记录
      this.searchHistory = JSON.parse(localStorage.getItem('searchHistory') || '[]');
    },
    clearHistory() {
      // 清空历史记录
      localStorage.removeItem('searchHistory');
      this.searchHistory = [];
    },
    useHistoryItem(query) {
      // 使用历史记录中的搜索词
      this.searchQuery = query;
      this.performSearch();
    },
    removeHistoryItem(index) {
      // 删除特定的历史记录项
      this.searchHistory.splice(index, 1);
      localStorage.setItem('searchHistory', JSON.stringify(this.searchHistory));
    },
    formatDate(dateString) {
      if (!dateString) return '';
      
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
    },
    goToUserProfile(userId) {
      this.$router.push({ name: 'profile', params: { id: userId } });
    },
    // 新增方法：切换关注/取消关注
    async toggleFollow(user) {
      if (!this.isAuthenticated) {
        this.$router.push('/login');
        return;
      }
      
      // 阻止事件冒泡，防止导航到用户个人页
      event.stopPropagation();
      
      try {
        // 乐观更新UI
        const isCurrentlyFollowing = user.isFollowing;
        
        // 立即在前端更新状态
        const userIndex = this.userResults.findIndex(u => u.id === user.id);
        if (userIndex !== -1) {
          this.userResults[userIndex].isFollowing = !isCurrentlyFollowing;
        }
        
        // 异步调用API
        if (isCurrentlyFollowing) {
          // 取消关注
          await apiService.users.unfollow(user.id);
          console.log(`已取消关注用户: ${user.username}`);
        } else {
          // 关注
          await apiService.users.follow(user.id);
          console.log(`已关注用户: ${user.username}`);
        }
      } catch (error) {
        console.error('关注/取消关注操作失败:', error);
        
        // 如果API调用失败，恢复原状态
        const userIndex = this.userResults.findIndex(u => u.id === user.id);
        if (userIndex !== -1) {
          this.userResults[userIndex].isFollowing = !this.userResults[userIndex].isFollowing;
        }
        
        // 显示错误提示（可选）
        alert('操作失败，请稍后再试');
      }
    },
  },
  mounted() {
    // 加载历史记录
    this.loadSearchHistory();
    
    // 自动聚焦搜索框
    this.$nextTick(() => {
      this.$refs.searchInput.focus();
    });
    
    // 如果有查询参数，则自动搜索
    if (this.$route.query.q) {
      this.searchQuery = this.$route.query.q;
      this.performSearch();
    }
  }
};
</script>

<style scoped>
.search-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--background-color);
}

.search-header {
  background-color: white;
  padding: 12px 16px;
  position: sticky;
  top: 0;
  z-index: 10;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.search-input-container {
  display: flex;
  align-items: center;
  gap: 10px;
}

.back-button {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: var(--text-color);
  padding: 8px;
}

.search-box {
  flex: 1;
  display: flex;
  align-items: center;
  background-color: var(--secondary-color);
  border-radius: 20px;
  padding: 8px 15px;
  position: relative;
}

.search-icon {
  color: var(--light-text-color);
  margin-right: 8px;
}

.clear-icon {
  color: var(--light-text-color);
  cursor: pointer;
  padding: 5px;
}

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 15px;
}

.search-button {
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-end));
  color: white;
  border: none;
  border-radius: 20px;
  padding: 8px 15px;
  font-size: 14px;
  cursor: pointer;
}

.search-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.search-section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  color: var(--text-color);
  font-weight: 500;
}

.clear-history-btn {
  background: none;
  border: none;
  color: var(--light-text-color);
  font-size: 14px;
  cursor: pointer;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-item {
  display: flex;
  align-items: center;
  padding: 10px;
  background-color: white;
  border-radius: 8px;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.history-item i {
  color: var(--light-text-color);
  margin-right: 10px;
}

.history-item span {
  flex: 1;
  color: var(--text-color);
}

.history-item .fa-times {
  padding: 5px;
}

.empty-history {
  text-align: center;
  padding: 30px 0;
  color: var(--light-text-color);
}

.search-results {
  margin-top: 5px;
}

/* 搜索结果分类标签 */
.search-tabs {
  display: flex;
  margin-bottom: 15px;
  background-color: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.search-tab {
  flex: 1;
  text-align: center;
  padding: 12px;
  color: var(--light-text-color);
  cursor: pointer;
  position: relative;
  font-weight: 500;
}

.search-tab.active {
  color: var(--primary-color);
}

.search-tab.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-end));
}

.tab-content {
  padding: 5px 0;
}

.loading-indicator {
  text-align: center;
  padding: 30px 0;
  color: var(--light-text-color);
}

.no-results {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 50px 0;
  color: var(--light-text-color);
}

.no-results i {
  font-size: 32px;
  margin-bottom: 15px;
}

.suggestion {
  font-size: 14px;
  margin-top: 8px;
}

/* 帖子搜索结果样式 */
.post-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-card {
  background-color: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.post-title {
  padding: 16px 16px 12px;
  font-size: 18px;
  font-weight: 600;
  margin: 0;
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
}

.post-action.active {
  color: var(--primary-color);
}

/* 用户搜索结果样式 */
.user-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.user-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
}

.user-avatar {
  margin-right: 15px;
}

.user-info {
  flex: 1;
}

.user-name {
  font-weight: 600;
  font-size: 16px;
  margin-bottom: 2px;
}

.user-id {
  font-size: 12px;
  color: var(--light-text-color);
  margin-bottom: 6px;
}

.user-bio {
  font-size: 13px;
  color: var(--light-text-color);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.follow-btn {
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-end));
  color: white;
  border: none;
  border-radius: 20px;
  padding: 6px 15px;
  font-size: 13px;
  cursor: pointer;
}

.follow-btn.following {
  background-image: none;
  background-color: #f0f0f0;
  color: var(--text-color);
  border: 1px solid var(--border-color);
}

.user-card .follow-btn {
  flex-shrink: 0;
}
</style> 
