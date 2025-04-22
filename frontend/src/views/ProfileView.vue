<template>
  <div class="profile-view">
    <div class="profile-header">
      <button class="back-button" @click="goBack">
        <i class="fas fa-arrow-left"></i>
      </button>
      <div class="profile-header-actions">
        <button class="action-button">
          <i class="fas fa-share-alt"></i>
        </button>
        <button class="action-button">
          <i class="fas fa-ellipsis-h"></i>
        </button>
      </div>
    </div>
    
    <div v-if="loading" class="loading-indicator">
      <i class="fas fa-spinner fa-spin"></i> 加载中...
    </div>
    
    <template v-else>
      <div class="profile-info">
        <UserAvatar 
          :src="profileAvatar || 'https://via.placeholder.com/100'" 
          :username="profileName"
          class="profile-avatar-component"
        />
        <h1 class="profile-name">{{ profileName }}</h1>
        <div class="profile-userid">ID: {{ profileId }}</div>
        
        <div class="profile-stats">
          <div class="stat-item" @click="openFollowingList">
            <div class="stat-value">{{ followingCount }}</div>
            <div class="stat-label">关注</div>
          </div>
          <div class="stat-item" @click="openFollowersList">
            <div class="stat-value">{{ followerCount }}</div>
            <div class="stat-label">粉丝</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ likesCount }}</div>
            <div class="stat-label">获赞</div>
          </div>
        </div>
        
        <p class="profile-bio">{{ formattedProfileBio }}</p>
        
        <div class="profile-actions">
          <button 
            v-if="!isCurrentUser && isAuthenticated" 
            class="follow-btn" 
            :class="{ 'following': isFollowing }"
            @click="toggleFollow"
            :disabled="followLoading"
          >
            <i v-if="followLoading" class="fas fa-spinner fa-spin"></i>
            <i v-else-if="isFollowing" class="fas fa-check"></i>
            <i v-else class="fas fa-plus"></i>
            {{ isFollowing ? '已关注' : '关注' }}
          </button>
          <button class="edit-profile-btn" v-if="isCurrentUser" @click="openEditModal">
            <i class="fas fa-pencil-alt"></i> 编辑资料
          </button>
          <button class="message-btn" v-if="!isCurrentUser && isAuthenticated" @click="goToMessages">
            <i class="fas fa-comment"></i> 私信
          </button>
          <button class="logout-btn" v-if="isCurrentUser" @click="logout">
            <i class="fas fa-sign-out-alt"></i> 退出登录
          </button>
        </div>
      </div>
      
      <div class="profile-tabs">
        <div 
          class="profile-tab" 
          :class="{ active: activeTab === 'posts' }"
          @click="setActiveTab('posts')"
        >
          帖子
        </div>
        <div 
          v-if="isCurrentUser"
          class="profile-tab" 
          :class="{ active: activeTab === 'likes' }"
          @click="setActiveTab('likes')"
        >
          喜欢
        </div>
        <div 
          v-if="isCurrentUser"
          class="profile-tab" 
          :class="{ active: activeTab === 'favorites' }"
          @click="setActiveTab('favorites')"
        >
          收藏
        </div>
      </div>
      
      <div class="profile-content">
        <div v-if="activeTab === 'posts'" class="posts-list">
          <div v-if="postsLoading" class="loading-indicator">
            <i class="fas fa-spinner fa-spin"></i> 加载中...
          </div>
          <div v-else-if="userPosts.length > 0" class="post-list">
            <div v-for="post in userPosts" :key="post.id" class="post-card" @click="goToPostDetail(post.id)">
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
                </div>
                <div class="post-time">{{ formatDate(post.created_at) }}</div>
              </div>
            </div>
          </div>
          <div v-else class="no-content">
            <i class="fas fa-file-alt"></i>
            <p>还没有发布任何帖子</p>
          </div>
        </div>
        
        <div v-else-if="activeTab === 'likes'" class="likes-list">
          <div v-if="likesLoading" class="loading-indicator">
            <i class="fas fa-spinner fa-spin"></i> 加载中...
          </div>
          <div v-else-if="likedPosts.length > 0" class="posts-list">
            <div v-for="post in likedPosts" :key="post.id" class="post-card" @click="goToPostDetail(post.id)">
              <h3 class="post-title">{{ post.title }}</h3>
              <p class="post-content">{{ post.content }}</p>
              <div class="post-footer">
                <div class="post-actions">
                  <div class="post-action">
                    <i class="fas fa-heart"></i> {{ post.likes }}
                  </div>
                  <div class="post-action">
                    <i class="far fa-comment"></i> {{ post.comments }}
                  </div>
                </div>
                <div class="post-time">{{ formatDate(post.created_at) }}</div>
              </div>
            </div>
          </div>
          <div v-else class="no-content">
            <i class="far fa-heart"></i>
            <p>还没有喜欢任何帖子</p>
          </div>
        </div>
        
        <div v-else-if="activeTab === 'favorites'" class="favorites-content">
          <div v-if="favoritesLoading" class="loading-indicator">
            <i class="fas fa-spinner fa-spin"></i> 加载中...
          </div>
          <div v-else-if="favoritedPosts && favoritedPosts.length > 0" class="posts-list">
            <div v-for="post in favoritedPosts" :key="post.id" class="post-card" @click="goToPostDetail(post.id)">
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
                  <div class="post-action bookmark">
                    <i class="fas fa-bookmark"></i>
                  </div>
                </div>
                <div class="post-time">{{ formatDate(post.created_at) }}</div>
              </div>
            </div>
          </div>
          <div v-else class="no-content">
            <i class="far fa-bookmark"></i>
            <p>还没有收藏任何帖子</p>
          </div>
        </div>
      </div>
    </template>
    
    <div v-if="showEditModal" class="edit-profile-modal">
      <div class="modal-content">
        <div class="modal-header">
          <h2>编辑个人资料</h2>
          <button class="close-btn" @click="closeEditModal">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>用户名</label>
            <input type="text" v-model="editForm.username" disabled class="form-control" />
          </div>
          <div class="form-group">
            <label>头像</label>
            <input type="text" v-model="editForm.avatar" class="form-control" placeholder="输入头像URL地址" />
          </div>
          <div class="form-group">
            <label>个人简介</label>
            <textarea v-model="editForm.bio" class="form-control" placeholder="介绍一下自己吧..."></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="closeEditModal">取消</button>
          <button class="save-btn" @click="saveProfile" :disabled="saving">
            <span v-if="saving"><i class="fas fa-spinner fa-spin"></i> 保存中...</span>
            <span v-else>保存</span>
          </button>
        </div>
      </div>
    </div>
    
    <!-- Follow users modal -->
    <div v-if="showFollowModal" class="follow-modal">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ followModalTitle }}</h2>
          <button class="close-btn" @click="closeFollowModal">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <div v-if="followUsersLoading" class="loading-indicator-small">
            <i class="fas fa-spinner fa-spin"></i> 加载中...
          </div>
          <div v-else-if="followUsers.length === 0" class="no-users">
            <p>{{ followModalEmptyText }}</p>
          </div>
          <div v-else class="user-list">
            <div v-for="user in followUsers" :key="user.id" class="user-item">
              <UserAvatar 
                :src="user.avatar" 
                :username="user.username"
                :userId="user.id"
              />
              <div class="user-info">
                <div class="user-name">{{ user.username }}</div>
                <div class="user-bio">{{ user.bio || '这个人很懒，还没有介绍自己...' }}</div>
              </div>
              <button 
                v-if="isAuthenticated && currentUserId !== user.id"
                class="follow-btn-sm" 
                :class="{ 'following': isUserInFollowingList(user.id) }"
                @click="toggleFollowUser(user.id)"
              >
                {{ isUserInFollowingList(user.id) ? '已关注' : '关注' }}
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
import { mapGetters } from 'vuex';
import apiService from '@/services/apiService';

