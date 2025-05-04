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
        <h1 class="profile-name">{{ profileNickname || defaultNickname }}</h1>
        <div class="profile-userid">锦书号: {{ profileName }}</div>
        
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
            <label>昵称</label>
            <input type="text" v-model="editForm.nickname" class="form-control" placeholder="设置一个昵称" />
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
import apiService from '@/services/apiService';
import { mapGetters } from 'vuex';
import cacheService, { CACHE_TYPES } from '@/services/cacheService';

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
      profileNickname: '',
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
        nickname: '',
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
      followingUserIds: [], // IDs of users the current user is following
      // 缓存相关
      useCacheForPosts: true,
      useCacheForLikes: true,
      useCacheForFavorites: true,
      cachedPostsTime: null,
      cachedLikesTime: null,
      cachedFavoritesTime: null
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
    defaultNickname() {
      return this.profileName ? `锦书用户_${this.profileName}` : '锦书用户';
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
      this.profileNickname = '';
      this.profileBio = '';
      this.profileAvatar = '';
      this.activeTab = 'posts';
      this.userPosts = [];
      this.likedPosts = [];
      this.favoritedPosts = [];
      this.error = null;
      // 重置缓存时间戳
      this.cachedPostsTime = null;
      this.cachedLikesTime = null;
      this.cachedFavoritesTime = null;
    },
    async loadUserProfile() {
      try {
        this.loading = true;
        
        // 尝试从缓存加载用户信息
        const profileCacheKey = cacheService.generateKey(CACHE_TYPES.USER_PROFILE, this.profileId);
        const cachedProfile = cacheService.getCache(profileCacheKey);
        
        if (cachedProfile) {
          console.log('使用缓存的用户信息数据');
          // 使用缓存数据
          this.profileName = cachedProfile.username;
          this.profileNickname = cachedProfile.nickname || '';
          this.profileBio = cachedProfile.bio || '';
          this.profileAvatar = cachedProfile.avatar;
          this.followerCount = cachedProfile.followerCount || 0;
          this.followingCount = cachedProfile.followingCount || 0;
          this.likesCount = cachedProfile.likesCount || 0;
          
          // 异步更新缓存
          this.fetchProfileDataAndCache();
        } else {
          // 缓存不存在，直接从服务器获取
          await this.fetchProfileDataAndCache();
        }
        
        // 加载用户帖子
        await this.fetchUserPosts();
        
        // 如果用户已登录，检查关注状态
        if (this.isAuthenticated && !this.isCurrentUser) {
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
    async fetchProfileDataAndCache() {
      try {
        // 获取用户信息
        const response = await apiService.users.getProfile(this.profileId);
        const userData = response.data;
        
        // 更新用户信息
        this.profileName = userData.username;
        this.profileNickname = userData.nickname || '';
        this.profileBio = userData.bio || '';
        this.profileAvatar = userData.avatar;
        
        // 获取关注数据
        await this.fetchFollowCounts();
        
        // 缓存用户数据
        const profileData = {
          username: userData.username,
          nickname: userData.nickname || '',
          bio: userData.bio || '',
          avatar: userData.avatar,
          followerCount: this.followerCount,
          followingCount: this.followingCount,
          likesCount: this.likesCount
        };
        
        const profileCacheKey = cacheService.generateKey(CACHE_TYPES.USER_PROFILE, this.profileId);
        cacheService.setCache(profileCacheKey, profileData);
        
      } catch (error) {
        console.error('获取和缓存用户资料失败:', error);
        throw error; // 向上传递错误
      }
    },
    goBack() {
      this.$router.go(-1);
    },
    logout() {
      // 退出前清除当前用户的缓存
      if (this.currentUser && this.currentUser.id) {
        cacheService.clearUserCache(this.currentUser.id);
      }
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
        
        // 尝试从缓存加载帖子数据
        if (this.useCacheForPosts) {
          const cacheKey = cacheService.generateKey(CACHE_TYPES.USER_POSTS, this.profileId);
          const cachedPosts = cacheService.getCache(cacheKey);
          
          if (cachedPosts) {
            console.log('使用缓存的用户帖子数据');
            this.userPosts = cachedPosts;
            this.cachedPostsTime = Date.now();
            
            // 如果这是当前活动标签，异步更新缓存
            if (this.activeTab === 'posts') {
              this.refreshPostsCache();
            }
            
            this.postsLoading = false;
            return;
          }
        }
        
        // 缓存不存在或不使用缓存，从服务器获取
        await this.refreshPostsCache();
        
      } catch (error) {
        console.error('获取用户帖子失败:', error);
        this.error = '加载用户帖子失败';
      } finally {
        this.postsLoading = false;
      }
    },
    async refreshPostsCache() {
      try {
        // 从服务器获取最新数据
        const response = await apiService.posts.getByUserId(this.profileId); 
        
        // 更新本地数据
        this.userPosts = response.data.content || response.data || [];
        this.cachedPostsTime = Date.now();
        
        // 缓存新数据
        const cacheKey = cacheService.generateKey(CACHE_TYPES.USER_POSTS, this.profileId);
        cacheService.setCache(cacheKey, this.userPosts);
        console.log('用户帖子数据已缓存');
        
      } catch (error) {
        console.error('刷新用户帖子缓存失败:', error);
        throw error; // 向上传递错误
      }
    },
    async fetchLikedPosts() {
      try {
        this.likesLoading = true;
        
        // 尝试从缓存加载点赞数据
        if (this.useCacheForLikes) {
          const cacheKey = cacheService.generateKey(CACHE_TYPES.USER_LIKES, this.profileId);
          const cachedLikes = cacheService.getCache(cacheKey);
          
          if (cachedLikes) {
            console.log('使用缓存的用户点赞数据');
            this.likedPosts = cachedLikes;
            this.cachedLikesTime = Date.now();
            
            // 如果这是当前活动标签，异步更新缓存
            if (this.activeTab === 'likes') {
              this.refreshLikesCache();
            }
            
            this.likesLoading = false;
            return;
          }
        }
        
        // 缓存不存在或不使用缓存，从服务器获取
        await this.refreshLikesCache();
        
      } catch (error) {
        console.error('获取用户点赞帖子失败:', error);
        this.error = '加载用户点赞帖子失败';
      } finally {
        this.likesLoading = false;
      }
    },
    async refreshLikesCache() {
      try {
        // 从服务器获取最新数据
        const response = await apiService.posts.getLikedByUserId(this.profileId);
        
        // 更新本地数据
        this.likedPosts = response.data.content || response.data || [];
        this.cachedLikesTime = Date.now();
        
        // 缓存新数据
        const cacheKey = cacheService.generateKey(CACHE_TYPES.USER_LIKES, this.profileId);
        cacheService.setCache(cacheKey, this.likedPosts);
        console.log('用户点赞数据已缓存');
        
      } catch (error) {
        console.error('刷新用户点赞缓存失败:', error);
        throw error; // 向上传递错误
      }
    },
    setActiveTab(tab) {
      // 如果是相同标签，不做任何操作
      if (this.activeTab === tab) return;
      
      this.activeTab = tab;
      
      if (tab === 'posts' && this.userPosts.length === 0) {
        this.fetchUserPosts();
      } else if (tab === 'likes' && this.likedPosts.length === 0) {
        this.fetchLikedPosts();
      } else if (tab === 'favorites' && this.favoritedPosts.length === 0) {
        this.fetchFavoritedPosts();
      } else {
        // 如果已有数据但缓存已超过5分钟，则在后台刷新数据
        const now = Date.now();
        if (tab === 'posts' && now - this.cachedPostsTime > 5 * 60 * 1000) {
          this.refreshPostsCache().catch(e => console.error('背景刷新帖子失败:', e));
        } else if (tab === 'likes' && now - this.cachedLikesTime > 5 * 60 * 1000) {
          this.refreshLikesCache().catch(e => console.error('背景刷新点赞失败:', e));
        } else if (tab === 'favorites' && now - this.cachedFavoritesTime > 5 * 60 * 1000) {
          this.refreshFavoritesCache().catch(e => console.error('背景刷新收藏失败:', e));
        }
      }
    },
    async fetchFavoritedPosts() {
      try {
        this.favoritesLoading = true;
        
        // 尝试从缓存加载收藏数据
        if (this.useCacheForFavorites) {
          const cacheKey = cacheService.generateKey(CACHE_TYPES.USER_FAVORITES, this.profileId);
          const cachedFavorites = cacheService.getCache(cacheKey);
          
          if (cachedFavorites) {
            console.log('使用缓存的用户收藏数据');
            this.favoritedPosts = cachedFavorites;
            this.cachedFavoritesTime = Date.now();
            
            // 如果这是当前活动标签，异步更新缓存
            if (this.activeTab === 'favorites') {
              this.refreshFavoritesCache();
            }
            
            this.favoritesLoading = false;
            return;
          }
        }
        
        // 缓存不存在或不使用缓存，从服务器获取
        await this.refreshFavoritesCache();
        
      } catch (error) {
        console.error('获取用户收藏帖子失败:', error);
        this.error = '加载用户收藏帖子失败';
      } finally {
        this.favoritesLoading = false;
      }
    },
    async refreshFavoritesCache() {
      try {
        // 从服务器获取最新数据
        const response = await apiService.posts.getFavoritedByUserId(this.profileId);
        
        // 更新本地数据
        this.favoritedPosts = response.data.content || response.data || [];
        this.cachedFavoritesTime = Date.now();
        
        // 缓存新数据
        const cacheKey = cacheService.generateKey(CACHE_TYPES.USER_FAVORITES, this.profileId);
        cacheService.setCache(cacheKey, this.favoritedPosts);
        console.log('用户收藏数据已缓存');
        
      } catch (error) {
        console.error('刷新用户收藏缓存失败:', error);
        throw error; // 向上传递错误
      }
    },
    openEditModal() {
      this.editForm = {
        username: this.profileName,
        nickname: this.profileNickname,
        avatar: this.profileAvatar,
        bio: this.profileBio,
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
          bio: this.editForm.bio,
          nickname: this.editForm.nickname
        });
        
        // 立即更新本地数据
        this.profileAvatar = this.editForm.avatar;
        this.profileBio = this.editForm.bio;
        this.profileNickname = this.editForm.nickname;
        
        // 更新缓存
        const profileCacheKey = cacheService.generateKey(CACHE_TYPES.USER_PROFILE, this.profileId);
        cacheService.clearTypeCache(CACHE_TYPES.USER_PROFILE, this.profileId);
        
        // 重新缓存用户资料
        const profileData = {
          username: this.profileName,
          nickname: this.profileNickname,
          bio: this.profileBio,
          avatar: this.profileAvatar,
          followerCount: this.followerCount,
          followingCount: this.followingCount,
          likesCount: this.likesCount
        };
        cacheService.setCache(profileCacheKey, profileData);
        
        alert('个人资料已更新');
        this.closeEditModal();
        
        // 使用温和的重载方式，避免整页刷新
        this.resetData();
        await this.loadUserProfile();
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
          
          // 清除相关缓存
          cacheService.clearTypeCache(CACHE_TYPES.USER_PROFILE, this.profileId);
        } else {
          // Follow
          await apiService.users.follow(this.profileId);
          this.isFollowing = true;
          this.followerCount += 1;
          
          // 清除相关缓存
          cacheService.clearTypeCache(CACHE_TYPES.USER_PROFILE, this.profileId);
        }
        
        // 更新用户资料缓存
        const profileData = {
          username: this.profileName,
          nickname: this.profileNickname,
          bio: this.profileBio,
          avatar: this.profileAvatar,
          followerCount: this.followerCount,
          followingCount: this.followingCount,
          likesCount: this.likesCount
        };
        const profileCacheKey = cacheService.generateKey(CACHE_TYPES.USER_PROFILE, this.profileId);
        cacheService.setCache(profileCacheKey, profileData);
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
          
          // 清除相关缓存
          cacheService.clearTypeCache(CACHE_TYPES.USER_PROFILE, userId);
        } else {
          // Follow
          await apiService.users.follow(userId);
          this.followingUserIds.push(userId);
          
          // If we're on our own followers list and follow someone back,
          // update the following count
          if (this.isCurrentUser && this.followModalType === 'followers') {
            this.followingCount += 1;
          }
          
          // 清除相关缓存
          cacheService.clearTypeCache(CACHE_TYPES.USER_PROFILE, userId);
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
  },
  beforeUnmount() {
    // 离开组件前确保不会有未完成的后台异步操作
    this.useCacheForPosts = false;
    this.useCacheForLikes = false;
    this.useCacheForFavorites = false;
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
  margin-bottom: 12px;
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