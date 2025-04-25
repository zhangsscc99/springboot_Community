<template>
  <div class="conversation-item" :class="{ 'unread': conversation.unreadCount > 0 }">
    <div class="avatar">
      <img :src="avatarSource" alt="头像" @error="handleImageError">
      <div class="badge" v-if="conversation.unreadCount > 0">
        {{ conversation.unreadCount > 99 ? '99+' : conversation.unreadCount }}
      </div>
    </div>
    <div class="content">
      <div class="top-row">
        <div class="name">{{ conversation.name }}</div>
        <div class="time">{{ formatTime(conversation.lastMessageTime) }}</div>
      </div>
      <div class="message-preview">{{ conversation.lastMessage || '点击开始聊天' }}</div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ConversationItem',
  props: {
    conversation: {
      type: Object,
      required: true,
      default: () => ({
        id: '',
        name: '',
        avatar: '',
        lastMessage: '',
        lastMessageTime: new Date(),
        unreadCount: 0
      })
    }
  },
  data() {
    return {
      defaultAvatar: 'https://thumbs.dreamstime.com/b/%E9%BB%98%E8%AE%A4%E5%A4%B4%E5%83%8F%E9%85%8D%E7%BD%AE%E6%96%87%E4%BB%B6%E5%9B%BE%E6%A0%87-%E7%A4%BE%E4%BA%A4%E5%AA%92%E4%BD%93%E7%94%A8%E6%88%B7%E7%9F%A2%E9%87%8F%E5%9B%BE-%E7%A4%BE%E4%BA%A4%E5%AA%92%E4%BD%93%E7%94%A8%E6%88%B7%E7%9F%A2%E9%87%8F%E5%9B%BE%E5%9B%BE%E5%83%8F-209162840.jpg',
      imageError: false
    }
  },
  computed: {
    avatarSource() {
      const timestamp = new Date().getTime();
      if (this.conversation.avatar) {
        if (this.conversation.avatar.startsWith('http')) {
          return `${this.conversation.avatar}${this.conversation.avatar.includes('?') ? '&' : '?'}t=${timestamp}`;
        }
        return `${this.conversation.avatar}?t=${timestamp}`;
      }
      return `https://via.placeholder.com/100?text=${this.conversation.name.charAt(0).toUpperCase()}&t=${timestamp}`;
    }
  },
  methods: {
    handleImageError(e) {
      this.imageError = true;
      e.target.src = this.defaultAvatar;
    },
    formatTime(time) {
      if (!time) return '';
      
      const date = new Date(time);
      const now = new Date();
      
      // If the message is from today, show only the time
      if (date.toDateString() === now.toDateString()) {
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      }
      
      // If the message is from this week, show the day of the week
      const diffDays = Math.round((now - date) / (1000 * 60 * 60 * 24));
      if (diffDays < 7) {
        const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
        return weekdays[date.getDay()];
      }
      
      // Otherwise, show the date
      return date.toLocaleDateString([], { month: 'numeric', day: 'numeric' });
    }
  }
}
</script>

<style scoped>
.conversation-item {
  display: flex;
  padding: 12px 16px;
  border-bottom: 1px solid #eaeaea;
  cursor: pointer;
  transition: background-color 0.2s;
}

.conversation-item:hover {
  background-color: #f8f8f8;
}

.conversation-item:active {
  background-color: #f0f0f0;
}

.conversation-item.unread .name {
  font-weight: 600;
  color: #000;
}

.conversation-item.unread .message-preview {
  color: #333;
}

.avatar {
  position: relative;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  margin-right: 12px;
  overflow: hidden;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.badge {
  position: absolute;
  top: -6px;
  right: -6px;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  background-color: #ff3b30;
  color: white;
  font-size: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  border: 1.5px solid #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  justify-content: center;
}

.top-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  align-items: center;
}

.name {
  font-size: 16px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 70%;
}

.time {
  font-size: 12px;
  color: #999;
}

.message-preview {
  font-size: 14px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style> 