export default {
  name: 'ProfileView',
  components: {
    UserAvatar
  },
  data() {
    return {
      loading: false,
      profileId: this.$route.params.id,
      currentUserId: parseInt(localStorage.getItem('userId')) || null,
      profileName: '',
      profileBio: '',
      profileAvatar: '',
      activeTab: 'posts',
      userPosts: [],
      likedPosts: [],
      postsLoading: false,
      likesLoading: false,
      error: null,
      favoritedPosts: [],
      favoritesLoading: false,
      showEditModal: false,
      editForm: {
        username: '',
        avatar: '',
        bio: '',
        email: ''
      },
      saving: false,
      // New follow-related data
      isFollowing: false,
      followLoading: false,
      followerCount: 0,
      followingCount: 0,
      likesCount: 0,
      showFollowModal: false,
      followModalType: 'followers', // or 'following'
      followUsers: [],
      followUsersLoading: false,
      followingUserIds: [] // IDs of users the current user is following
    };
  },
  computed: {
    ...mapGetters({
      isAuthenticated: 'isAuthenticated',
      currentUser: 'currentUser'
    }),
    formattedProfileBio() {
      return this.profileBio || '这个人很懒，还没有介绍自己...';
    },
    isCurrentUser() {
      if (!this.isAuthenticated || !this.currentUser) {
        return false;
      }
      
      return this.profileId == this.currentUser.id;
    },
    followModalTitle() {
      return this.followModalType === 'followers' ? '粉丝列表' : '关注列表';
    },
    followModalEmptyText() {
      return this.followModalType === 'followers' ? '还没有粉丝' : '还没有关注任何人';
    }
  },
  watch: {
    // 监听路由参数变化
    '$route.params.id': {
      handler(newId, oldId) {
        if (newId !== oldId) {
          this.resetData();
          this.profileId = newId;
          this.loadUserProfile();
        }
      },
      immediate: false
    },
    // Watch for authentication state change
    isAuthenticated(newValue) {
      if (newValue && this.profileId) {
        this.checkFollowStatus();
      }
    }
  },
  methods: {
    resetData() {
      // 重置所有数据状态
      this.profileName = '';
      this.profileBio = '';
      this.profileAvatar = '';
      this.activeTab = 'posts';
      this.userPosts = [];
      this.likedPosts = [];
      this.favoritedPosts = [];
      this.error = null;
    },
    async loadUserProfile() {
      try {
        this.loading = true;
        
        // 获取用户信息
        const response = await apiService.users.getProfile(this.profileId);
        const userData = response.data;
        
        // 更新用户信息
        this.profileName = userData.username;
        this.profileBio = userData.bio || '';
        this.profileAvatar = userData.avatar;
        
        // 加载用户帖子
        await this.fetchUserPosts();
        
        // 获取关注数据
        await this.fetchFollowCounts();
        
        // 如果用户已登录，检查关注状态
        if (this.isAuthenticated) {
          await this.checkFollowStatus();
        }
        
        // 如果是当前用户查看自己的资料，预加载喜欢和收藏的帖子
        if (this.isCurrentUser) {
          // 异步加载其他标签的数据，但不等待完成
          this.fetchLikedPosts();
          this.fetchFavoritedPosts();
        }
      } catch (error) {
        console.error('获取用户资料失败:', error);
        this.error = '加载用户资料失败';
      } finally {
        this.loading = false;
      }
    },
    goBack() {
      this.$router.go(-1);
    },
    logout() {
      this.$store.dispatch('logout');
      this.$router.push('/');
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
    },
    async fetchUserPosts() {
      try {
        this.postsLoading = true;
        // 修正: profileId 而不是 userId
        const response = await apiService.posts.getByUserId(this.profileId); 
        
        // 确保正确映射数据
        this.userPosts = response.data.content || response.data || [];
        console.log('获取到的用户帖子:', this.userPosts);
      } catch (error) {
        console.error('获取用户帖子失败:', error);
        this.error = '加载用户帖子失败';
      } finally {
        this.postsLoading = false;
      }
    },
    async fetchLikedPosts() {
      try {
        this.likesLoading = true;
        const response = await apiService.posts.getLikedByUserId(this.profileId);
        this.likedPosts = response.data.content || response.data || [];
        console.log('获取到的用户点赞帖子:', this.likedPosts);
      } catch (error) {
        console.error('获取用户点赞帖子失败:', error);
        this.error = '加载用户点赞帖子失败';
      } finally {
        this.likesLoading = false;
      }
    },
    setActiveTab(tab) {
      this.activeTab = tab;
      
      if (tab === 'posts' && this.userPosts.length === 0) {
        this.fetchUserPosts();
      } else if (tab === 'likes' && this.likedPosts.length === 0) {
        this.fetchLikedPosts();
      } else if (tab === 'favorites' && this.favoritedPosts.length === 0) {
        this.fetchFavoritedPosts();
      }
    },
    async fetchFavoritedPosts() {
      try {
        this.favoritesLoading = true;
        const response = await apiService.posts.getFavoritedByUserId(this.profileId);
        this.favoritedPosts = response.data.content || response.data || [];
        console.log('获取到的用户收藏帖子:', this.favoritedPosts);
      } catch (error) {
        console.error('获取用户收藏帖子失败:', error);
        this.error = '加载用户收藏帖子失败';
      } finally {
        this.favoritesLoading = false;
      }
    },
    openEditModal() {
      this.editForm = {
        username: this.profileName,
        avatar: '',
        bio: this.formattedProfileBio,
        email: ''
      };
      this.showEditModal = true;
    },
    closeEditModal() {
      this.showEditModal = false;
    },
    async saveProfile() {
      try {
        this.saving = true;
        
        await apiService.users.updateProfile(this.profileId, {
          avatar: this.editForm.avatar,
          bio: this.editForm.bio
        });
        
        // 立即更新本地数据
        this.profileAvatar = this.editForm.avatar;
        this.profileBio = this.editForm.bio; // 更新 data 中的 profileBio
        
        alert('个人资料已更新');
        this.closeEditModal();
        
        // 页面刷新以确保一切更新
        window.location.reload();
      } catch (error) {
        console.error('更新个人资料失败:', error);
        alert('更新个人资料失败: ' + (error.response?.data || error.message));
      } finally {
        this.saving = false;
      }
    },
    async fetchFollowCounts() {
      try {
        const response = await apiService.users.getFollowCounts(this.profileId);
        this.followerCount = response.data.followerCount || 0;
        this.followingCount = response.data.followingCount || 0;
        // For now, we'll use a placeholder for likes count
        this.likesCount = 0; // This would need to be implemented on the backend
      } catch (error) {
        console.error('获取关注数据失败:', error);
      }
    },
    async checkFollowStatus() {
      if (!this.isAuthenticated || this.isCurrentUser) {
        return;
      }
      
      try {
        const response = await apiService.users.checkFollowing(this.profileId);
        this.isFollowing = response.data.following;
      } catch (error) {
        console.error('检查关注状态失败:', error);
      }
    },
    async toggleFollow() {
      if (!this.isAuthenticated) {
        // Redirect to login if not authenticated
        this.$router.push('/login');
        return;
      }
      
      this.followLoading = true;
      
      try {
        if (this.isFollowing) {
          // Unfollow
          await apiService.users.unfollow(this.profileId);
          this.isFollowing = false;
          this.followerCount = Math.max(0, this.followerCount - 1);
        } else {
          // Follow
          await apiService.users.follow(this.profileId);
          this.isFollowing = true;
          this.followerCount += 1;
        }
      } catch (error) {
        console.error('操作关注失败:', error);
        alert('操作失败，请稍后再试');
      } finally {
        this.followLoading = false;
      }
    },
    async openFollowersList() {
      this.followModalType = 'followers';
      this.showFollowModal = true;
      this.followUsers = [];
      await this.fetchFollowUsers('followers');
    },
    async openFollowingList() {
      this.followModalType = 'following';
      this.showFollowModal = true;
      this.followUsers = [];
      await this.fetchFollowUsers('following');
    },
    closeFollowModal() {
      this.showFollowModal = false;
    },
    async fetchFollowUsers(type) {
      this.followUsersLoading = true;
      
      try {
        let response;
        if (type === 'followers') {
          response = await apiService.users.getFollowers(this.profileId);
          this.followUsers = response.data.followers || [];
        } else {
          response = await apiService.users.getFollowing(this.profileId);
          this.followUsers = response.data.following || [];
        }
        
        // If the current user is authenticated, fetch their following list
        // to determine which users in this list they are already following
        if (this.isAuthenticated && !this.isCurrentUser) {
          await this.fetchCurrentUserFollowing();
        }
      } catch (error) {
        console.error(`获取${type === 'followers' ? '粉丝' : '关注'}列表失败:`, error);
      } finally {
        this.followUsersLoading = false;
      }
    },
    async fetchCurrentUserFollowing() {
      try {
        const response = await apiService.users.getFollowing(this.currentUserId);
        this.followingUserIds = (response.data.following || []).map(user => user.id);
      } catch (error) {
        console.error('获取当前用户关注列表失败:', error);
      }
    },
    isUserInFollowingList(userId) {
      return this.followingUserIds.includes(userId);
    },
    async toggleFollowUser(userId) {
      if (!this.isAuthenticated) {
        this.$router.push('/login');
        return;
      }
      
      try {
        if (this.isUserInFollowingList(userId)) {
          // Unfollow
          await apiService.users.unfollow(userId);
          this.followingUserIds = this.followingUserIds.filter(id => id !== userId);
          
          // If we're on our own following list, remove the user
          if (this.isCurrentUser && this.followModalType === 'following') {
            this.followUsers = this.followUsers.filter(user => user.id !== userId);
            this.followingCount = Math.max(0, this.followingCount - 1);
          }
        } else {
          // Follow
          await apiService.users.follow(userId);
          this.followingUserIds.push(userId);
          
          // If we're on our own followers list and follow someone back,
          // update the following count
          if (this.isCurrentUser && this.followModalType === 'followers') {
            this.followingCount += 1;
          }
        }
      } catch (error) {
        console.error('操作关注失败:', error);
        alert('操作失败，请稍后再试');
      }
    },
    goToMessages() {
      // 直接导航到与该用户的聊天页面，而不是消息列表页面
      this.$router.push(`/chat/${this.profileId}`);
    }
  },
  // 添加路由导航守卫
  beforeRouteUpdate(to, from, next) {
    // 当路由参数变化但组件实例被复用时触发
    if (to.params.id !== from.params.id) {
      this.resetData();
      this.profileId = to.params.id;
      this.loadUserProfile();
    }
    next();
  },
  async created() {
    // 根据路由参数获取 profileId
    this.profileId = this.$route.params.id || (this.currentUser ? this.currentUser.id : null);
    
    // 如果用户未登录且没有提供ID，则重定向到首页
    if (!this.profileId) {
      this.$router.push('/');
      return;
    }
    
    await this.loadUserProfile();
  },
  mounted() {
    // 不需要在这里重复调用fetchUserProfile
  }
}
</script>

