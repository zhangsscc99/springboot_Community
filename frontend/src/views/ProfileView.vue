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
      currentUserId: null,
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
      isFollowing: false,
      followLoading: false,
      followerCount: 0,
      followingCount: 0,
      likesCount: 0,
      showFollowModal: false,
      followModalType: 'followers',
      followUsers: [],
      followUsersLoading: false,
      followingUserIds: [],
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
    isAuthenticated(newValue) {
      if (newValue && this.profileId) {
        this.checkFollowStatus();
      }
    }
  },
  methods: {
    resetData() {
      this.profileName = '';
      this.profileNickname = '';
      this.profileBio = '';
      this.profileAvatar = '';
      this.activeTab = 'posts';
      this.userPosts = [];
      this.likedPosts = [];
      this.favoritedPosts = [];
      this.error = null;
      this.cachedPostsTime = null;
      this.cachedLikesTime = null;
      this.cachedFavoritesTime = null;
    },
    async loadUserProfile() {
      try {
        this.loading = true;
        
        const profileCacheKey = cacheService.generateKey(CACHE_TYPES.USER_PROFILE, this.profileId);
        const cachedProfile = cacheService.getCache(profileCacheKey);
        
        if (cachedProfile) {
          console.log('使用缓存的用户信息数据');
          this.profileName = cachedProfile.username;
          this.profileNickname = cachedProfile.nickname || '';
          this.profileBio = cachedProfile.bio || '';
          this.profileAvatar = cachedProfile.avatar;
          
          // 临时使用缓存的关注数据
          this.followerCount = cachedProfile.followerCount || 0;
          this.followingCount = cachedProfile.followingCount || 0;
          this.likesCount = cachedProfile.likesCount || 0;
          
          // 但无论如何都要异步更新关注数据，确保显示最新的关注/粉丝数
          this.fetchFollowCounts().then(counts => {
            // 如果实际关注数与缓存不同，刷新缓存
            if (counts.followerCount !== this.followerCount || 
                counts.followingCount !== this.followingCount) {
              this.fetchProfileDataAndCache();
            }
          });
          
          // 异步更新缓存
          this.fetchProfileDataAndCache();
        } else {
          await this.fetchProfileDataAndCache();
        }
        
        await this.fetchUserPosts();
        
        if (this.isAuthenticated && !this.isCurrentUser) {
          await this.checkFollowStatus();
        }
        
        if (this.isCurrentUser) {
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
        const response = await apiService.users.getProfile(this.profileId);
        const userData = response.data;
        
        this.profileName = userData.username;
        this.profileNickname = userData.nickname || '';
        this.profileBio = userData.bio || '';
        this.profileAvatar = userData.avatar;
        
        await this.fetchFollowCounts();
        
        if (this.isCurrentUser) {
          this.updateLocalUserInfo();
        }
        
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
        throw error;
      }
    },
    updateLocalUserInfo() {
      // 更新localStorage中的用户信息
      const userInfoStr = localStorage.getItem('userInfo');
      if (userInfoStr) {
        try {
          const userInfo = JSON.parse(userInfoStr);
          
          // 更新关注计数
          userInfo.followingCount = this.followingCount;
          userInfo.followerCount = this.followerCount;
          
          // 确保userId也存在于localStorage中
          if (userInfo.id) {
            localStorage.setItem('userId', userInfo.id);
          }
          
          // 保存回localStorage
          localStorage.setItem('userInfo', JSON.stringify(userInfo));
          console.log('更新了localStorage中的用户关注数据:', {
            followingCount: this.followingCount,
            followerCount: this.followerCount
          });
        } catch (e) {
          console.error('更新localStorage用户信息失败:', e);
        }
      }
    },
    goBack() {
      this.$router.go(-1);
    },
    logout() {
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
        
        if (this.useCacheForPosts) {
          const cacheKey = cacheService.generateKey(CACHE_TYPES.USER_POSTS, this.profileId);
          const cachedPosts = cacheService.getCache(cacheKey);
          
          if (cachedPosts) {
            console.log('使用缓存的用户帖子数据');
            this.userPosts = cachedPosts;
            this.cachedPostsTime = Date.now();
            
            if (this.activeTab === 'posts') {
              this.refreshPostsCache();
            }
            
            this.postsLoading = false;
            return;
          }
        }
        
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
        const response = await apiService.posts.getByUserId(this.profileId); 
        
        this.userPosts = response.data.content || response.data || [];
        this.cachedPostsTime = Date.now();
        
        const cacheKey = cacheService.generateKey(CACHE_TYPES.USER_POSTS, this.profileId);
        cacheService.setCache(cacheKey, this.userPosts);
        console.log('用户帖子数据已缓存');
        
      } catch (error) {
        console.error('刷新用户帖子缓存失败:', error);
        throw error;
      }
    },
    async fetchLikedPosts() {
      try {
        this.likesLoading = true;
        
        if (this.useCacheForLikes) {
          const cacheKey = cacheService.generateKey(CACHE_TYPES.USER_LIKES, this.profileId);
          const cachedLikes = cacheService.getCache(cacheKey);
          
          if (cachedLikes) {
            console.log('使用缓存的用户点赞数据');
            this.likedPosts = cachedLikes;
            this.cachedLikesTime = Date.now();
            
            if (this.activeTab === 'likes') {
              this.refreshLikesCache();
            }
            
            this.likesLoading = false;
            return;
          }
        }
        
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
        const response = await apiService.posts.getLikedByUserId(this.profileId);
        
        this.likedPosts = response.data.content || response.data || [];
        this.cachedLikesTime = Date.now();
        
        const cacheKey = cacheService.generateKey(CACHE_TYPES.USER_LIKES, this.profileId);
        cacheService.setCache(cacheKey, this.likedPosts);
        console.log('用户点赞数据已缓存');
        
      } catch (error) {
        console.error('刷新用户点赞缓存失败:', error);
        throw error;
      }
    },
    setActiveTab(tab) {
      if (this.activeTab === tab) return;
      
      this.activeTab = tab;
      
      if (tab === 'posts' && this.userPosts.length === 0) {
        this.fetchUserPosts();
      } else if (tab === 'likes' && this.likedPosts.length === 0) {
        this.fetchLikedPosts();
      } else if (tab === 'favorites' && this.favoritedPosts.length === 0) {
        this.fetchFavoritedPosts();
      } else {
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
        
        if (this.useCacheForFavorites) {
          const cacheKey = cacheService.generateKey(CACHE_TYPES.USER_FAVORITES, this.profileId);
          const cachedFavorites = cacheService.getCache(cacheKey);
          
          if (cachedFavorites) {
            console.log('使用缓存的用户收藏数据');
            this.favoritedPosts = cachedFavorites;
            this.cachedFavoritesTime = Date.now();
            
            if (this.activeTab === 'favorites') {
              this.refreshFavoritesCache();
            }
            
            this.favoritesLoading = false;
            return;
          }
        }
        
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
        const response = await apiService.posts.getFavoritedByUserId(this.profileId);
        
        this.favoritedPosts = response.data.content || response.data || [];
        this.cachedFavoritesTime = Date.now();
        
        const cacheKey = cacheService.generateKey(CACHE_TYPES.USER_FAVORITES, this.profileId);
        cacheService.setCache(cacheKey, this.favoritedPosts);
        console.log('用户收藏数据已缓存');
        
      } catch (error) {
        console.error('刷新用户收藏缓存失败:', error);
        throw error;
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
        
        this.profileAvatar = this.editForm.avatar;
        this.profileBio = this.editForm.bio;
        this.profileNickname = this.editForm.nickname;
        
        const profileCacheKey = cacheService.generateKey(CACHE_TYPES.USER_PROFILE, this.profileId);
        cacheService.clearTypeCache(CACHE_TYPES.USER_PROFILE, this.profileId);
        
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
        console.log('获取用户关注数据, 用户ID:', this.profileId);
        const response = await apiService.users.getFollowCounts(this.profileId);
        
        // 更新关注相关的计数
        const followerCount = response.data.followerCount || 0;
        const followingCount = response.data.followingCount || 0;
        
        console.log('获取到的关注数据:', {
          followerCount: followerCount,
          followingCount: followingCount
        });
        
        // 更新状态
        this.followerCount = followerCount;
        this.followingCount = followingCount;
        this.likesCount = response.data.likesCount || 0;
        
        // 如果是当前用户，更新localStorage中的数据
        if (this.isCurrentUser) {
          this.updateLocalUserInfo();
        }
        
        return {
          followerCount,
          followingCount
        };
      } catch (error) {
        console.error('获取关注数据失败:', error);
        return {
          followerCount: 0,
          followingCount: 0
        };
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
        this.$router.push('/login');
        return;
      }
      
      this.followLoading = true;
      
      try {
        console.log(`切换关注状态, 当前状态: ${this.isFollowing ? '已关注' : '未关注'}, 用户ID: ${this.profileId}`);
        
        if (this.isFollowing) {
          // 取消关注 - 先乐观更新UI
          this.isFollowing = false;
          this.followerCount = Math.max(0, this.followerCount - 1);
          
          // 然后调用API
          await apiService.users.unfollow(this.profileId);
          console.log(`已取消关注用户: ${this.profileId}`);
          
          // 更新当前用户的关注计数
          if (this.currentUser) {
            this.currentUser.followingCount = Math.max(0, (this.currentUser.followingCount || 0) - 1);
            const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
            userInfo.followingCount = Math.max(0, (userInfo.followingCount || 0) - 1);
            localStorage.setItem('userInfo', JSON.stringify(userInfo));
          }
          
          // 清除相关缓存
          cacheService.clearTypeCache(CACHE_TYPES.USER_PROFILE, this.profileId);
          if (this.currentUserId) {
            cacheService.clearTypeCache(CACHE_TYPES.USER_PROFILE, this.currentUserId);
          }
        } else {
          // 关注 - 先乐观更新UI
          this.isFollowing = true;
          this.followerCount += 1;
          
          // 然后调用API
          await apiService.users.follow(this.profileId);
          console.log(`已关注用户: ${this.profileId}`);
          
          // 更新当前用户的关注计数
          if (this.currentUser) {
            this.currentUser.followingCount = (this.currentUser.followingCount || 0) + 1;
            const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
            userInfo.followingCount = (userInfo.followingCount || 0) + 1;
            localStorage.setItem('userInfo', JSON.stringify(userInfo));
          }
          
          // 清除相关缓存
          cacheService.clearTypeCache(CACHE_TYPES.USER_PROFILE, this.profileId);
          if (this.currentUserId) {
            cacheService.clearTypeCache(CACHE_TYPES.USER_PROFILE, this.currentUserId);
          }
        }
        
        // 确保更新follower属性缓存
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
        
        // 刷新当前用户的关注列表
        if (this.isAuthenticated) {
          await this.fetchCurrentUserFollowing();
        }
      } catch (error) {
        console.error('操作关注失败:', error);
        
        // 恢复UI状态
        if (this.isFollowing) {
          this.isFollowing = false;
          this.followerCount = Math.max(0, this.followerCount - 1);
        } else {
          this.isFollowing = true;
          this.followerCount += 1;
        }
        
        alert('操作失败，请稍后再试');
      } finally {
        this.followLoading = false;
      }
    },
    async openFollowersList() {
      this.followModalType = 'followers';
      this.showFollowModal = true;
      this.followUsers = [];
      
      // 确保有正确的currentUserId
      if (this.isAuthenticated && !this.currentUserId) {
        this.currentUserId = parseInt(localStorage.getItem('userId')) || null;
      }
      
      // 先获取当前用户关注的用户列表（用于显示关注状态）
      if (this.isAuthenticated) {
        await this.fetchCurrentUserFollowing();
      }
      
      await this.fetchFollowUsers('followers');
    },
    async openFollowingList() {
      this.followModalType = 'following';
      this.showFollowModal = true;
      this.followUsers = [];
      
      // 确保有正确的currentUserId
      if (this.isAuthenticated && !this.currentUserId) {
        this.currentUserId = parseInt(localStorage.getItem('userId')) || null;
      }
      
      // 先获取当前用户关注的用户列表（用于显示关注状态）
      if (this.isAuthenticated) {
        await this.fetchCurrentUserFollowing();
      }
      
      await this.fetchFollowUsers('following');
    },
    closeFollowModal() {
      this.showFollowModal = false;
    },
    async fetchFollowUsers(type) {
      this.followUsersLoading = true;
      
      try {
        console.log(`获取${type === 'followers' ? '粉丝' : '关注'}列表, 用户ID: ${this.profileId}`);
        let response;
        if (type === 'followers') {
          response = await apiService.users.getFollowers(this.profileId);
          this.followUsers = response.data.followers || [];
          console.log('获取到粉丝列表:', this.followUsers.length, '个粉丝');
        } else {
          response = await apiService.users.getFollowing(this.profileId);
          this.followUsers = response.data.following || [];
          console.log('获取到关注列表:', this.followUsers.length, '个关注');
        }
        
        // 如果当前用户已登录并且有效，确保更新关注状态显示
        if (this.isAuthenticated && !this.isCurrentUser) {
          // 检查是否已经获取了当前用户的关注列表
          if (this.followingUserIds.length === 0) {
            await this.fetchCurrentUserFollowing();
          }
          
          // 在UI上标记已关注的用户
          this.followUsers.forEach(user => {
            console.log(`检查用户 ${user.username} (ID:${user.id}) 的关注状态:`, 
                      this.isUserInFollowingList(user.id) ? '已关注' : '未关注');
          });
        }
      } catch (error) {
        console.error(`获取${type === 'followers' ? '粉丝' : '关注'}列表失败:`, error);
        this.followUsers = []; // 确保失败时清空列表
      } finally {
        this.followUsersLoading = false;
      }
    },
    async fetchCurrentUserFollowing() {
      try {
        // Make sure currentUserId is valid
        if (!this.currentUserId && this.isAuthenticated) {
          // Try to get currentUserId from localStorage or userInfo
          const userInfoStr = localStorage.getItem('userInfo');
          if (userInfoStr) {
            try {
              const userInfo = JSON.parse(userInfoStr);
              if (userInfo && userInfo.id) {
                this.currentUserId = userInfo.id;
              }
            } catch (e) {
              console.error('解析userInfo失败:', e);
            }
          }
          
          // If still no currentUserId, try to get from localStorage.userId
          if (!this.currentUserId) {
            this.currentUserId = parseInt(localStorage.getItem('userId')) || null;
          }
        }
        
        if (!this.currentUserId) {
          console.error('无法获取当前用户ID，无法检查关注状态');
          return;
        }
        
        console.log('获取当前用户(ID:', this.currentUserId, ')的关注列表');
        const response = await apiService.users.getFollowing(this.currentUserId);
        this.followingUserIds = (response.data.following || []).map(user => user.id);
        console.log('当前用户关注的用户IDs:', this.followingUserIds);
      } catch (error) {
        console.error('获取当前用户关注列表失败:', error);
      }
    },
    isUserInFollowingList(userId) {
      return this.followingUserIds.includes(parseInt(userId));
    },
    async toggleFollowUser(userId) {
      if (!this.isAuthenticated) {
        this.$router.push('/login');
        return;
      }
      
      try {
        const isFollowing = this.isUserInFollowingList(userId);
        console.log('切换关注状态, 用户ID:', userId, '当前状态:', isFollowing ? '已关注' : '未关注');
        
        if (isFollowing) {
          // 取消关注 - 乐观更新UI
          this.followingUserIds = this.followingUserIds.filter(id => id !== parseInt(userId));
          
          // 异步调用API
          await apiService.users.unfollow(userId);
          console.log('已取消关注用户:', userId);
          
          // 如果在自己的关注列表页面，从列表中移除该用户
          if (this.isCurrentUser && this.followModalType === 'following') {
            this.followUsers = this.followUsers.filter(user => user.id !== parseInt(userId));
            this.followingCount = Math.max(0, this.followingCount - 1);
          }
          
          // 更新用户资料缓存
          this.updateLocalUserInfo();
          
          // 清除相关缓存
          cacheService.clearTypeCache(CACHE_TYPES.USER_PROFILE, userId);
          cacheService.clearTypeCache(CACHE_TYPES.USER_PROFILE, this.currentUserId);
        } else {
          // 关注 - 乐观更新UI
          this.followingUserIds.push(parseInt(userId));
          
          // 异步调用API
          await apiService.users.follow(userId);
          console.log('已关注用户:', userId);
          
          // 如果在自己的粉丝列表页面并关注了粉丝，更新关注计数
          if (this.isCurrentUser && this.followModalType === 'followers') {
            this.followingCount += 1;
          }
          
          // 更新用户资料缓存
          this.updateLocalUserInfo();
          
          // 清除相关缓存
          cacheService.clearTypeCache(CACHE_TYPES.USER_PROFILE, userId);
          cacheService.clearTypeCache(CACHE_TYPES.USER_PROFILE, this.currentUserId);
        }
      } catch (error) {
        console.error('操作关注失败:', error);
        alert('操作失败，请稍后再试');
        
        // 如果API调用失败，回滚UI更新
        await this.fetchCurrentUserFollowing();
      }
    },
    goToMessages() {
      this.$router.push(`/chat/${this.profileId}`);
    }
  },
  beforeRouteUpdate(to, from, next) {
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
    
    // 设置当前用户ID - 首先从localStorage.userId获取
    this.currentUserId = parseInt(localStorage.getItem('userId')) || null;
    
    // 如果获取失败，尝试从userInfo中获取
    if (!this.currentUserId && this.isAuthenticated) {
      const userInfoStr = localStorage.getItem('userInfo');
      if (userInfoStr) {
        try {
          const userInfo = JSON.parse(userInfoStr);
          if (userInfo && userInfo.id) {
            this.currentUserId = userInfo.id;
            // 确保userId存在localStorage中，方便其他地方使用
            localStorage.setItem('userId', userInfo.id);
            console.log('已从userInfo中获取并设置userId:', userInfo.id);
          }
        } catch (e) {
          console.error('解析userInfo失败:', e);
        }
      }
    }
    
    console.log('当前状态检查: 用户ID=', this.currentUserId, '资料页ID=', this.profileId);
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