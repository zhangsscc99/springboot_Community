<template>
  <div class="user-avatar-container" 
       :class="{ 'clickable': clickable && userId }"
       @click="clickable && userId ? navigateToProfile() : null">
      <img 
        :src="imageSource" 
      :alt="`${username}'s avatar`" 
      class="user-avatar"
        @error="handleImageError"
      />
    <div v-if="showUsername" class="avatar-username">{{ username }}</div>
  </div>
</template>

<script>
export default {
  name: 'UserAvatar',
  props: {
    src: {
      type: String,
      default: ''
    },
    username: {
      type: String,
      required: true
    },
    showUsername: {
      type: Boolean,
      default: false
    },
    clickable: {
      type: Boolean,
      default: true
    },
    userId: {
      type: [String, Number],
      default: null
    }
  },
  data() {
    return {
      defaultAvatar: 'https://thumbs.dreamstime.com/b/%E9%BB%98%E8%AE%A4%E5%A4%B4%E5%83%8F%E9%85%8D%E7%BD%AE%E6%96%87%E4%BB%B6%E5%9B%BE%E6%A0%87-%E7%A4%BE%E4%BA%A4%E5%AA%92%E4%BD%93%E7%94%A8%E6%88%B7%E7%9F%A2%E9%87%8F%E5%9B%BE-%E7%A4%BE%E4%BA%A4%E5%AA%92%E4%BD%93%E7%94%A8%E6%88%B7%E7%9F%A2%E9%87%8F%E5%9B%BE%E5%9B%BE%E5%83%8F-209162840.jpg',
      imageError: false
    }
  },
  computed: {
    imageSource() {
      const timestamp = new Date().getTime();
      if (this.src) {
        if (this.src.startsWith('http')) {
          return `${this.src}${this.src.includes('?') ? '&' : '?'}t=${timestamp}`;
        }
        return `${this.src}?t=${timestamp}`;
      }
      return `https://via.placeholder.com/100?text=${this.username.charAt(0).toUpperCase()}&t=${timestamp}`;
    }
  },
  methods: {
    handleImageError(e) {
      this.imageError = true;
      e.target.src = this.defaultAvatar;
    },
    navigateToProfile() {
      console.log('🚀 === UserAvatar navigateToProfile 被调用 ===');
      console.log('传入的 props.userId:', this.userId);
      console.log('传入的 props.username:', this.username);
      console.log('userId 类型:', typeof this.userId);
      
      if (!this.userId) {
        console.log('❌ [Debug] 没有提供用户ID，无法导航到个人主页');
        return;
      }

      console.log('✅ [Debug] 开始导航到用户主页，用户ID:', this.userId);
      
      // 获取当前用户ID (尝试从多个位置获取)
      let currentUserId = null;
      
      // 首先尝试从 userInfo 获取
      const userInfoJson = localStorage.getItem('userInfo');
      if (userInfoJson) {
        try {
          const userData = JSON.parse(userInfoJson);
          if (userData && userData.id) {
            currentUserId = userData.id;
            console.log('[Debug] 从 userInfo 获取到当前用户ID:', currentUserId);
          }
        } catch (e) {
          console.error('[Debug] 解析 userInfo 失败:', e);
        }
      }
      
      // 如果从 userInfo 没有获取到，尝试从 user 获取
      if (!currentUserId) {
        const userJson = localStorage.getItem('user');
        if (userJson) {
          try {
            const userData = JSON.parse(userJson);
            if (userData) {
              if (userData.id) {
                currentUserId = userData.id;
                console.log('[Debug] 从 user 获取到当前用户ID:', currentUserId);
              } else if (userData.userId) {
                currentUserId = userData.userId;
                console.log('[Debug] 从 user.userId 获取到当前用户ID:', currentUserId);
              } else if (userData.user && userData.user.id) {
                currentUserId = userData.user.id;
                console.log('[Debug] 从 user.user.id 获取到当前用户ID:', currentUserId);
              }
            }
          } catch (e) {
            console.error('[Debug] 解析 user 失败:', e);
          }
        }
      }
      
      // 如果还没有找到，尝试直接使用 userId
      if (!currentUserId) {
        currentUserId = localStorage.getItem('userId');
        if (currentUserId) {
          console.log('[Debug] 从 userId 获取到当前用户ID:', currentUserId);
        }
      }
      
      // 导航到个人主页
      const targetRoute = { name: 'profile', params: { id: this.userId } };
      console.log('🎯 [Debug] 即将导航到路由:', targetRoute);
      console.log('🎯 [Debug] 目标用户ID:', this.userId);
      console.log('🎯 [Debug] 目标用户名:', this.username);
      
      // 如果当前路径是个人主页，且正在导航到自己的主页，强制刷新路由
      const isProfilePage = this.$route.name === 'profile';
      const isNavigatingToSelf = currentUserId && this.userId == currentUserId;
      const isAlreadyOnSelfProfile = isProfilePage && this.$route.params.id == currentUserId;

      if (isNavigatingToSelf && isProfilePage && !isAlreadyOnSelfProfile) {
        // 先导航到另一个路由，然后回到个人主页，强制刷新组件
        console.log('[Debug] 需要强制刷新个人主页');
        this.$router.push({ path: '/' }).then(() => {
          this.$router.push(targetRoute);
        }).catch(err => {
          console.error('[Debug] 路由导航错误:', err);
        });
      } else {
        // 正常导航
        console.log('[Debug] 正常导航到个人主页');
        this.$router.push(targetRoute).catch(err => {
          if (err.name !== 'NavigationDuplicated') {
            console.error('[Debug] 路由导航错误:', err);
          }
        });
      }
    }
  }
}
</script>

<style scoped>
.user-avatar-container {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
}

.user-avatar-container.clickable {
  cursor: pointer;
}

.user-avatar-container.clickable:hover .user-avatar {
  /* 创建更加柔和的渐变色边框效果 */
  border: 2px solid transparent;
  /* 使用径向渐变背景来创建边框效果 */
  background-image: 
    linear-gradient(white, white), 
    linear-gradient(to right, 
      rgba(var(--primary-gradient-start-rgb), 0.4), 
      rgba(var(--primary-gradient-end-rgb), 0.4)
    );
  background-origin: border-box;
  background-clip: padding-box, border-box;
  /* 添加细微的阴影效果增强立体感 */
  box-shadow: 0 0 8px rgba(var(--primary-gradient-start-rgb), 0.15);
  /* 保留原有的缩放效果 */
  transform: scale(1.05);
  transition: all 0.3s ease;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  /* 使用纯色边框作为初始状态 */
  border: 2px solid transparent;
  /* 设置背景图像以便在悬停时能平滑过渡 */
  background-image: 
    linear-gradient(white, white), 
    linear-gradient(white, white);
  background-origin: border-box;
  background-clip: padding-box, border-box;
  /* 添加平滑过渡效果 */
  transition: all 0.3s ease;
}

.avatar-username {
  font-size: 11px;
  margin-top: 3px;
  color: var(--text-color);
  max-width: 75px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: center;
}
</style> 