<style scoped>
.profile-view {
  flex: 1;
  background-color: var(--background-color);
}

.profile-header {
  display: flex;
  justify-content: space-between;
  padding: 15px;
  background-color: white;
  position: sticky;
  top: 0;
  z-index: 10;
}

.back-button {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: var(--text-color);
}

.profile-header-actions {
  display: flex;
  gap: 15px;
}

.action-button {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: var(--text-color);
}

.loading-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 0;
  color: var(--light-text-color);
}

.loading-indicator i {
  font-size: 32px;
  margin-bottom: 15px;
}

.profile-info {
  padding: 20px;
  text-align: center;
  background-color: white;
}

/* Style for the profile avatar component */
.profile-avatar-component {
  display: inline-block;
  margin-bottom: 15px;
}

.profile-avatar-component :deep(.user-avatar) {
  width: 100px;
  height: 100px;
}

/* Hide the old profile-avatar class */
.profile-avatar {
  display: none;
}

.profile-name {
  font-size: 22px;
  font-weight: 600;
  margin: 0 0 5px;
}

.profile-userid {
  font-size: 14px;
  color: var(--light-text-color);
  margin-bottom: 20px;
}

.profile-stats {
  display: flex;
  justify-content: center;
  gap: 30px;
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
}

.stat-label {
  font-size: 14px;
  color: var(--light-text-color);
}

