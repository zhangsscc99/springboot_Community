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
          src="https://via.placeholder.com/100" 
          :username="profileName"
          class="profile-avatar-component"
        />
        <h1 class="profile-name">{{ profileName }}</h1>
        <div class="profile-userid">ID: {{ profileId }}</div>
        
        <div class="profile-stats">
          <div class="stat-item">
            <div class="stat-value">108</div>
            <div class="stat-label">关注</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">2534</div>
            <div class="stat-label">粉丝</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">62</div>
            <div class="stat-label">获赞</div>
          </div>
        </div>
        
        <p class="profile-bio">{{ profileBio }}</p>
        
        <div class="profile-actions">
          <button class="follow-btn" v-if="!isCurrentUser">
            <i class="fas fa-plus"></i> 关注
          </button>
          <button class="edit-profile-btn" v-else>
            <i class="fas fa-pencil-alt"></i> 编辑资料
          </button>
          <button class="message-btn">
            <i class="fas fa-comment"></i> 私信
          </button>
        </div>
      </div>
      
      <div class="profile-tabs">
        <div 
          class="profile-tab" 
          :class="{ active: activeTab === 'posts' }"
          @click="activeTab = 'posts'"
        >
          帖子
        </div>
        <div 
          class="profile-tab" 
          :class="{ active: activeTab === 'likes' }"
          @click="activeTab = 'likes'"
        >
          喜欢
        </div>
        <div 
          class="profile-tab" 
          :class="{ active: activeTab === 'comments' }"
          @click="activeTab = 'comments'"
        >
          评论
        </div>
      </div>
      
      <div class="profile-content">
        <div v-if="activeTab === 'posts'" class="posts-list">
          <div v-if="userPosts.length > 0" class="post-list">
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
          <div class="no-content">
            <i class="far fa-heart"></i>
            <p>还没有喜欢任何帖子</p>
          </div>
        </div>
        
        <div v-else-if="activeTab === 'comments'" class="comments-list">
          <div class="no-content">
            <i class="far fa-comment"></i>
            <p>还没有评论任何帖子</p>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import UserAvatar from '@/components/UserAvatar.vue';

export default {
  name: 'ProfileView',
  components: {
    UserAvatar
  },
  data() {
    return {
      loading: false,
      profileId: '',
      activeTab: 'posts',
      userPosts: [
        {
          id: 1,
          title: "执行间隙为什么会毁掉一个人？",
          content: "刚刚读到一篇好文：一个人真正废掉的核心原因，往往不是能力不足或资源匮乏，而是从想法到行动的「执行间隙」太大，想做的事情迟迟不能落地。",
          likes: 1488,
          comments: 3468,
          created_at: "2023-05-13T14:45:00Z"
        },
        {
          id: 2,
          title: "大厂程序员的那些事",
          content: "今天想和大家分享一下在大厂工作的一些感受和思考。大厂工作节奏确实比较快，但是学习的机会也很多...",
          likes: 254,
          comments: 78,
          created_at: "2023-05-10T09:15:00Z"
        }
      ]
    };
  },
  computed: {
    profileName() {
      return this.profileId === '123' ? '咫尺燃灯' : 'User Not Found';
    },
    profileBio() {
      return this.profileId === '123' ? '科技|思考|阅读|写作 / 分享有趣有用的内容 / 欢迎交流' : '';
    },
    isCurrentUser() {
      // In a real app, compare to logged-in user ID
      return this.profileId === '123';
    }
  },
  methods: {
    goBack() {
      this.$router.go(-1);
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
  created() {
    this.loading = true;
    // Simulate API call
    setTimeout(() => {
      this.profileId = this.$route.params.id || '123';
      this.loading = false;
    }, 500);
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
</style>