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
      if (this.userId) {
        this.$router.push({ name: 'profile', params: { id: this.userId } });
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
  border: 2px solid var(--primary-color);
  transform: scale(1.05);
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  transition: all 0.2s ease;
  border: 2px solid transparent;
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