.profile-bio {
  margin: 0 auto 20px;
  max-width: 400px;
  font-size: 15px;
  line-height: 1.5;
  color: var(--text-color);
}

.profile-actions {
  display: flex;
  justify-content: center;
  gap: 15px;
}

.follow-btn,
.edit-profile-btn,
.message-btn {
  padding: 8px 20px;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
}

.follow-btn,
.edit-profile-btn {
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-end));
  color: white;
  border: none;
}

.message-btn {
  background-color: var(--secondary-color);
  border: none;
  color: var(--text-color);
}

.profile-tabs {
  display: flex;
  background-color: white;
  margin-top: 10px;
  border-bottom: 1px solid var(--border-color);
}

.profile-tab {
  flex: 1;
  text-align: center;
  padding: 15px 0;
  font-size: 15px;
  color: var(--light-text-color);
  cursor: pointer;
  position: relative;
}

.profile-tab.active {
  color: var(--primary-color);
  font-weight: 500;
}

.profile-tab.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 3px;
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-end));
  transform: translateY(1px);
}

.profile-content {
  padding: 15px;
}

.no-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 50px 0;
  color: var(--light-text-color);
  text-align: center;
}

.no-content i {
  font-size: 32px;
  margin-bottom: 15px;
}

.posts-list .post-card {
  background-color: white;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 15px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.posts-list .post-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 10px 0;
}

