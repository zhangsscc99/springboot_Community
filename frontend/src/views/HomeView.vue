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
      
      <div v-if="isPreloading && showPreloader" class="preloading-indicator">
        <i class="fas fa-spinner fa-spin"></i> 正在加载更多内容...
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters, mapState } from 'vuex';
import UserAvatar from '@/components/UserAvatar.vue';

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
      }
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
      isTabCacheExpired: 'isTabCacheExpired'
    }),
    hasAnyTabPosts() {
      return Object.values(this.tabPosts).some(posts => posts && posts.length > 0);
    },
    showPreloader() {
      return this.currentPreloadTab === this.activeTab;
    }
  },
  methods: {
    async switchTab(tab) {
      this.cancelPreloading();
      
      this.$store.dispatch('setActiveTab', tab);
    
      if (this.isTabCacheExpired(tab) || !this.tabPosts[tab] || this.tabPosts[tab].length === 0) {
        await this.$store.dispatch('fetchPostsByTab', tab);
      }
      
      this.preloadOtherTabs(tab);
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
  beforeUnmount() {
    this.cancelPreloading();
  },
  async created() {
    const currentTab = this.activeTab;
    await this.switchTab(currentTab);
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
}

.post-card {
  animation: fade-in 0.3s ease-in-out;
}

@keyframes fade-in {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style> 