.posts-list .post-content {
  font-size: 14px;
  color: var(--light-text-color);
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.posts-list .post-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--light-text-color);
  font-size: 14px;
}

.posts-list .post-actions {
  display: flex;
  gap: 15px;
}

.posts-list .post-action {
  display: flex;
  align-items: center;
  gap: 5px;
}

.posts-list .post-time {
  font-size: 12px;
}

.logout-btn {
  background-color: #f5f5f5;
  color: var(--text-color);
  border: 1px solid var(--border-color);
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 5px;
}

.logout-btn:hover {
  background-color: #f0f0f0;
  border-color: #d0d0d0;
}

@media (max-width: 576px) {
  .profile-info {
    padding: 15px;
  }
  
  .profile-avatar-component :deep(.user-avatar) {
    width: 80px;
    height: 80px;
  }
  
  .profile-name {
    font-size: 20px;
  }
  
  .profile-stats {
    gap: 20px;
  }
  
  .stat-value {
    font-size: 16px;
  }
  
  .profile-actions {
    flex-wrap: wrap;
  }
  
  .follow-btn,
  .edit-profile-btn,
  .message-btn {
    padding: 6px 15px;
    font-size: 13px;
  }
}

/* 为收藏图标添加样式 */
.bookmark {
  color: var(--primary-color);
}

.edit-profile-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background-color: white;
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid var(--border-color);
}

.modal-header h2 {
  margin: 0;
  font-size: 18px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #666;
}

.modal-body {
  padding: 20px;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
  color: #444;
}

.form-control {
  width: 100%;
  padding: 10px;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  font-size: 14px;
}

textarea.form-control {
  min-height: 100px;
  resize: vertical;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  padding: 15px 20px;
  border-top: 1px solid var(--border-color);
  gap: 10px;
}

.cancel-btn, .save-btn {
  padding: 8px 20px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  border: none;
}

.cancel-btn {
  background-color: #f5f5f5;
  color: #444;
}

.save-btn {
  background-color: var(--primary-color);
  color: white;
}

.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* New styles for follow features */
.follow-btn.following {
  /* 使用明显不同的样式来表示已关注状态 */
  background-image: none !important;
  background-color: white !important;
  color: var(--primary-color) !important;
  border: 1px solid var(--primary-color) !important;
  opacity: 1 !important;
}

/* 只在真正悬停时改变样式，显示取消关注的视觉效果 */
.follow-btn.following:hover {
  background-color: #f8d7da !important;
  background-image: none !important; /* 悬停时去掉渐变 */
  border: 1px solid #f5c6cb !important;
  color: #721c24 !important;
}

.follow-btn.following:hover i {
  display: none;
}

.follow-btn.following:hover::after {
  content: '取消关注';
  font-size: 14px;
}

.loading-indicator-small {
  text-align: center;
  padding: 20px;
  color: var(--light-text-color);
}

.follow-btn-sm {
  padding: 5px 10px;
  border-radius: 4px;
  font-size: 12px;
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-end));
  color: white;
  border: none;
  cursor: pointer;
}

.follow-btn-sm.following {
  /* 使用明显不同的样式来表示已关注状态 */
  background-image: none !important;
  background-color: white !important;
  color: var(--primary-color) !important;
  border: 1px solid var(--primary-color) !important;
  opacity: 1 !important;
}

/* 小按钮的hover样式 */
.follow-btn-sm.following:hover {
  background-color: #f8d7da !important;
  background-image: none !important;
  border: 1px solid #f5c6cb !important;
  color: #721c24 !important;
}

.follow-btn-sm.following:hover::after {
  content: '取消关注';
  font-size: 12px;
  margin-left: 2px;
}

.follow-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.follow-modal .modal-content {
  background-color: white;
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}

.follow-modal .modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid var(--border-color);
}

.follow-modal .modal-header h2 {
  margin: 0;
  font-size: 18px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #666;
}

.follow-modal .modal-body {
  padding: 15px;
}

.no-users {
  text-align: center;
  padding: 30px 0;
  color: var(--light-text-color);
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.user-item {
  display: flex;
  align-items: center;
  padding: 10px;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.user-item:hover {
  background-color: #f9f9f9;
}

.user-info {
  flex: 1;
  margin-left: 12px;
  overflow: hidden;
}

.user-name {
  font-weight: 500;
  font-size: 15px;
  margin-bottom: 2px;
}

.user-bio {
  font-size: 13px;
  color: var(--light-text-color);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Make the profile stats clickable */
.stat-item {
  cursor: pointer;
  transition: transform 0.2s;
}

.stat-item:hover {
  transform: translateY(-2px);
}

.stat-item:hover .stat-value {
  color: var(--primary-color);
}
</style>